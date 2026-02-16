package com.adasoft.pharmasuite.apips.mcp.config;

import java.util.List;

import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class McpConfig {

    @Value("${mcp.server.name}")
    private String mcpServerName;

    @Value("${server.application.version}")
    private String appVersion;

    public static final String MCP_MESSAGE_ENDPOINT = "/mcp/message";
    public static final String MCP_SSE_ENDPOINT = "/mcp/sse";

    @Bean
    public WebMvcSseServerTransportProvider mcpTransportProvider() {
        return WebMvcSseServerTransportProvider.builder()
                .sseEndpoint(MCP_SSE_ENDPOINT)
                .messageEndpoint(MCP_MESSAGE_ENDPOINT)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> mcpRouter(WebMvcSseServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @Bean
    public McpSyncServer mcpServer(
            WebMvcSseServerTransportProvider transportProvider,
            List<McpServerFeatures.SyncToolSpecification> tools
    ) {
        var capabilities = McpSchema.ServerCapabilities.builder()
                .tools(true)
                .logging()
                .build();
        System.out.println("MCP tools discovered: " + tools.size());

        LogManagement.info("MCP tools discovered: " + tools.size(), this.getClass());
        McpSyncServer mcpServer = McpServer.sync(transportProvider)
                .serverInfo(mcpServerName, appVersion)
                .capabilities(capabilities)
                .build();

        tools.forEach(mcpServer::addTool);
        return mcpServer;
    }
}
