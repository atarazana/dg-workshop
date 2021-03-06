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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.snowdrop.example.service.infrastructure.Metadata;

@Configuration
public class FruitCacheManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(FruitCacheManager.class);

    private final CacheManager cacheManager;
    private final RemoteCacheManager nativeCacheManager;

    @Autowired
    public FruitCacheManager(CacheManager cacheManager, RemoteCacheManager nativeCacheManager) {
        this.cacheManager = cacheManager;
        this.nativeCacheManager = nativeCacheManager;
    }

    @Bean
    Cache getFruitsCache() {
        LOGGER.info(">> getFruitsCache");
        return this.cacheManager.getCache(Metadata.FRUITS_CACHE);
    }

    @Bean
    RemoteCache<Long, Fruit> getFruitsCacheNative() {
        LOGGER.info(">> getFruitsCacheNative");
        return this.nativeCacheManager.getCache(Metadata.FRUITS_CACHE);
    }

    @Bean
    Cache getFruitsBySeasonCache() {
        LOGGER.info(">> getFruitsBySeasonCache");
        return this.cacheManager.getCache(Metadata.FRUITS_BY_SEASON_CACHE);
    }

    @Bean
    RemoteCache<String, FruitsBySeason> getFruitsBySeasonCacheNative() {
        LOGGER.info(">> getFruitsBySeasonCacheNative");
        return this.nativeCacheManager.getCache(Metadata.FRUITS_BY_SEASON_CACHE);
    }
}
