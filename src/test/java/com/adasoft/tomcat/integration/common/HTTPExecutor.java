package com.adasoft.tomcat.integration.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;

/**
 * Interfaz para ejecutar peticiones HTTP.
 * Abstrae la implementación concreta del cliente HTTP utilizado.
 */
public interface HTTPExecutor {
    /**
     * Ejecuta una petición HTTP y devuelve el resultado.
     *
     * @param request Especificación de la petición HTTP
     * @return Resultado de la petición HTTP
     */
    HttpResult execute(HttpRequestSpec request);
}

/**
 * Especificación de una petición HTTP.
 * Contiene el método HTTP y la URI a invocar.
 *
 * @param method Método HTTP (GET, POST, PUT, DELETE, etc.)
 * @param uri    URI completa de la petición
 */
record HttpRequestSpec(HttpMethod method, URI uri) {}

/**
 * Resultado de una petición HTTP.
 * Contiene el código de estado, encabezados y cuerpo de la respuesta.
 *
 * @param statusCode Código de estado HTTP (200, 404, 500, etc.)
 * @param headers    Encabezados de la respuesta HTTP
 * @param body       Cuerpo de la respuesta en formato String
 */
record HttpResult(int statusCode, HttpHeaders headers, String body) {}