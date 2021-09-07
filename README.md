# Deploying the operator

You have to be administrator to run this script successfully!

```sh
./run-admin.sh
```

## Deploying a PostgreSQL, a DG cluster and other needed artifacts...

You have to be logged in your OpenShift cluster before running this script, check if you are running this command. It should return a string containing the user or an error if you're not logged in.

```sh
oc whoami
```

If you are correctly logged in, please run this script to deploy, this creates all the infrastrucure needed in a project called `dg-workshop-userId`.

NOTE: Run `oc project` to see that the default project is the project the script just created.

```sh
./run.sh $(oc whoami)
```

## If Spring Boot

Deploy code with no cache involved

```sh
mvn clean oc:deploy -Popenshift -DskipTests
```

## If Quarkus

Deploy code with no cache involved

```sh
export PROJECT_NAME=$(oc project -q)
./mvnw clean package -Dquarkus.kubernetes.deploy=true -DskipTests
```

Wait until the application is deployed... blah blah

### Preparing our code to connect to Data Grid

#### Adding inifinispan client extension

First let's add the extension needed `infinispan-client`.

```sh
./mvnw quarkus:add-extension -Dextensions="infinispan-client"
```

You'll see this...

```sh
[SUCCESS] ✅ Extension io.quarkus:quarkus-infinispan-client has been installed
```

#### Update `application.properties` to connect to the Infinispan cluster

Let's update `application.properties` so that our code can connect to the Infinispan cluster. Add these lines at the end of the file:

```properties
# Infinispan
# %dev ==> docker run -it -p 11222:11222 -e USER="developer" -e PASS="developer" infinispan/server
# %dev ==> oc port-forward -n ${PROJECT_NAME} svc/eda-infinispan 11222:11222
%dev.quarkus.infinispan-client.server-list=localhost:11222
#%dev.quarkus.infinispan-client.server-list=${INFINISPAN_SERVICE_HOST}:80
#%dev.quarkus.infinispan-client.server-list=eda-infinispan:11222
quarkus.infinispan-client.server-list=eda-infinispan:11222
quarkus.infinispan-client.auth-username=developer
quarkus.infinispan-client.auth-password=developer
quarkus.infinispan-client.auth-server-name=infinispan
quarkus.infinispan-client.auth-realm=default
quarkus.infinispan-client.near-cache-max-entries=1000
quarkus.infinispan-client.client-intelligence=BASIC
```

As you can see we have provided are several possibilities to connect to Infinispan in development mode (profile `dev`). We're going to use podman/docker to run Infinispan locally, specifically we're going to use this one:

```properties
%dev.quarkus.infinispan-client.server-list=localhost:11222
```

But it could also be interesting to use `Telepresence`, specially if there are other services we need to work with, databases, kafka, etc. In that case you would use this and of course `Telepresence`.

```properties
%dev.quarkus.infinispan-client.server-list=eda-infinispan:11222
```

#### Upgrade the Code to enable caching

Update `Fruit.java` with this code so that the Fruit objects can be sent/retrieved from Infinispan using `Protobuf`. Notice that we have added a default constructor and another one with all the properties, additionally we have getters and setters, both the constructor that initializes all the properties and the getters are annotated so that a `Protobuf` definition is auto-generated.

```java
package com.redhat.atomic.fruit;

import java.util.List;

import javax.persistence.Entity;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Fruit extends PanacheEntity {

    public String name;
    public String season;

    public Fruit() {
    }

    @ProtoFactory
    public Fruit(Long id, String name, String season) {
        this.id = id;
        this.name = name;
        this.season = season;
    }

    @ProtoField(number = 1)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ProtoField(number = 2)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ProtoField(number = 3)
    public String getSeason() {
        return season;
    }
    
    public void setSeason(String season) {
        this.season = season;
    }

    public static List<Fruit> getAllFruitsForSeason(String season) {
        return find("season", season).list();
    }
}
```

Annotating and preparing the `Fruit` class is not enough... we need to declare the object according to the `Protobuf` protocol, we could do this manually but there's a better way. Crate new file in this folder `apps/backend-quarkus/src/main/java/com/redhat/atomic/fruit/infrastructure` and give it this name `FruitContextInitializer.java`. 

```java
package com.redhat.atomic.fruit.infrastructure;

import com.redhat.atomic.fruit.Fruit;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Fruit.class }, schemaPackageName = "dg.workshop")
interface FruitContextInitializer extends SerializationContextInitializer {
}
```

Now that we can send and receive `Fruit` objects and generate the `Protobuf` schema let's update the business logic of our REST service, open `` and substitute the code with this one.

