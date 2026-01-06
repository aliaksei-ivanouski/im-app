package com.aivanouski.im.identity.presentation.rest;

import com.aivanouski.im.identity.application.auth.AuthService;
import com.aivanouski.im.identity.application.auth.AuthStartResult;
import com.aivanouski.im.identity.domain.auth.AuthTokens;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/auth/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AuthStartResponse> start(@RequestBody AuthStartRequest request) {
        return Mono.fromCallable(() -> authService.startAuth(request.phone(), request.deviceId()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(AuthController::toResponse);
    }

    @PostMapping(path = "/auth/device/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TokenResponse> verify(@RequestBody VerifyDeviceRequest request) {
        return Mono.fromCallable(() -> authService.verifyDevice(
                        request.phone(),
                        request.deviceId(),
                        request.otp(),
                        request.devicePublicKey(),
                        request.publicKeyAlg(),
                        request.signature()
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .map(AuthController::toTokenResponse);
    }

    @PostMapping(path = "/auth/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        return Mono.fromCallable(() -> authService.refresh(request.deviceId(), request.refreshToken()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(AuthController::toTokenResponse);
    }

    private static AuthStartResponse toResponse(AuthStartResult result) {
        return new AuthStartResponse(
                result.status(),
                result.challenge(),
                result.challengeExpiresAt() == null ? null : result.challengeExpiresAt().toString(),
                result.otpExpiresAt() == null ? null : result.otpExpiresAt().toString()
        );
    }

    private static TokenResponse toTokenResponse(AuthTokens tokens) {
        return new TokenResponse(
                tokens.accessToken().value(),
                tokens.accessToken().expiresInSeconds(),
                tokens.refreshToken().value(),
                tokens.refreshToken().expiresInSeconds()
        );
    }

}
