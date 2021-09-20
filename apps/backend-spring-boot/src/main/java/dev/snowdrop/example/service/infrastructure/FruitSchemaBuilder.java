package dev.snowdrop.example.service.infrastructure;

import dev.snowdrop.example.service.Fruit;
import dev.snowdrop.example.service.FruitsBySeason;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Fruit.class, FruitsBySeason.class }, schemaPackageName = "dg.workshop")
interface FruitSchemaBuilder extends GeneratedSchema {
}