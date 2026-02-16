package com.adasoft.tomcat.integration.common;

import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * Define un caso de prueba HTTP completo.
 * Se utiliza para cargar definiciones de prueba desde archivos JSON.
 *
 * @param name   Nombre descriptivo de la prueba
 * @param method Método HTTP a utilizar
 * @param path   Ruta del endpoint
 * @param query  Parámetros de consulta (query string)
 * @param expect Expectativas de respuesta
 */
public record HttpCaseDefinition(
        String name,
        HttpMethod method,
        String path,
        Map<String, Object> query,
        Expect expect
) {
    /**
     * Genera un nombre para mostrar en los resultados de prueba.
     * Si no hay nombre definido, genera uno a partir del método y path.
     *
     * @return Nombre descriptivo de la prueba
     */
    public String displayName() {
        return (name == null || name.isBlank()) ? (method + " " + path) : name;
    }

    /**
     * Define las expectativas de respuesta para la prueba.
     *
     * @param status             Código de estado HTTP esperado
     * @param bodyType           Tipo de estructura JSON esperada (ARRAY u OBJECT)
     * @param requiredJsonPaths  Lista de rutas JSON que deben existir en la respuesta
     */
    public record Expect(int status, BodyType bodyType, List<String> requiredJsonPaths) {}

    /**
     * Tipo de estructura JSON esperada en el cuerpo de la respuesta.
     */
    public enum BodyType {
        /** La respuesta debe ser un array JSON [] */
        ARRAY,
        /** La respuesta debe ser un objeto JSON {} */
        OBJECT
    }
}
