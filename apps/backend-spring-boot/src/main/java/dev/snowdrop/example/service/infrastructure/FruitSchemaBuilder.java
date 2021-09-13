package dev.snowdrop.example.service.infrastructure;

import dev.snowdrop.example.service.Fruit;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Fruit.class }, schemaPackageName = "dg.workshop")
interface FruitSchemaBuilder extends GeneratedSchema {
}