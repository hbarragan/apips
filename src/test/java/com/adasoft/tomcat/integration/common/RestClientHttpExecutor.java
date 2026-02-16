package com.adasoft.tomcat.integration.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * Implementación de HTTPExecutor usando Spring RestClient.
 * Ejecuta peticiones HTTP reales contra un servidor.
 */
public final class RestClientHttpExecutor implements HTTPExecutor {

    private final RestClient client;

    /**
     * Constructor que recibe un cliente REST configurado.
     *
     * @param client Cliente REST de Spring ya configurado con baseUrl
     */
    public RestClientHttpExecutor(RestClient client) {
        this.client = client;
    }

    /**
     * Ejecuta una petición HTTP usando Spring RestClient.
     *
     * @param request Especificación de la petición (método y URI)
     * @return Resultado con status, headers y body de la respuesta
     */
    @Override
    public HttpResult execute(HttpRequestSpec request) {
        return client
                .method(request.method())
                .uri(request.uri())
                .accept(MediaType.APPLICATION_JSON)
                .exchange((req, response) -> {
                    // Extraer status code
                    int status = response.getStatusCode().value();

                    // Extraer headers
                    HttpHeaders headers = response.getHeaders();

                    // Leer body completo como String en UTF-8
                    String body = new String(
                        response.getBody().readAllBytes(),
                        StandardCharsets.UTF_8
                    );

                    return new HttpResult(status, headers, body);
                });
    }
}
