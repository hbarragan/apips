package com.adasoft.pharmasuite.apips.core.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Value("${app.security.sso.enabled}")
    private boolean securityEnabled;
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger",            // la UI
            "/swagger-ui/**",      // estáticos de Swagger UI
            "/swagger-ui.html",    // acceso directo UI
            "/openapi.yaml",       // tu especificación estática
            "/v3/api-docs/**",     // especificación dinámica
            "/health", "/health/**",
            "/mcp/**",
            "/sse", "/sse/**"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui/index.html");
    }


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (securityEnabled) {
            http
                    .cors(Customizer.withDefaults())
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(SWAGGER_WHITELIST).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight
                            .anyRequest().authenticated()
                    )
                    .oauth2Login(Customizer.withDefaults())
                    .oauth2Client(Customizer.withDefaults())
                    .logout(l -> l.logoutSuccessUrl("/").permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
        }
        return http.build();
    }

}