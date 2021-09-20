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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import dev.snowdrop.example.exception.NotFoundException;
import dev.snowdrop.example.exception.UnprocessableEntityException;
import dev.snowdrop.example.exception.UnsupportedMediaTypeException;
import io.micrometer.core.instrument.Metrics;

@RestController
@RequestMapping(value = "/")
public class FruitController {

    private static final String FORCED_INTERNAL_ERROR = "FORCED INTERNAL ERROR";

    private static final Logger LOGGER = LoggerFactory.getLogger(FruitController.class);
    
    private final FruitRepository repository;
    private final FruitCacheRepository cacheRepository;

    @Autowired
    public FruitController(FruitRepository repository, FruitCacheRepository cacheRepository) {
        LOGGER.info("FruitController: " + repository + " / " + cacheRepository);
        this.repository = repository;
        this.cacheRepository = cacheRepository;
    }

    @RequestMapping(value = "/")
    public RedirectView welcome() {
        return new RedirectView("index.html");
    }
    
    @PostMapping("/")
    public Fruit processCloudEvent(
        @RequestHeader("ce-id") String id,
        @RequestHeader("ce-type") String type,
        @RequestHeader("ce-source") String source,
        @RequestHeader("ce-specversion") String specversion,
        @RequestHeader("ce-user") String user,
        @RequestHeader("content-type") String contentType,
        @RequestHeader("content-length") String contentLength,
        @RequestBody(required = false) Fruit fruit) {
        
        System.out.println("ce-id=" + id);
        System.out.println("ce-type=" + type);
        System.out.println("ce-source=" + source);
        System.out.println("ce-specversion=" + specversion);
    
        System.out.println("ce-user=" +user);
        System.out.println("content-type=" + contentType);
        System.out.println("content-length=" + contentLength);
        
        return post(fruit);
    }

    @GetMapping("/fruit/{id}")
    public Fruit get(@PathVariable("id") Long id) {
        if (checkThrowErrors()) {
            throwInternalServerError();
        }

        // >>> Prometheus metric
        Metrics.counter("api.http.requests.total", "api", "fruit", "method", "GET", "endpoint", 
            "/fruit/" + id).increment();
        // <<< Prometheus metric
        verifyFruitExists(id);

        timeOut();

        //return repository.findById(id).get();
        // return cache.get(id);
        return cacheRepository.findById(id);
    }

    @GetMapping("/fruit-by-season/{season}")
    public List<Fruit> get(@PathVariable("season") String season) {
        if (checkThrowErrors()) {
            throwInternalServerError();
        }

        // >>> Prometheus metric
        Metrics.counter("api.http.requests.total", "api", "fruit", "method", "GET", "endpoint", 
            "/fruit-by-season/" + season).increment();
        // <<< Prometheus metric

        timeOut();

        //return repository.findById(id).get();
        // return cache.get(id);
        return cacheRepository.findBySeason(season).isPresent() ? cacheRepository.findBySeason(season).get().getFruits() : new ArrayList<Fruit> ();
    }

    @GetMapping("/fruit/no-cache")
    public List<Fruit> allFruitsNoCache() {
        if (checkThrowErrors()) {
            throwInternalServerError();
        }

        // Prometheus metric
        Metrics.counter("api.http.requests.total", "api", "fruit", "method", "GET", "endpoint", 
        "/fruit/no-cache").increment();
        // <<< Prometheus metric
        
        Spliterator<Fruit> fruits = repository.findAll()
                .spliterator();

        timeOut();

        return StreamSupport
                .stream(fruits, false)
                .collect(Collectors.toList());
    }

    // @GetMapping("/cache/warmup")
    // public List<Fruit> cacheWarmUp() {
    //     LOGGER.info("Warming up the cache...");
    //     Map<Long, Fruit> map = StreamSupport.stream(repository.findAll().spliterator(), false)
    //         .collect(Collectors.toMap(Fruit::getId, Function.identity()));

    //     cache.putAll(map);

    //     return cache.values().stream().collect(Collectors.toList());
    // }

    @GetMapping("/fruit")
    public List<Fruit> getAll() {
        LOGGER.info("getAll() was called");
        if (checkThrowErrors()) {
            throwInternalServerError();
        }

        // Prometheus metric
        Metrics.counter("api.http.requests.total", "api", "fruit", "method", "GET", "endpoint", 
        "/fruit").increment();
        // <<< Prometheus metric
        
        // Spliterator<Fruit> fruits = repository.findAll()
        //         .spliterator();

        timeOut();

        // return StreamSupport
        //         .stream(fruits, false)
        //         .collect(Collectors.toList());
        // return cache.values().stream().collect(Collectors.toList());
        return cacheRepository.findAll();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/fruit")
    public Fruit post(@RequestBody(required = false) Fruit fruit) {
        if (checkThrowErrors()) {
            throwInternalServerError();
        }

        verifyCorrectPayload(fruit);

        timeOut();

        // repository.save(fruit);
        // return cache.put(fruit.getId(), fruit);
        LOGGER.info("cacheRepository.upsert(" + fruit.getId() + ", " + fruit + ")");
        return cacheRepository.upsert(fruit);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/fruit/{id}")
    public Fruit put(@PathVariable("id") Long id, @RequestBody(required = false) Fruit fruit) {
        if (checkThrowErrors()) {
            throwInternalServerError();
        }

        verifyFruitExists(id);
        verifyCorrectPayload(fruit);

        fruit.setId(id);

        timeOut();

        // repository.save(fruit);
        // return cache.put(fruit.getId(), fruit);
        LOGGER.info("cacheRepository.upsert(" + fruit.getId() + ", " + fruit + ")");
        return cacheRepository.upsert(fruit);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fruit/{id}")
    public void delete(@PathVariable("id") Long id) {
        if (checkThrowErrors()) {
            throwInternalServerError();
        }
        
        verifyFruitExists(id);

        // repository.deleteById(id);
        // cache.remove(id);
        cacheRepository.delete(id);
        
        timeOut();
    }

    private void verifyFruitExists(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(String.format("Fruit with id=%d was not found", id));
        }
    }

    private void verifyCorrectPayload(Fruit fruit) {
        if (Objects.isNull(fruit)) {
            throw new UnsupportedMediaTypeException("Fruit cannot be null");
        }

        if (Objects.isNull(fruit.getName()) || fruit.getName().trim().length() == 0) {
            throw new UnprocessableEntityException("The name is required!");
        }

        if (!Objects.isNull(fruit.getId())) {
            throw new UnprocessableEntityException("Id field must be generated");
        }
    }

    private void throwInternalServerError() throws ResponseStatusException {
        LOGGER.error(FORCED_INTERNAL_ERROR);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean checkThrowErrors() {
        return SetupController.getThrowErrors();
    }

    private void timeOut() {
        LOGGER.info("DELAY OF " + SetupController.getDelayInMilliseconds() + " WAS ADDED");
        try {
			TimeUnit.MILLISECONDS.sleep(SetupController.getDelayInMilliseconds());
		} catch (InterruptedException e) {
		}
    }
}
