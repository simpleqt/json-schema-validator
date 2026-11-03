/*
 * Copyright (c) 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * UnevaluatedItemsValidatorTest.
 */
class UnevaluatedItemsValidatorTest {
    @Test
    void unevaluatedItemsFalse() {
        String schemaData = "{\r\n"
                + "  \"oneOf\": [\r\n"
                + "    { \r\n"
                + "      \"type\" : \"array\" ,\r\n"
                + "      \"prefixItems\" : [\r\n"
                + "        { \"type\" : \"integer\" }\r\n"
                + "      ]\r\n"
                + "    }\r\n"
                + "  ],\r\n"
                + "  \"unevaluatedItems\" : false\r\n"
                + "}";
        String inputData = "[1,2,3]";
        Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schemaData);
        List<Error> messages = schema.validate(inputData, InputFormat.JSON);
        assertEquals(2, messages.size());
        List<Error> assertions = messages.stream().collect(Collectors.toList());
        assertEquals("unevaluatedItems", assertions.get(0).getKeyword());
        assertEquals("", assertions.get(0).getInstanceLocation().toString());
        assertEquals("/unevaluatedItems", assertions.get(0).getEvaluationPath().toString());
        assertEquals("unevaluatedItems", assertions.get(1).getKeyword());
        assertEquals("", assertions.get(1).getInstanceLocation().toString());
        assertEquals("/unevaluatedItems", assertions.get(1).getEvaluationPath().toString());
    }

    @Test
    void unevaluatedItemsSchema() {
        String schemaData = "{\r\n"
                + "  \"oneOf\": [\r\n"
                + "    { \r\n"
                + "      \"type\" : \"array\" ,\r\n"
                + "      \"prefixItems\" : [\r\n"
                + "        { \"type\" : \"integer\" }\r\n"
                + "      ]\r\n"
                + "    }\r\n"
                + "  ],\r\n"
                + "  \"unevaluatedItems\" : { \"type\" : \"string\" }\r\n"
                + "}";
        String inputData = "[1,2,3]";
        Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schemaData);
        List<Error> messages = schema.validate(inputData, InputFormat.JSON);
        assertEquals(2, messages.size());
        List<Error> assertions = messages.stream().collect(Collectors.toList());
        assertEquals("type", assertions.get(0).getKeyword());
        assertEquals("/1", assertions.get(0).getInstanceLocation().toString());
        assertEquals("/unevaluatedItems/type", assertions.get(0).getEvaluationPath().toString());
        assertEquals("type", assertions.get(1).getKeyword());
        assertEquals("/2", assertions.get(1).getInstanceLocation().toString());
        assertEquals("/unevaluatedItems/type", assertions.get(1).getEvaluationPath().toString());
    }

    static Stream<Arguments> unknownKeywordThatSharesApplicatorName() {
        String additionalItemsRoot = "{\r\n"
                + "  \"$schema\" : \"https://json-schema.org/draft/2019-09/schema\",\r\n"
                + "  \"$id\" : \"https://www.example.org/root-unknown-additional-items.json\",\r\n"
                + "  \"$ref\" : \"sub-2020-additional-items.json\",\r\n"
                + "  \"unevaluatedItems\" : false\r\n"
                + "}";
        String additionalItemsSub = "{\r\n"
                + "  \"$schema\" : \"https://json-schema.org/draft/2020-12/schema\",\r\n"
                + "  \"$id\" : \"https://www.example.org/sub-2020-additional-items.json\",\r\n"
                + "  \"additionalItems\" : true\r\n"
                + "}";
        String containsRoot = "{\r\n"
                + "  \"$schema\" : \"https://json-schema.org/draft/2020-12/schema\",\r\n"
                + "  \"$id\" : \"https://www.example.org/root-unknown-contains.json\",\r\n"
                + "  \"allOf\" : [ { \"$ref\" : \"sub-draft4-contains.json\" } ],\r\n"
                + "  \"unevaluatedItems\" : false\r\n"
                + "}";
        String containsSub = "{\r\n"
                + "  \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\r\n"
                + "  \"id\" : \"https://www.example.org/sub-draft4-contains.json\",\r\n"
                + "  \"contains\" : false\r\n"
                + "}";
        return Stream.of(
                Arguments.of("unknown additionalItems in a 2020-12 subschema",
                        "https://www.example.org/root-unknown-additional-items.json",
                        Map.of("https://www.example.org/root-unknown-additional-items.json", additionalItemsRoot,
                                "https://www.example.org/sub-2020-additional-items.json", additionalItemsSub),
                        "[1,2]"),
                Arguments.of("unknown contains in a Draft 4 subschema",
                        "https://www.example.org/root-unknown-contains.json",
                        Map.of("https://www.example.org/root-unknown-contains.json", containsRoot,
                                "https://www.example.org/sub-draft4-contains.json", containsSub),
                        "[1,2]"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unknownKeywordThatSharesApplicatorName")
    void annotationCollectionDoesNotChangeUnknownKeywordResult(String description, String rootIri,
            Map<String, String> schemas, String inputData) {
        List<String> withoutAnnotations = validateUnknownKeywordSchema(rootIri, schemas, inputData, false);
        List<String> withAnnotations = validateUnknownKeywordSchema(rootIri, schemas, inputData, true);
        assertEquals(2, withoutAnnotations.size());
        assertEquals(withoutAnnotations, withAnnotations);
    }

    private List<String> validateUnknownKeywordSchema(String rootIri, Map<String, String> schemas, String inputData,
            boolean collectAnnotations) {
        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2019_09,
                builder -> builder.schemas(schemas));
        Schema schema = schemaRegistry.getSchema(SchemaLocation.of(rootIri));
        return schema.validate(inputData, InputFormat.JSON,
                executionContext -> executionContext.executionConfig(config -> config
                        .annotationCollectionEnabled(collectAnnotations).annotationCollectionFilter(keyword -> true)))
                .stream().map(error -> error.getKeyword() + "|" + error.getInstanceLocation() + "|"
                        + error.getEvaluationPath())
                .collect(Collectors.toList());
    }
}
