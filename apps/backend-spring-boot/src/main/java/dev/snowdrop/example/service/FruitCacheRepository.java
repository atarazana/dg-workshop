package dev.snowdrop.example.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import dev.snowdrop.example.service.infrastructure.Metadata;

@Repository
@Qualifier("SpringCache") 
@CacheConfig(cacheNames=Metadata.FRUITS_CACHE)
public class FruitCacheRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(FruitCacheRepository.class);
    
    private final FruitRepository repository;

    @Autowired
    @Qualifier("getFruitsCacheNative")
    private RemoteCache<Long, Fruit> fruitsCache;
    
    @Autowired
    @Qualifier("getFruitsBySeasonCache")
    private Cache fruitsBySeasonCache;

    @Autowired
    public FruitCacheRepository(FruitRepository repository) {
        this.repository = repository;
    }

    @CacheEvict(allEntries=true)
    public List<Fruit> findAll(){
        LOGGER.info("findAll was called");
        
        LOGGER.info("repository.findAll()");
        List<Fruit> fruitList = StreamSupport
            .stream(repository.findAll().spliterator(), false)
            .collect(Collectors.toList());

        LOGGER.info("cache.putAll(map)");
        Map<Long, Fruit> map = StreamSupport.stream(repository.findAll().spliterator(), false)
                .collect(Collectors.toMap(Fruit::getId, Function.identity()));
        fruitsCache.putAll(map);

        return fruitList;
    }

    @Cacheable(key="#id", unless="#result == null")
    public Fruit findById(Long id){
        LOGGER.info("findById was called");
    	return repository.findById(id).get(); 	
    }

    @Cacheable(cacheNames=Metadata.FRUITS_BY_SEASON_CACHE, key="#season", unless="#result == null")
    public Optional<FruitsBySeason> findBySeason(String season){
        LOGGER.info("findBySeason was called");
    	List<Fruit> fruits = repository.findBySeason(season);
        return Optional.of(new FruitsBySeason(season, fruits));
    }

    @CachePut(key="#fruit.id")
    public Fruit upsert(Fruit fruit){
        LOGGER.info("upsert was called");
        List<String> seasonsToEvict = new ArrayList<String>();
        if (fruit.getId() != null) {
            if (repository.existsById(fruit.getId())) {
                // Update...
                seasonsToEvict.add(repository.findById(fruit.getId()).get().getSeason());
            } else {
                // Create but id needs some fixing... nullifying
                fruit.setId(null);
            }
        }
        seasonsToEvict.add(fruit.getSeason());

        for (String season : seasonsToEvict) {
            fruitsBySeasonCache.evict(season);    
        }
        
        return repository.save(fruit);
    }
    
    @CacheEvict(key="#id")
    public void delete(Long id){
        LOGGER.info("delete was called");
        if (fruitsCache.containsKey(id)) {
            fruitsBySeasonCache.evict(fruitsCache.get(id).getSeason());
        }
        
        repository.deleteById(id);
    }

    public Boolean existsById(Long id) {
        LOGGER.info("existsById was called");
        return findById(id) != null && findById(id).getId() != null;
    }
}

