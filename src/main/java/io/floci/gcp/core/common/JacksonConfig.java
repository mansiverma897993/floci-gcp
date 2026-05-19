package io.floci.gcp.core.common;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class JacksonConfig implements ObjectMapperCustomizer {

    private static final int MAX_STRING_LENGTH = 100_000_000;

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(MAX_STRING_LENGTH)
                        .build());
    }
}
