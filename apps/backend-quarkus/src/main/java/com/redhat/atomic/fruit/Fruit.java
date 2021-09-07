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