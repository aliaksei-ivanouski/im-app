package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.identity.domain.DeviceRecord;
import com.aivanouski.im.identity.domain.RefreshTokenRecord;
import com.aivanouski.im.identity.domain.auth.AuthTokens;
import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceRefreshTest {
    @Test
    void refreshRejectsRevokedToken() {
        Fixture fixture = new Fixture();
        RefreshTokenRecord revoked = fixture.tokenRecord(fixture.deviceId, Instant.now().plusSeconds(60), Instant.now());
        when(fixture.refreshTokenLookupRepository.findByTokenHash("hash")).thenReturn(Optional.of(revoked));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> fixture.service.refresh(fixture.deviceId, "rt")
        );

        assertEquals("Refresh token expired.", ex.getMessage());
        verifyNoInteractions(fixture.deviceLookupRepository);
    }

    @Test
    void refreshRejectsExpiredToken() {
        Fixture fixture = new Fixture();
        RefreshTokenRecord expired = fixture.tokenRecord(fixture.deviceId, Instant.now().minusSeconds(5), null);
        when(fixture.refreshTokenLookupRepository.findByTokenHash("hash")).thenReturn(Optional.of(expired));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> fixture.service.refresh(fixture.deviceId, "rt")
        );

        assertEquals("Refresh token expired.", ex.getMessage());
    }

    @Test
    void refreshRejectsDeviceMismatch() {
        Fixture fixture = new Fixture();
        RefreshTokenRecord record = fixture.tokenRecord(UUID.randomUUID(), Instant.now().plusSeconds(60), null);
        when(fixture.refreshTokenLookupRepository.findByTokenHash("hash")).thenReturn(Optional.of(record));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> fixture.service.refresh(fixture.deviceId, "rt")
        );

        assertEquals("Refresh token device mismatch.", ex.getMessage());
    }

    @Test
    void refreshRejectsMissingDevice() {
        Fixture fixture = new Fixture();
        RefreshTokenRecord record = fixture.tokenRecord(fixture.deviceId, Instant.now().plusSeconds(60), null);
        when(fixture.refreshTokenLookupRepository.findByTokenHash("hash")).thenReturn(Optional.of(record));
        when(fixture.deviceLookupRepository.findById(fixture.deviceId)).thenReturn(Optional.empty());

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> fixture.service.refresh(fixture.deviceId, "rt")
        );

        assertEquals("Device not found.", ex.getMessage());
    }

    @Test
    void refreshRejectsUnverifiedDevice() {
        Fixture fixture = new Fixture();
        RefreshTokenRecord record = fixture.tokenRecord(fixture.deviceId, Instant.now().plusSeconds(60), null);
        when(fixture.refreshTokenLookupRepository.findByTokenHash("hash")).thenReturn(Optional.of(record));
        DeviceRecord device = fixture.deviceRecord(fixture.deviceId, null);
        when(fixture.deviceLookupRepository.findById(fixture.deviceId)).thenReturn(Optional.of(device));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> fixture.service.refresh(fixture.deviceId, "rt")
        );

        assertEquals("Device not verified.", ex.getMessage());
    }

    @Test
    void refreshRotatesTokensForVerifiedDevice() {
        Fixture fixture = new Fixture();
        RefreshTokenRecord record = fixture.tokenRecord(fixture.deviceId, Instant.now().plusSeconds(60), null);
        when(fixture.refreshTokenLookupRepository.findByTokenHash("hash")).thenReturn(Optional.of(record));
        DeviceRecord device = fixture.deviceRecord(fixture.deviceId, Instant.now());
        when(fixture.deviceLookupRepository.findById(fixture.deviceId)).thenReturn(Optional.of(device));
        when(fixture.tokenService.createAccessToken(record.userId(), fixture.deviceId)).thenReturn("access");
        when(fixture.tokenService.createRefreshToken()).thenReturn("refresh");

        AuthTokens tokens = fixture.service.refresh(fixture.deviceId, "rt");

        assertEquals("access", tokens.accessToken().value());
        assertEquals("refresh", tokens.refreshToken().value());
        verify(fixture.refreshTokenRepository, Mockito.times(2)).save(Mockito.any());
    }

    private static final class Fixture {
        private final UserRepository userRepository = Mockito.mock(UserRepository.class);
        private final DeviceLookupRepository deviceLookupRepository = Mockito.mock(DeviceLookupRepository.class);
        private final DeviceRepository deviceRepository = Mockito.mock(DeviceRepository.class);
        private final RefreshTokenLookupRepository refreshTokenLookupRepository = Mockito.mock(RefreshTokenLookupRepository.class);
        private final RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        private final TokenService tokenService = Mockito.mock(TokenService.class);
        private final TokenSettings tokenSettings = Mockito.mock(TokenSettings.class);
        private final SignatureVerifier signatureVerifier = Mockito.mock(SignatureVerifier.class);
        private final RefreshTokenHasher refreshTokenHasher = Mockito.mock(RefreshTokenHasher.class);
        private final ChallengeGenerator challengeGenerator = Mockito.mock(ChallengeGenerator.class);
        private final OtpService otpService = Mockito.mock(OtpService.class);
        private final OtpHasher otpHasher = Mockito.mock(OtpHasher.class);
        private final PhoneNumberValidator phoneNumberValidator = Mockito.mock(PhoneNumberValidator.class);
        private final AuthChallengeStore authChallengeStore = Mockito.mock(AuthChallengeStore.class);
        private final PhoneCrypto phoneCrypto = Mockito.mock(PhoneCrypto.class);
        private final DeviceOtpStore deviceOtpStore = Mockito.mock(DeviceOtpStore.class);

        private final AuthStartValidator authStartValidator = new AuthStartValidator();
        private final VerifyDeviceValidator verifyDeviceValidator = new VerifyDeviceValidator();
        private final RefreshTokenValidator refreshTokenValidator = new RefreshTokenValidator();

        private final UUID deviceId = UUID.randomUUID();
        private final AuthService service;

        private Fixture() {
            when(refreshTokenHasher.hash("rt")).thenReturn("hash");
            when(tokenSettings.accessTtlSeconds()).thenReturn(900L);
            when(tokenSettings.refreshTtlSeconds()).thenReturn(3600L);
            when(tokenSettings.deviceChallengeTtlSeconds()).thenReturn(300L);
            when(tokenSettings.otpTtlSeconds()).thenReturn(300L);

            this.service = new AuthService(
                    userRepository,
                    deviceLookupRepository,
                    deviceRepository,
                    refreshTokenLookupRepository,
                    refreshTokenRepository,
                    authStartValidator,
                    verifyDeviceValidator,
                    refreshTokenValidator,
                    tokenService,
                    tokenSettings,
                    signatureVerifier,
                    refreshTokenHasher,
                    challengeGenerator,
                    otpService,
                    otpHasher,
                    phoneNumberValidator,
                    authChallengeStore,
                    phoneCrypto,
                    deviceOtpStore
            );
        }

        private RefreshTokenRecord tokenRecord(UUID deviceId, Instant expiresAt, Instant revokedAt) {
            return new RefreshTokenRecord(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    deviceId,
                    "hash",
                    expiresAt,
                    revokedAt,
                    Instant.now().minusSeconds(60)
            );
        }

        private DeviceRecord deviceRecord(UUID id, Instant verifiedAt) {
            return new DeviceRecord(
                    id,
                    UUID.randomUUID(),
                    "pk",
                    "EC",
                    null,
                    null,
                    verifiedAt,
                    Instant.now().minusSeconds(60),
                    null
            );
        }
    }
}
