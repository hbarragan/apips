package com.adasoft.pharmasuite.apips.websocket.config;

import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.domain.MessageAction;
import com.adasoft.pharmasuite.apips.websocket.domain.SubscriptionRegistry;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;

public class WebSocketHandler extends TextWebSocketHandler {

    private final JobQuartzService jobQuartzService;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PING_TOKEN = "__ping__";
    private static final String PONG_TOKEN = "__pong__";

    public WebSocketHandler(JobQuartzService jobQuartzService) {
        this.jobQuartzService = jobQuartzService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LogManagement.info("CONNECTION ESTABLISHED: " + session.getId(), this.getClass());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        if (payload != null && payload.contains(PING_TOKEN)) {
            LogManagement.info("ping: " + payload, this);
            notify(PONG_TOKEN, session);
            return;
        }

        try {
            MessageAction act = mapper.readValue(payload, MessageAction.class);
            String varName = act.getVarName();

            if (act.isAdd()) {
                SubscriptionRegistry.subscribe(varName, session);
                notify("info:Subscribed " + varName, session);
            } else if (act.isRemove()) {
                removeSession(session, varName);
            }

        } catch (Exception e) {
            sendErrorMessageWS(message, session);
        }
    }

    private void removeSession(WebSocketSession session, String varName) throws IOException {
        SubscriptionRegistry.unsubscribe(varName, session);
        SubscriptionRegistry.removeSession(session);
        if (!SubscriptionRegistry.hasSubscribers(varName)) {
            jobQuartzService.remove(varName);
        }
        notify("info:Unsubscribed " + varName, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LogManagement.info("DISCONNECTED: " + session.getId(), this.getClass().getName());

        Set<String> varNames = SubscriptionRegistry.getSubsBySession().get(session);
        if (varNames != null) {
            for (String varName : varNames) {
                try {
                    removeSession(session, varName);
                } catch (IOException e) {
                    LogManagement.error("Error al limpiar sesión cerrada para varName: " + varName, this.getClass().getName());
                }
            }
        }
        SubscriptionRegistry.removeSession(session);
    }

    private void notify(String msg, WebSocketSession session) throws IOException {
        session.sendMessage(new TextMessage(msg));
        LogManagement.info(msg, this.getClass().getName());
    }

    private void sendErrorMessageWS(TextMessage message , WebSocketSession session) throws IOException {
        LogManagement.error("Invalid message: " + message.getPayload(), this.getClass().getName());
        notify("error:Invalid message format", session);
    }
}