package com.adasoft.tomcat.integration.common.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class WsCaseRunner {
    private static final Logger logger = LoggerFactory.getLogger(WsCaseRunner.class);

    private final ObjectMapper mapper;
    private final RestClient restClient;

    public WsCaseRunner(ObjectMapper mapper, RestClient restClient) {
        this.mapper = mapper;
        this.restClient = restClient;
    }

    public void run(WsCaseDefinition tc, int serverPort) throws Exception {
        List<String> messages = new CopyOnWriteArrayList<>();

        // Cliente WebSocket JDK
        var httpClient = HttpClient.newHttpClient();
        var listener = new WebSocket.Listener() {
            private final StringBuilder sb = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                sb.append(data);
                if (last) {
                    String payload = sb.toString();
                    messages.add(payload);
                    logger.info("[WS] Mensaje recibido: {}", payload);
                    sb.setLength(0);
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                webSocket.request(1);
                return null;
            }

            @Override
            public void onOpen(WebSocket webSocket) {
                logger.info("[WS] Conexión abierta");
                webSocket.request(1);
            }
        };

        URI wsUri = new URI(String.format("ws://localhost:%d/ws", serverPort));
        var wsFuture = httpClient.newWebSocketBuilder().buildAsync(wsUri, listener);
        WebSocket webSocket = wsFuture.join();

        try {
            if (tc.getSubscribeMessage() != null) {
                String msg = mapper.writeValueAsString(tc.getSubscribeMessage());
                webSocket.sendText(msg, true).join();
                logger.info("[WS] Enviado subscribe: {}", msg);

                // Si se define un expect.subscribeAck, esperar a recibirlo antes de proseguir
                if (tc.getExpect() != null && tc.getExpect().getSubscribeAck() != null) {
                    String ack = tc.getExpect().getSubscribeAck();
                    Awaitility.await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).until(() -> {
                        return messages.stream().anyMatch(m -> m != null && m.contains(ack));
                    });
                    logger.info("[WS] Subscribe ack recibido: {}", ack);
                }
            }

            // Ejecutar llamadas HTTP en orden
            if (tc.getHttpCalls() != null && !tc.getHttpCalls().isEmpty()) {
                int callIndex = 0;
                var resolved = tc.getExpect() != null ? tc.getExpect().resolvedExpectedMessagesPerCall() : null;
                for (var call : tc.getHttpCalls()) {
                    callIndex++;
                    var uriBuilder = org.springframework.web.util.UriComponentsBuilder.fromPath(call.getPath());
                    if (call.getQuery() != null) {
                        call.getQuery().forEach((k, v) -> uriBuilder.queryParam(k, v));
                    }
                    var uri = uriBuilder.build().toUri();
                    logger.info("[WS] Ejecutando HTTP {} {}", call.getMethod(), uri);

                    int beforeCallCount = messages.size();

                    var result = restClient
                            .method(HttpMethod.valueOf(call.getMethod()))
                            .uri(uri)
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange((req, resp) -> {
                                int status = resp.getStatusCode().value();
                                HttpHeaders headers = resp.getHeaders();
                                String body = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                return new SimpleHttpResult(status, headers, body);
                            });

                    assertThat(result.status()).as(tc.getName() + " -> http status").isEqualTo(200);

                    // Evaluar expectativas WS para esta llamada si existen
                    if (resolved != null && !resolved.isEmpty()) {
                        int expectedMessages = resolved.get(Math.min(callIndex - 1, resolved.size() - 1));
                        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).until(() -> messages.size() >= beforeCallCount + expectedMessages);
                        int afterCallCount = messages.size();
                        int newMessagesCount = afterCallCount - beforeCallCount;
                        assertThat(newMessagesCount).as(tc.getName() + " -> ws new messages after call " + callIndex).isGreaterThanOrEqualTo(expectedMessages);

                        // Extraer nuevos mensajes y validarlos (JSONPaths) si aplica
                        if (tc.getExpect().getRequiredJsonPathsInWs() != null && !tc.getExpect().getRequiredJsonPathsInWs().isEmpty()) {
                            List<String> newMessages = messages.subList(beforeCallCount, afterCallCount);
                            String jsonCandidate = null;
                            for (int i = newMessages.size() - 1; i >= 0; i--) {
                                String candidate = newMessages.get(i);
                                try {
                                    mapper.readTree(candidate);
                                    jsonCandidate = candidate;
                                    break;
                                } catch (Exception ex) {
                                    // no es JSON, continuar
                                }
                            }
                            if (jsonCandidate == null) {
                                throw new AssertionError(tc.getName() + " -> expected a JSON message among new messages to validate requiredJsonPathsInWs but none was received. NewMessages: " + newMessages);
                            }
                            Object jsonDoc = JsonPath.parse(jsonCandidate).json();
                            for (var path : tc.getExpect().getRequiredJsonPathsInWs()) {
                                Object value = JsonPath.read(jsonDoc, path);
                                assertThat(value).as(tc.getName() + " -> ws jsonPath " + path).isNotNull();
                            }
                        }
                    } else if (tc.getExpect() != null && tc.getExpect().getMessagesAfterCalls() != null) {
                        // Backward compatibility: comportamiento antiguo
                        int expectedMessages = tc.getExpect().getMessagesAfterCalls().get(Math.min(callIndex - 1, tc.getExpect().getMessagesAfterCalls().size() - 1));
                        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).until(() -> messages.size() >= expectedMessages);
                        assertThat(messages.size()).as(tc.getName() + " -> ws messages after call " + callIndex).isGreaterThanOrEqualTo(expectedMessages);
                    }
                }
            }

        } finally {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
            }
        }
    }

    private static final class SimpleHttpResult {
        private final int status;
        private final HttpHeaders headers;
        private final String body;

        SimpleHttpResult(int status, HttpHeaders headers, String body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        public int status() { return status; }
        public HttpHeaders headers() { return headers; }
        public String body() { return body; }
    }
}
