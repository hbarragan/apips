package com.adasoft.pharmasuite.apips.websocket.domain;

import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionRegistry {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    @Getter
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Getter
    private static final Map<String, Set<WebSocketSession>> subsByMsg = new ConcurrentHashMap<>();

    @Getter
    private static final Map<WebSocketSession, Set<String>> subsBySession = new ConcurrentHashMap<>();

    public static void subscribe(String varName, WebSocketSession session) {
        sessions.add(session);
        subsByMsg.computeIfAbsent(varName, k -> ConcurrentHashMap.newKeySet()).add(session);
        subsBySession.computeIfAbsent(session, k -> ConcurrentHashMap.newKeySet()).add(varName);
    }

    public static void unsubscribe(String varName, WebSocketSession session) {
        Set<WebSocketSession> subscribers = subsByMsg.get(varName);
        if (subscribers != null) {
            subscribers.remove(session);
            if (subscribers.isEmpty()) {
                subsByMsg.remove(varName);
            }
        }

        Set<String> varNames = subsBySession.get(session);
        if (varNames != null) {
            varNames.remove(varName);
            if (varNames.isEmpty()) {
                subsBySession.remove(session);
            }
        }
    }

    public static void removeSession(WebSocketSession session) {
        sessions.remove(session);

        Set<String> subscribedVars = subsBySession.remove(session);
        if (subscribedVars != null) {
            for (String varName : subscribedVars) {
                Set<WebSocketSession> subscribers = subsByMsg.get(varName);
                if (subscribers != null) {
                    subscribers.remove(session);
                    if (subscribers.isEmpty()) {
                        subsByMsg.remove(varName);
                    }
                }
            }
        }
    }

    public static boolean hasSubscribers(String varName) {
        Set<WebSocketSession> subscribers = subsByMsg.get(varName);
        return subscribers != null && !subscribers.isEmpty();
    }

    public static void broadcast(String varName, String payload) throws JsonProcessingException {
        Set<WebSocketSession> subscribers = subsByMsg.getOrDefault(varName, Collections.emptySet());


        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setVarName(varName);
        messageResponse.setData(payload);

        String json = OBJECT_MAPPER.writeValueAsString(messageResponse);
        TextMessage message = new TextMessage(json);

        for (WebSocketSession session : subscribers) {
            if (session.isOpen()) {

                boolean send = false;
                int count = 0;
                int maxCount = 3;
                long timeSleep = 2000;

                while (!send && count < maxCount) {
                    try {
                        session.sendMessage(message);
                        LogManagement.info("Send msg to websocket id: " + session.getId(), SubscriptionRegistry.class);
                        send = true;
                    } catch (IOException e) {
                        count++;
                        LogManagement.error("Error sending message to session: " + session.getId(), SubscriptionRegistry.class);
                        if (count < maxCount) {
                            try {
                                Thread.sleep(timeSleep);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