```java
package com.redhat.atomic.fruit;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.QueryResult;
import org.jboss.logging.Logger;

import io.quarkus.infinispan.client.Remote;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitResource {
    private static final Logger LOGGER = Logger.getLogger(FruitResource.class);

    @ConfigProperty(name = "hello.message")
    String message;

    @Inject 
    @Remote("fruits")
    RemoteCache<Long, Fruit> cache;

    @GET
    @Path("fruit/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOGGER.debug("Hello method is called with message: " + this.message); // logging & custom property
        return message; // custom property
    }
    
    @GET
    @Path("fruit")
    public List<Fruit> allFruits() {
        LOGGER.info("allFruits");
        // ArrayList<Fruit> fruits = new ArrayList<Fruit>();
        // for (Map.Entry<Long, Fruit> fruit : cache.entrySet()) {
        //     fruits.add(fruit.getValue());
        // }
        // return fruits;
        // return Fruit.listAll(); 
        return cache.values().stream().collect(Collectors.toList());
    }

    @GET
    @Path("fruit/no-cache")
    public List<Fruit> allFruitsNoCache() {
        LOGGER.info("allFruitsNoCache");
        return Fruit.listAll(); 
    }

    @GET
    @Path("cache/warmup")
    public List<Fruit> cacheWarmUp() {
        LOGGER.info("Warming up the cache...");
        Map<Long, Fruit> map = Fruit.<Fruit>listAll().stream()
            .collect(Collectors.toMap(Fruit::getId, Function.identity()));

        cache.putAll(map);

        return cache.values().stream().collect(Collectors.toList());
    }

    @GET
    @Path("fruit/by-season/{season}")
    public List<Fruit> fruitsBySeason(@PathParam("season") String season) {
        LOGGER.info("allFruitsNoCache");
        // return Fruit.getAllFruitsForSeason(season);
        QueryFactory queryFactory = Search.getQueryFactory(cache);
        Query<Fruit> query = queryFactory.create("FROM dg.workshop.Fruit WHERE season = '" + season + "' " );

        // Execute the query
        QueryResult<Fruit> queryResult = query.execute();

        LOGGER.info("fruitsBySeason->size = " + queryResult.hitCount());

        return queryResult.list()
                .stream()
                //.filter(c -> c.getTimestamp().compareTo(Instant.now().minusSeconds(periodInSeconds)) > 0)
                .collect(Collectors.toList());
    }

    @GET
    @Path("fruit/{id}")
    public Fruit fruitsById(@PathParam("id") Long id) {
        //return Fruit.findById(id);
        return cache.get(id);
    }

    @POST
    @Path("/")
    public Response processCloudEvent(
        @HeaderParam("ce-id") String id,
        @HeaderParam("ce-type") String type,
        @HeaderParam("ce-source") String source,
        @HeaderParam("ce-specversion") String specversion,
        @HeaderParam("ce-user") String user,
        @HeaderParam("content-type") String contentType,
        @HeaderParam("content-length") String contentLength,
        Fruit fruit) {
        
        System.out.println("ce-id=" + id);
        System.out.println("ce-type=" + type);
        System.out.println("ce-source=" + source);
        System.out.println("ce-specversion=" + specversion);
    
        System.out.println("ce-user=" +user);
        System.out.println("content-type=" + contentType);
        System.out.println("content-length=" + contentLength);
        
        return saveFruit(fruit);
    }

    @POST
    @Path("fruit")
    @Transactional
    public Response saveFruit(Fruit fruit) {
        // since the FruitEntity is a panache entity
        // persist is available by default
        fruit.persist();
        cache.put(fruit.id, fruit);
        final URI createdUri = UriBuilder.fromResource(FruitResource.class)
                        .path(Long.toString(fruit.id))
                        .build();
        return Response.created(createdUri).build();
    }

    @PUT
    @Path("fruit/{id}")
    @Transactional
    public Response updateFruit(@PathParam("id") Long id, Fruit fruit) {
        LOGGER.info(String.format("id: %s fruit: %s", id, fruit));

        // since the FruitEntity is a panache entity
        // persist is available by default
        Fruit found = Fruit.findById(id);
        LOGGER.info("found" + found);
        if (found != null) {
            found.name = fruit.name;
            found.season = fruit.season;
            found.persist();
            cache.put(id, found); // Put into the cache
        } else {
            fruit.persist();
            cache.put(id, fruit); // Put into the cache
        }

        final URI createdUri = UriBuilder.fromResource(FruitResource.class)
                        .path(Long.toString(id))
                        .build();
        return Response.created(createdUri).build();
    }

    @DELETE
    @Path("fruit/{id}")
    @Transactional
    public void deleteFruit(@PathParam("id") Long id) {
        // since the FruitEntity is a panache entity
        // persist is available by default
        Fruit.deleteById(id);
        cache.remove(id);
    }
}
```

All CRUD functions have been modified so that changes to the database are propagated to the cache also notice that we have aded some annotations to inject the `fruits` cache.

```java
    @Inject 
    @Remote("fruits")
    RemoteCache<Long, Fruit> cache;
```

Special attention to method `allFruits` and `fruitsBySeason` because these two don't go to the database at all and that's why you have to warm up the cache before you can use them.

One last step before we test our code. This is only to make our lives easier during the inner loop development cycles, this class is going to be run whenever the application is started up the goal of it is to create the cache we need and only in `dev` mode.

Create a new file called `AppLifecycleBean.java` and put it in this folder `apps/backend-quarkus/src/main/java/com/redhat/atomic/fruit/AppLifecycleBean.java`. Then copy this content.

```java
package com.redhat.atomic.fruit;

import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.logging.Logger;

@Startup 
@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOGGER = Logger.getLogger(AppLifecycleBean.class);

    public AppLifecycleBean() {
        LOGGER.info(">> The application is starting... active profile: " + ProfileManager.getActiveProfile());
        if (ProfileManager.getActiveProfile() == "dev") {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            Properties p = new Properties();
            try(InputStream is = this.getClass().getClassLoader().getResourceAsStream("META-INF/resources/hotrod-client.properties")) {
                p.load(is);
                LOGGER.info(">> properties: " + p);
                builder.addServer()
                    .host("localhost")
                    .port(11222)
                    .security().authentication()
                    .username("developer")
                    .password("developer")
                    .remoteCache("fruits")
                    .configuration("<replicated-cache name=\"fruits\"><encoding media-type=\"application/x-protostream\"/></replicated-cache>");
                RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());
                RemoteCache<Long, Fruit> cache = cacheManager.getCache("fruits");
                LOGGER.info("cache: " + cache);
            } catch (Throwable t) {
                LOGGER.error("File not found. " + t.getMessage());
            }
        }
        LOGGER.info("<< The application is starting... ");
    }

}
```

