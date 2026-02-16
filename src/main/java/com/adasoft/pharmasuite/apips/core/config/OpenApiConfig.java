package com.adasoft.pharmasuite.apips.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${springdoc.custom.title}")
    private String title;

    @Value("${springdoc.custom.description}")
    private String description;

    @Value("${springdoc.custom.terms-of-service}")
    private String termsOfService;

    @Value("${springdoc.custom.version}")
    private String version;

    @Value("${springdoc.custom.contact.name}")
    private String contactName;

    @Value("${springdoc.custom.contact.url}")
    private String contactUrl;

    @Value("${springdoc.custom.contact.email}")
    private String contactEmail;

    @Value("${springdoc.custom.license.name}")
    private String licenseName;

    @Value("${springdoc.custom.license.url}")
    private String licenseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .termsOfService(termsOfService)
                        .version(version)
                        .contact(new Contact()
                                .name(contactName)
                                .url(contactUrl)
                                .email(contactEmail))
                        .license(new License()
                                .name(licenseName)
                                .url(licenseUrl))
                );
    }
}