package com.adasoft.pharmasuite.apips.mcp.config;

import io.modelcontextprotocol.server.McpServerFeatures;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Configuration
public class ToolAutoDiscoveryConfig {

    @Bean
    public List<McpServerFeatures.SyncToolSpecification> tools(ListableBeanFactory bf) {

        String[] names = bf.getBeanNamesForAnnotation(Component.class); // o todos si quieres

        List<Object> toolBeans = new ArrayList<>();
        for (String name : names) {
            Class<?> type = bf.getType(name);
            if (type != null && hasToolMethod(type)) {
                toolBeans.add(bf.getBean(name));
            }
        }

        if (toolBeans.isEmpty()) return List.of();

        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(toolBeans.toArray())
                .build()
                .getToolCallbacks();

        return McpToolUtils.toSyncToolSpecifications(callbacks);
    }


    private boolean hasToolMethod(Class<?> type) {
        for (Method m : type.getMethods()) {
            if (m.isAnnotationPresent(Tool.class)) {
                return true;
            }
        }
        return false;
    }
}
