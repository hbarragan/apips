package com.adasoft.tomcat.integration.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.stream.Stream;

/**
 * Clase base para pruebas de integración (IT Tests).
 * Proporciona configuración común para conectarse al servidor de pruebas
 * tanto en modo HTTP como HTTPS.
 */
public class ITTestCommon {
    // Puerto HTTPS configurado en el servidor
    @Value("${server.port}")
    protected int httpsPort;

    // Indica si el modo HTTP (sin SSL) está habilitado
    @Value("${app.security.https.http.enable:false}")
    protected boolean httpEnabled;

    // Puerto HTTP (sin SSL) si está habilitado
    @Value("${app.security.https.http.port:0}")
    protected int httpPort;

    // URLs base para conexiones HTTP y HTTPS
    private static final String HTTP_BASE_URL = "http://localhost:";
    private static final String HTTPS_BASE_URL = "https://localhost:";
    /**
     * Crea un cliente REST configurado con la URL base del servidor.
     * Utiliza HTTP o HTTPS según la configuración.
     *
     * @return RestClient configurado y listo para usar
     */
    protected RestClient getRestClient() {
        return RestClient.builder()
                .baseUrl(getBaseUrl())
                .build();
    }

    /**
     * Obtiene la URL base del servidor según la configuración.
     *
     * @return URL completa (http://localhost:puerto o https://localhost:puerto)
     */
    protected String getBaseUrl() {
        if (httpEnabled) {
            return HTTP_BASE_URL + httpPort;
        } else {
            return HTTPS_BASE_URL + httpsPort;
        }
    }

    // Añadido: ObjectMapper compartido para las pruebas
    protected final ObjectMapper objectMapper = new ObjectMapper();

    // Añadidos: fábricas para los componentes usados por las pruebas
    protected ClasspathHttpCaseRepository createRepository(String casesPattern) {
        return new ClasspathHttpCaseRepository(objectMapper, casesPattern);
    }

    protected RestClientHttpExecutor createExecutor() {
        return new RestClientHttpExecutor(getRestClient());
    }

    protected HttpCaseRunner createRunner(RestClientHttpExecutor executor) {
        return new HttpCaseRunner(objectMapper, executor);
    }

    // Método centralizado que carga los casos y devuelve los DynamicTest
    protected Stream<DynamicTest> loadAndRunCases(String casesPattern) {
        var repository = createRepository(casesPattern);
        var executor   = createExecutor();
        var runner     = createRunner(executor);

        return repository.load()
                .map(tc -> DynamicTest.dynamicTest(
                        tc.displayName(),
                        () -> runner.run(tc)
                ));
    }
}
