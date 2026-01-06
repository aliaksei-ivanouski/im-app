package com.aivanouski.im.identity.presentation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.aivanouski.im.identity.application.auth.TokenService;
import com.aivanouski.im.identity.infrastructure.security.JwtProperties;
import com.aivanouski.im.identity.infrastructure.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            HandshakeBearerTokenConverter bearerTokenConverter,
            JwtAuthenticationManager authenticationManager
    ) {
        AuthenticationWebFilter authFilter = new AuthenticationWebFilter(authenticationManager);
        authFilter.setServerAuthenticationConverter(bearerTokenConverter);
        authFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        authFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers("/ws/messages", "/chat/**"));
        authFilter.setAuthenticationFailureHandler((exchange, ex) -> {
            String body = "{\"code\":\"unauthorized\",\"message\":\"" + ex.getMessage() + "\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getExchange().getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return exchange.getExchange().getResponse()
                    .writeWith(reactor.core.publisher.Mono.just(exchange.getExchange().getResponse().bufferFactory().wrap(bytes)));
        });

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/ws/messages").authenticated()
                        .pathMatchers("/chat/**").authenticated()
                        .pathMatchers("/auth/**").permitAll()
                        .anyExchange().permitAll()
                )
                .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public TokenService jwtService(JwtProperties properties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new JwtService(properties, objectMapper);
    }
}
