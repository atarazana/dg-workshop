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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Entity
@ProtoDoc("@Indexed")
public class Fruit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FruitSequence")
    @SequenceGenerator(name = "FruitSequence", sequenceName = "FRUIT_SEQ")    
    private Long id;
    
    private String name;

    private String season;

    public Fruit() {
    }

    @ProtoFactory
    public Fruit(Long id, String name, String season) {
        this.id = id;
        this.name = name;
        this.season = season;
    }
    
    public Fruit(String name, String season) {
        this.name = name;
        this.season = season;
    }

    @ProtoField(number = 1)
    @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ProtoField(number = 2)
    @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ProtoField(number = 3)
    @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }
}
