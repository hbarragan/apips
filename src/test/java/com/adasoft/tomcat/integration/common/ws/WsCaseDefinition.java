package com.adasoft.tomcat.integration.common.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class WsCaseDefinition {
    private String name;
    private Map<String, Object> subscribeMessage;
    private List<HttpCall> httpCalls;
    private Expect expect;

    public static final class HttpCall {
        private String method;
        private String path;
        private Map<String, Object> query;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Map<String, Object> getQuery() { return query; }
        public void setQuery(Map<String, Object> query) { this.query = query; }
    }

    public static final class Expect {
        private List<Integer> messagesAfterCalls;
        private List<String> requiredJsonPathsInWs;
        private String subscribeAck; // nuevo: texto esperado en ACK de suscripción
        private List<Integer> expectedMessagesPerCall; // nuevo: delta esperada por llamada

        public List<Integer> getMessagesAfterCalls() { return messagesAfterCalls; }
        public void setMessagesAfterCalls(List<Integer> messagesAfterCalls) { this.messagesAfterCalls = messagesAfterCalls; }
        public List<String> getRequiredJsonPathsInWs() { return requiredJsonPathsInWs; }
        public void setRequiredJsonPathsInWs(List<String> requiredJsonPathsInWs) { this.requiredJsonPathsInWs = requiredJsonPathsInWs; }

        public String getSubscribeAck() { return subscribeAck; }
        public void setSubscribeAck(String subscribeAck) { this.subscribeAck = subscribeAck; }

        public List<Integer> getExpectedMessagesPerCall() { return expectedMessagesPerCall; }
        public void setExpectedMessagesPerCall(List<Integer> expectedMessagesPerCall) { this.expectedMessagesPerCall = expectedMessagesPerCall; }

        // retrocompat: si expectedMessagesPerCall no está definido, devolver messagesAfterCalls
        public List<Integer> resolvedExpectedMessagesPerCall() {
            return (expectedMessagesPerCall != null && !expectedMessagesPerCall.isEmpty()) ? expectedMessagesPerCall : messagesAfterCalls;
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Object> getSubscribeMessage() { return subscribeMessage; }
    public void setSubscribeMessage(Map<String, Object> subscribeMessage) { this.subscribeMessage = subscribeMessage; }
    public List<HttpCall> getHttpCalls() { return httpCalls; }
    public void setHttpCalls(List<HttpCall> httpCalls) { this.httpCalls = httpCalls; }
    public Expect getExpect() { return expect; }
    public void setExpect(Expect expect) { this.expect = expect; }
}
