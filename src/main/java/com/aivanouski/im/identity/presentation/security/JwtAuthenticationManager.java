package com.aivanouski.im.identity.presentation.security;

import com.aivanouski.im.identity.application.auth.TokenClaims;
import com.aivanouski.im.identity.application.auth.DeviceLookupRepository;
import com.aivanouski.im.identity.application.auth.DeviceRepository;
import com.aivanouski.im.identity.application.auth.TokenService;
import com.aivanouski.im.shared.exception.ApplicationException;
import com.aivanouski.im.identity.domain.DeviceRecord;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
    private final TokenService tokenService;
    private final DeviceLookupRepository deviceLookupRepository;
    private final DeviceRepository deviceRepository;

    public JwtAuthenticationManager(
            TokenService tokenService,
            DeviceLookupRepository deviceLookupRepository,
            DeviceRepository deviceRepository
    ) {
        this.tokenService = tokenService;
        this.deviceLookupRepository = deviceLookupRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken tokenAuth)) {
            return Mono.empty();
        }
        Object credentials = tokenAuth.getCredentials();
        if (!(credentials instanceof String token) || token.isBlank()) {
            return Mono.error(new BadCredentialsException("Missing token"));
        }
        return Mono.fromCallable(() -> validate(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Authentication validate(String token) {
        TokenClaims claims;
        try {
            claims = tokenService.verify(token);
        } catch (ApplicationException ex) {
            throw new BadCredentialsException(ex.getMessage(), ex);
        }
        UUID userId = claims.userId();
        UUID deviceId = claims.deviceId();
        DeviceRecord device = deviceLookupRepository.findById(deviceId)
                .orElseThrow(() -> new BadCredentialsException("Device not found"));
        if (!device.userId().equals(userId) || device.verifiedAt() == null) {
            throw new BadCredentialsException("Device not verified");
        }
        DeviceRecord updated = new DeviceRecord(
                device.id(),
                device.userId(),
                device.publicKey(),
                device.publicKeyAlg(),
                device.challenge(),
                device.challengeExpiresAt(),
                device.verifiedAt(),
                device.createdAt(),
                Instant.now()
        );
        deviceRepository.save(updated);
        return new JwtAuthenticationToken(userId, deviceId, token, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

}
