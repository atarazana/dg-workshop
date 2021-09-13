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
         URI cacheConfigUri;
         try {
            cacheConfigUri = this.getClass().getClassLoader().getResource("fruits-cache.xml").toURI();
         } catch (URISyntaxException e) {
            throw new RuntimeException(e);
         }

         builder.remoteCache(Metadata.FRUITS_CACHE)
                 .configurationURI(cacheConfigUri)
                 .marshaller(ProtoStreamMarshaller.class);

         // Add marshaller in the client, the class is generated from the interface in compile time
         builder.addContextInitializer(new FruitSchemaBuilderImpl());
      };
   }
}