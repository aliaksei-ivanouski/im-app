package com.aivanouski.im.identity.presentation.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class HandshakeBearerTokenConverter implements ServerAuthenticationConverter {
    private static final String ACCESS_TOKEN_PARAM = "access_token";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(extractToken(exchange))
                .map(token -> new UsernamePasswordAuthenticationToken(token, token));
    }

    private Optional<String> extractToken(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            return Optional.of(auth.substring(BEARER_PREFIX.length()).trim());
        }
        String queryToken = request.getQueryParams().getFirst(ACCESS_TOKEN_PARAM);
        if (queryToken != null && !queryToken.isBlank()) {
            return Optional.of(queryToken.trim());
        }
        return Optional.ofNullable(exchange.getRequest().getCookies().getFirst(ACCESS_TOKEN_COOKIE))
                .map(cookie -> cookie.getValue().trim())
                .filter(token -> !token.isBlank());
    }
}
