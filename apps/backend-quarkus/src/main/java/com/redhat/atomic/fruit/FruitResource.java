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