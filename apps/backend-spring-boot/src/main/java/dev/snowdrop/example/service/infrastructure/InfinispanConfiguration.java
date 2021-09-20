package dev.snowdrop.example.service.infrastructure;

import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.spring.starter.remote.InfinispanRemoteCacheCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class InfinispanConfiguration {

   @Bean
   @Order(Ordered.HIGHEST_PRECEDENCE)
   public InfinispanRemoteCacheCustomizer caches() {
      final Logger LOGGER = LoggerFactory.getLogger(InfinispanRemoteCacheCustomizer.class);
      return builder -> {
         LOGGER.info("InfinispanRemoteCacheCustomizer->caches()");
         
         try {
            URI fruitsCacheConfigUri = this.getClass().getClassLoader().getResource("fruits-cache.xml").toURI();
            URI fruitsBySeasonCacheConfigUri = this.getClass().getClassLoader().getResource("fruits-by-season-cache.xml").toURI();

            builder.remoteCache(Metadata.FRUITS_CACHE)
                 .configurationURI(fruitsCacheConfigUri)
                 .marshaller(ProtoStreamMarshaller.class);
            LOGGER.info("InfinispanRemoteCacheCustomizer->caches() " + Metadata.FRUITS_CACHE + " created");

            builder.remoteCache(Metadata.FRUITS_BY_SEASON_CACHE)
                 .configurationURI(fruitsBySeasonCacheConfigUri)
                 .marshaller(ProtoStreamMarshaller.class);
            LOGGER.info("InfinispanRemoteCacheCustomizer->caches() " + Metadata.FRUITS_BY_SEASON_CACHE + " created");
         } catch (URISyntaxException e) {
            throw new RuntimeException(e);
         }

         // Add marshaller in the client, the class is generated from the interface in compile time
         builder.addContextInitializer(new FruitSchemaBuilderImpl());
         LOGGER.info("InfinispanRemoteCacheCustomizer->caches() marshaller added");
      };
   }
}