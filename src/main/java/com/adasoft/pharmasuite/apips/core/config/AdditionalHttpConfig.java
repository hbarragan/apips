package com.adasoft.pharmasuite.apips.core.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdditionalHttpConfig {

    @Value("${app.security.https.enabled:false}")
    private boolean httpsEnabled;

    @Value("${app.security.https.http.enable:false}")
    private boolean httpEnabled;

    @Value("${app.security.https.http.port:9101}")
    private int httpPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> additionalHttpConnector() {
        return factory -> {
            if (httpsEnabled && httpEnabled) {
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setScheme("http");
                connector.setSecure(false);
                connector.setPort(httpPort);
                factory.addAdditionalConnectors(connector);
            }
        };
    }
}
