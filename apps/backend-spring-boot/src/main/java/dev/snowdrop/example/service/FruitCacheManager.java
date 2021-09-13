/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.snowdrop.example.service;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.core.env.Environment;

import dev.snowdrop.example.service.infrastructure.Metadata;

@Configuration
public class FruitCacheManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(FruitCacheManager.class);

    // @Autowired
    // private Environment environment;

    
    private final RemoteCacheManager cacheManager;

    @Autowired
    public FruitCacheManager(RemoteCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Bean
    RemoteCache<Long, Fruit> getFruitsCache() {
        LOGGER.info(">> getFruitsCache");
        return this.cacheManager.getCache(Metadata.FRUITS_CACHE);
    }

    // @Bean
    // RemoteCacheManager getCacheManager() {
    //     LOGGER.info(">> getCacheManager");
    //     ConfigurationBuilder builder = new ConfigurationBuilder();
        
    //     List<String> activeProfiles = Arrays.asList(this.environment.getActiveProfiles());
    //     if (activeProfiles.contains("local")) {
    //         Properties p = new Properties();
    //         try(InputStream is = this.getClass().getClassLoader().getResourceAsStream("META-INF/caching/hotrod-client.properties")) {
    //             p.load(is);
    //             LOGGER.info(">> properties: " + p);
    //             builder.withProperties(p);
    //             builder.addServer()
    //                 .host("localhost")
    //                 .port(11222)
    //                 .security().authentication()
    //                 .username("developer")
    //                 .password("developer");
    //                 //.remoteCache("fruits")
    //                 //.configuration("<replicated-cache name=\"fruits\"><encoding media-type=\"application/x-protostream\"/></replicated-cache>");
                
    //             builder.remoteCache(Metadata.FRUITS_CACHE)
    //                 // .configurationURI(cacheConfigUri)
    //                 .marshaller(ProtoStreamMarshaller.class);
   
    //             // Add marshaller in the client, the class is generated from the interface in compile time
    //             builder.addContextInitializer(new FruitSchemaBuilderImpl());
                
    //         } catch (Throwable t) {
    //             LOGGER.error("File not found. " + t.getMessage());
    //         }
    //     }

    //     this.cacheManager = new RemoteCacheManager(builder.build());

    //     RemoteCache<Long, Fruit> cache = cacheManager.getCache(Metadata.FRUITS_CACHE);
    //     LOGGER.info("cache: " + cache);

    //     return cacheManager;
    // }
}
