package com.adasoft.tomcat.integration.common.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

public final class ClasspathWsCaseRepository {
    private final ObjectMapper mapper;
    private final String pattern;

    public ClasspathWsCaseRepository(ObjectMapper mapper, String pattern) {
        this.mapper = mapper;
        this.pattern = pattern;
    }

    public Stream<WsCaseDefinition> load() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(pattern);
            Arrays.sort(resources, Comparator.comparing(r -> r.getFilename() == null ? "" : r.getFilename()));
            return Arrays.stream(resources).map(this::loadCaseFromFile);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron cargar casos WS con el patrón: " + pattern, e);
        }
    }

    private WsCaseDefinition loadCaseFromFile(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return mapper.readValue(is, WsCaseDefinition.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error al leer caso WS desde archivo: " + resource.getFilename(), e);
        }
    }
}

