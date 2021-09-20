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

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoDoc("@Indexed")
public class FruitsBySeason {

    private String season;
    private List<Fruit> fruits;

    public FruitsBySeason() {
    }

    @ProtoFactory
    public FruitsBySeason(String season, List<Fruit> fruits) {
        this.season = season;
        this.fruits = fruits;
    }

    @ProtoField(number = 1)
    @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }


    @ProtoField(number = 2, collectionImplementation = ArrayList.class)
    @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
    public List<Fruit> getFruits() {
        return fruits;
    }

    public void setFruits(List<Fruit> fruits) {
        this.fruits = fruits;
    }
}
