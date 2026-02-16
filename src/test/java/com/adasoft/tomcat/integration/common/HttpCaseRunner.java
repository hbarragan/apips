package com.adasoft.tomcat.integration.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Ejecuta casos de prueba HTTP definidos en formato JSON.
 * Permite validar status, tipo de contenido, estructura de respuesta y valores específicos.
 */
public final class HttpCaseRunner {

    private static final Logger logger = LoggerFactory.getLogger(HttpCaseRunner.class);
    private static final int MAX_BODY_LOG_LENGTH = 1000; // Máximo de caracteres del body a mostrar en logs

    private final ObjectMapper mapper;       // Para convertir JSON a/desde objetos Java
    private final HTTPExecutor executor;     // Ejecuta peticiones HTTP
    private final boolean verboseLogging;    // Si true, muestra información detallada de cada petición

    /**
     * Constructor que recibe las dependencias necesarias.
     * Por defecto, habilita el logging detallado.
     *
     * @param mapper   Para serializar/deserializar JSON
     * @param executor Para ejecutar peticiones HTTP
     */
    public HttpCaseRunner(ObjectMapper mapper, HTTPExecutor executor) {
        this(mapper, executor, true);
    }

    /**
     * Constructor que permite controlar el nivel de logging.
     *
     * @param mapper         Para serializar/deserializar JSON
     * @param executor       Para ejecutar peticiones HTTP
     * @param verboseLogging Si true, muestra información detallada de peticiones y respuestas
     */
    public HttpCaseRunner(ObjectMapper mapper, HTTPExecutor executor, boolean verboseLogging) {
        this.mapper = mapper;
        this.executor = executor;
        this.verboseLogging = verboseLogging;
    }

    /**
     * Ejecuta un caso de prueba HTTP y valida su resultado.
     *
     * @param tc El caso de prueba a ejecutar
     * @throws Exception Si ocurre un error durante la ejecución o validación
     */
    public void run(HttpCaseDefinition tc) throws Exception {
        HttpResult result = null;
        try {
            // 1. Construir la URI con path y parámetros de consulta
            URI uri = buildUri(tc.path(), tc.query());

            // Log de la petición
            if (verboseLogging) {
                logRequest(tc, uri);
                logger.debug("[DEBUG] Antes de ejecutar la petición HTTP");
            }

            // 2. Ejecutar la petición HTTP
            result = executor.execute(new HttpRequestSpec(tc.method(), uri));

            if (verboseLogging) {
                logger.debug("[DEBUG] Después de ejecutar la petición HTTP. Status: {}", result.statusCode());
            }

            // *** IMPORTANTE: Mostrar la respuesta SIEMPRE, incluso si luego fallan las validaciones ***
            if (verboseLogging) {
                logger.debug("[DEBUG] Antes de logResponse");
                logResponse(tc, result);
                logger.debug("[DEBUG] Después de logResponse");
            }

            // 3. Validar código de estado
            assertThat(result.statusCode())
                    .as(tc.displayName() + " -> status")
                    .isEqualTo(tc.expect().status());

            // 4. Validar que el tipo de contenido sea JSON
            String contentType = getHeaderIgnoreCase(result.headers(), HttpHeaders.CONTENT_TYPE);
            assertThat(contentType)
                    .as(tc.displayName() + " -> content-type")
                    .contains("application/json");

            // 5. Validar el tipo raíz del cuerpo (objeto o array)
            assertBodyRootType(tc, result.body());

            // 6. Validar paths JSON requeridos
            assertRequiredJsonPaths(tc, result.body());

            // 7. Log de éxito
            if (verboseLogging) {
                logSuccess(tc);
            }
        } catch (Exception e) {
            // Log de error CON la respuesta si la tenemos
            if (verboseLogging) {
                if (result != null) {
                    logger.info("[DEBUG] Excepción capturada pero tenemos respuesta, mostrándola:");
                    try {
                        logResponse(tc, result);
                    } catch (Exception logEx) {
                        logger.error("[ERROR] No se pudo mostrar la respuesta: {}", logEx.getMessage());
                    }
                }
                logFailure(tc, e);
            }
            throw e; // Re-lanzar la excepción para que JUnit la capture
        }
    }

