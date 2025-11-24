package com.example.dbi.BüchereiVerwaltung.validation;

import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class MongoSchemaValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchema schema;

    public MongoSchemaValidator(ObjectMapper objectMapper, ResourceLoader resourceLoader) throws IOException {
        this.objectMapper = objectMapper;
        Resource schemaResource = resourceLoader.getResource("classpath:schemas/book-document-schema.json");
        JsonNode schemaNode = objectMapper.readTree(schemaResource.getInputStream());
        this.schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode);
    }

    public void validate(BookDocument document) {
        validate(objectMapper.valueToTree(document));
    }

    public void validate(JsonNode node) {
        Set<ValidationMessage> errors = schema.validate(node);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Mongo Document verletzt Schema: " + errors);
        }
    }
}

