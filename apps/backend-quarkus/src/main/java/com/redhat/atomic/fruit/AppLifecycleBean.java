package com.redhat.atomic.fruit;

import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.configuration.ProfileManager;

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