    /**
     * Construye una URI a partir de un path y parámetros de consulta.
     *
     * @param path  Ruta de la API
     * @param query Mapa de parámetros de consulta
     * @return URI construida
     */
    private URI buildUri(String path, Map<String, Object> query) {
        var builder = UriComponentsBuilder.fromPath(path);

        // Añadir parámetros de consulta si existen
        if (query != null && !query.isEmpty()) {
            query.forEach((key, value) -> addQueryParam(builder, key, value));
        }

        // La URL base ya se configura en RestClient.builder().baseUrl(...)
        return builder.build().toUri();
    }

    /**
     * Añade un parámetro de consulta a la URI, manejando diferentes tipos de valores
     * incluyendo valores OData, colecciones y arrays.
     *
     * @param builder Constructor de URI
     * @param key     Clave del parámetro
     * @param value   Valor del parámetro (puede ser String, Collection, Array u otro objeto)
     */
    private static void addQueryParam(UriComponentsBuilder builder, String key, Object value) {
        // No procesar valores nulos
        if (value == null) {
            return;
        }

        // CASO 1: String con formato OData o expresión de filtro compleja
        if (value instanceof String s && isComplexQueryValue(s)) {
            builder.queryParam(key, value.toString());
            return;
        }

        // CASO 2: Colección de valores
        if (value instanceof Collection<?> collection) {
            collection.stream()
                    .filter(item -> item != null)
                    .forEach(item -> builder.queryParam(key, item));
            return;
        }

        // CASO 3: Array de valores
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            for (Object item : array) {
                if (item != null) {
                    builder.queryParam(key, item);
                }
            }
            return;
        }

