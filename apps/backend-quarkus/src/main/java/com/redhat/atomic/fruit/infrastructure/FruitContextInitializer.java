package com.redhat.atomic.fruit.infrastructure;

import com.redhat.atomic.fruit.Fruit;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Fruit.class }, schemaPackageName = "dg.workshop")
interface FruitContextInitializer extends SerializationContextInitializer {
}