        // CASO 4: Valor simple
        builder.queryParam(key, value);
    }

    /**
     * Determina si un valor de consulta es complejo (formato OData o similar).
     *
     * @param value Valor a evaluar
     * @return true si es un valor complejo
     */
    private static boolean isComplexQueryValue(String value) {
        return value.contains(" ") || value.contains("'") || value.contains("=");
    }

    /**
     * Valida que el cuerpo de la respuesta sea del tipo esperado (Array u Objeto).
     *
     * @param tc   Caso de prueba que contiene las expectativas
     * @param body Cuerpo de la respuesta en formato JSON
     * @throws Exception Si ocurre un error al procesar el JSON
     */
    private void assertBodyRootType(HttpCaseDefinition tc, String body) throws Exception {
        JsonNode root = mapper.readTree(body);
        String testName = tc.displayName();

        // Validar el tipo de estructura JSON recibida
        if (tc.expect().bodyType() == HttpCaseDefinition.BodyType.ARRAY) {
            assertThat(root.isArray())
                    .as(testName + " -> bodyType=ARRAY")
                    .isTrue();
        } else {
            assertThat(root.isObject())
                    .as(testName + " -> bodyType=OBJECT")
                    .isTrue();
        }
    }

    /**
     * Valida que el cuerpo de la respuesta contenga todos los paths JSON requeridos.
     *
     * @param tc   Caso de prueba que contiene las rutas a validar
     * @param body Cuerpo de la respuesta en formato JSON
     */
    private static void assertRequiredJsonPaths(HttpCaseDefinition tc, String body) {
        List<String> paths = tc.expect().requiredJsonPaths();
        if (paths == null || paths.isEmpty()) {
            return; // No hay paths para validar
        }

        // Parsear el JSON una sola vez para todas las validaciones
        Object jsonDocument = JsonPath.parse(body).json();
        String testName = tc.displayName();

        // Verificar cada ruta JSON esperada
        for (String path : paths) {
            // JsonPath.read lanza excepción automáticamente si la ruta no existe
            Object value = JsonPath.read(jsonDocument, path);
            assertThat(value)
                    .as(testName + " -> jsonPath " + path)
                    .isNotNull();
        }
    }

    /**
     * Busca un encabezado HTTP por su nombre, ignorando mayúsculas/minúsculas.
     *
     * @param headers Encabezados HTTP
     * @param name    Nombre del encabezado a buscar
     * @return Valor del encabezado o cadena vacía si no existe
     */
    private static String getHeaderIgnoreCase(HttpHeaders headers, String name) {
        if (headers == null) {
            return "";
        }

        // Buscar el encabezado ignorando mayúsculas/minúsculas
        for (Map.Entry<String, List<String>> entry : headers.headerSet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                List<String> values = entry.getValue();
                return (values != null && !values.isEmpty()) ? values.get(0) : "";
            }
        }

        return "";
    }

    /**
     * Registra información sobre la petición HTTP que se va a ejecutar.
     * Usa System.out para garantizar que siempre se muestre.
     *
     * @param tc  Caso de prueba
     * @param uri URI completa de la petición
     */
    private void logRequest(HttpCaseDefinition tc, URI uri) {
        String message = "\n" +
                "═══════════════════════════════════════════════════════════════\n" +
                "🔵 EJECUTANDO: " + tc.displayName() + "\n" +
                "───────────────────────────────────────────────────────────────\n" +
                "   Método:  " + tc.method() + "\n" +
                "   URI:     " + uri + "\n" +
                "═══════════════════════════════════════════════════════════════";

        System.out.println(message);
    }

    /**
     * Registra información sobre la respuesta HTTP recibida.
     * Usa System.out para garantizar que siempre se muestre.
     *
     * @param tc     Caso de prueba
     * @param result Resultado de la petición HTTP
     */
    private void logResponse(HttpCaseDefinition tc, HttpResult result) {
        String bodyToLog = formatBodyForLog(result.body());
        String bodyPreview = result.body().length() > MAX_BODY_LOG_LENGTH
            ? "(truncado, longitud total: " + result.body().length() + " caracteres)"
            : "";

        String message = "\n" +
                "───────────────────────────────────────────────────────────────\n" +
                "✅ RESPUESTA: " + tc.displayName() + "\n" +
                "───────────────────────────────────────────────────────────────\n" +
                "   Status:       " + result.statusCode() + " " + getStatusDescription(result.statusCode()) + "\n" +
                "   Content-Type: " + getHeaderIgnoreCase(result.headers(), HttpHeaders.CONTENT_TYPE) + "\n" +
                "   Body Length:  " + result.body().length() + " bytes\n" +
                "   Body:         " + bodyToLog + "\n" +
                "   " + bodyPreview + "\n" +
                "═══════════════════════════════════════════════════════════════\n";

        System.out.println(message);
    }

    /**
     * Formatea el cuerpo de la respuesta para mostrarlo en los logs.
     * Si es JSON, intenta formatearlo de manera legible.
     *
     * @param body Cuerpo de la respuesta
     * @return Cuerpo formateado para log
     */
    private String formatBodyForLog(String body) {
        if (body == null || body.isBlank()) {
            return "(vacío)";
        }

        try {
            // Intentar formatear como JSON con indentación
            JsonNode jsonNode = mapper.readTree(body);
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

            // Truncar si es muy largo
            if (prettyJson.length() > MAX_BODY_LOG_LENGTH) {
                return prettyJson.substring(0, MAX_BODY_LOG_LENGTH) + "\n...";
            }
            return prettyJson;
        } catch (Exception e) {
            // Si no es JSON válido, devolver el texto truncado
            return body.length() > MAX_BODY_LOG_LENGTH
                ? body.substring(0, MAX_BODY_LOG_LENGTH) + "..."
                : body;
        }
    }

    /**
     * Obtiene una descripción del código de estado HTTP.
     *
     * @param statusCode Código de estado
     * @return Descripción del código
     */
    private String getStatusDescription(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }

    /**
     * Registra que el test ha pasado exitosamente.
     * Usa System.out para garantizar que siempre se muestre.
     *
     * @param tc Caso de prueba
     */
    private void logSuccess(HttpCaseDefinition tc) {
        String message = "\n" +
                "✅✅✅ TEST PASADO: " + tc.displayName() + "\n" +
                "═══════════════════════════════════════════════════════════════\n";

        System.out.println(message);
    }

    /**
     * Registra que el test ha fallado.
     * Usa System.err para garantizar que siempre se muestre.
     *
     * @param tc Caso de prueba
     * @param e  Excepción que causó el fallo
     */
    private void logFailure(HttpCaseDefinition tc, Exception e) {
        String message = "\n" +
                "❌❌❌ TEST FALLIDO: " + tc.displayName() + "\n" +
                "───────────────────────────────────────────────────────────────\n" +
                "   Razón: " + e.getMessage() + "\n" +
                "═══════════════════════════════════════════════════════════════\n";

        System.err.println(message);
    }
}
