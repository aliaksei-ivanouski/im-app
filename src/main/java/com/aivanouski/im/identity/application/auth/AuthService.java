package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.shared.exception.ValidationException;
import com.aivanouski.im.identity.domain.auth.AuthTokens;
import com.aivanouski.im.identity.domain.auth.AccessToken;
import com.aivanouski.im.identity.domain.auth.RefreshToken;
import com.aivanouski.im.identity.domain.DeviceRecord;
import com.aivanouski.im.identity.domain.RefreshTokenRecord;
import com.aivanouski.im.identity.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final DeviceLookupRepository deviceLookupRepository;
    private final DeviceRepository deviceRepository;
    private final RefreshTokenLookupRepository refreshTokenLookupRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthStartValidator authStartValidator;
    private final VerifyDeviceValidator verifyDeviceValidator;
    private final RefreshTokenValidator refreshTokenValidator;
    private final TokenService tokenService;
    private final TokenSettings tokenSettings;
    private final SignatureVerifier signatureVerifier;
    private final RefreshTokenHasher refreshTokenHasher;
    private final ChallengeGenerator challengeGenerator;
    private final OtpService otpService;
    private final OtpHasher otpHasher;
    private final PhoneNumberValidator phoneNumberValidator;
    private final AuthChallengeStore authChallengeStore;
    private final PhoneCrypto phoneCrypto;
    private final DeviceOtpStore deviceOtpStore;

    public AuthService(
            UserRepository userRepository,
            DeviceLookupRepository deviceLookupRepository,
            DeviceRepository deviceRepository,
            RefreshTokenLookupRepository refreshTokenLookupRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthStartValidator authStartValidator,
            VerifyDeviceValidator verifyDeviceValidator,
            RefreshTokenValidator refreshTokenValidator,
            TokenService tokenService,
            TokenSettings tokenSettings,
            SignatureVerifier signatureVerifier,
            RefreshTokenHasher refreshTokenHasher,
            ChallengeGenerator challengeGenerator,
            OtpService otpService,
            OtpHasher otpHasher,
            PhoneNumberValidator phoneNumberValidator,
            AuthChallengeStore authChallengeStore,
            PhoneCrypto phoneCrypto,
            DeviceOtpStore deviceOtpStore
    ) {
        this.userRepository = userRepository;
        this.deviceLookupRepository = deviceLookupRepository;
        this.deviceRepository = deviceRepository;
        this.refreshTokenLookupRepository = refreshTokenLookupRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authStartValidator = authStartValidator;
        this.verifyDeviceValidator = verifyDeviceValidator;
        this.refreshTokenValidator = refreshTokenValidator;
        this.tokenService = tokenService;
        this.tokenSettings = tokenSettings;
        this.signatureVerifier = signatureVerifier;
        this.refreshTokenHasher = refreshTokenHasher;
        this.challengeGenerator = challengeGenerator;
        this.otpService = otpService;
        this.otpHasher = otpHasher;
        this.phoneNumberValidator = phoneNumberValidator;
        this.authChallengeStore = authChallengeStore;
        this.phoneCrypto = phoneCrypto;
        this.deviceOtpStore = deviceOtpStore;
    }

    @Transactional
    public AuthStartResult startAuth(String phone, UUID deviceId) {
        authStartValidator.validate(phone, deviceId);
        String normalizedPhone = phoneNumberValidator.normalize(phone);
        String phoneHash = phoneCrypto.hash(normalizedPhone);
        UserAccount user = userRepository.findByPhoneHash(phoneHash)
                .orElseGet(() -> userRepository.save(new UserAccount(
                        UUID.randomUUID(),
                        phoneHash,
                        phoneCrypto.encrypt(normalizedPhone),
                        Instant.now()
                )));
        DeviceRecord device = deviceLookupRepository.findById(deviceId)
                .map(existing -> {
                    if (!existing.userId().equals(user.id())) {
                        throw new ValidationException("Device belongs to another user.");
                    }
                    return existing;
                })
                .orElseGet(() -> new DeviceRecord(
                        deviceId,
                        user.id(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        Instant.now(),
                        null
                ));

        String challenge = challengeGenerator.generate();
        Instant expiresAt = Instant.now().plusSeconds(tokenSettings.deviceChallengeTtlSeconds());
        authChallengeStore.save(normalizedPhone, challenge, expiresAt);
        OtpCode otp = otpService.sendOtp(normalizedPhone);
        String otpHash = otpHasher.hash(otp.code());
        DeviceRecord updatedDevice = new DeviceRecord(
                device.id(),
                device.userId(),
                device.publicKey(),
                device.publicKeyAlg(),
                challenge,
                expiresAt,
                device.verifiedAt(),
                device.createdAt(),
                device.lastSeenAt()
        );
        deviceRepository.save(updatedDevice);
        deviceOtpStore.save(device.id(), new DeviceOtp(otpHash, otp.expiresAt()));
        return AuthStartResult.otpRequired(challenge, expiresAt, otp.expiresAt());
    }

    @Transactional
    public AuthTokens verifyDevice(String phone, UUID deviceId, String otp, String publicKey, String publicKeyAlg, String signature) {
        verifyDeviceValidator.validate(phone, deviceId, otp, publicKey, publicKeyAlg, signature);
        String normalizedPhone = phoneNumberValidator.normalize(phone);
        String phoneHash = phoneCrypto.hash(normalizedPhone);
        UserAccount user = userRepository.findByPhoneHash(phoneHash)
                .orElseGet(() -> userRepository.save(new UserAccount(
                        UUID.randomUUID(),
                        phoneHash,
                        phoneCrypto.encrypt(normalizedPhone),
                        Instant.now()
                )));
        AuthChallenge challenge = authChallengeStore.findByPhone(normalizedPhone)
                .orElseThrow(() -> new ValidationException("Challenge expired."));
        DeviceRecord existingDevice = deviceLookupRepository.findByIdAndUserId(deviceId, user.id())
                .orElseThrow(() -> new ValidationException("Device not found."));
        DeviceOtp deviceOtp = deviceOtpStore.findByDeviceId(deviceId)
                .orElseThrow(() -> new ValidationException("OTP expired."));
        if (Instant.now().isAfter(deviceOtp.expiresAt())) {
            deviceOtpStore.remove(deviceId);
            throw new ValidationException("OTP expired.");
        }
        if (!otpHasher.matches(otp, deviceOtp.codeHash())) {
            throw new ValidationException("Invalid OTP.");
        }
        if (signature == null || signature.isBlank()) {
            throw new ValidationException("signature is required.");
        }
        if (!signatureVerifier.verify(publicKeyAlg, publicKey, challenge.challenge(), signature)) {
            throw new ValidationException("Invalid device signature.");
        }
        Instant verifiedAt = existingDevice.verifiedAt() == null ? Instant.now() : existingDevice.verifiedAt();
        DeviceRecord updated = new DeviceRecord(
                existingDevice.id(),
                existingDevice.userId(),
                publicKey,
                publicKeyAlg,
                null,
                null,
                verifiedAt,
                existingDevice.createdAt(),
                existingDevice.lastSeenAt()
        );
        deviceRepository.save(updated);
        deviceOtpStore.remove(deviceId);
        authChallengeStore.remove(normalizedPhone);
        return issueTokens(user.id(), updated.id());
    }

    @Transactional
    public AuthTokens refresh(UUID deviceId, String refreshToken) {
        refreshTokenValidator.validate(deviceId, refreshToken);
        String hash = refreshTokenHasher.hash(refreshToken);
        RefreshTokenRecord existing = refreshTokenLookupRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ValidationException("Invalid refresh token."));
        if (existing.revokedAt() != null || Instant.now().isAfter(existing.expiresAt())) {
            throw new ValidationException("Refresh token expired.");
        }
        if (!existing.deviceId().equals(deviceId)) {
            throw new ValidationException("Refresh token device mismatch.");
        }
        DeviceRecord device = deviceLookupRepository.findById(deviceId)
                .orElseThrow(() -> new ValidationException("Device not found."));
        if (device.verifiedAt() == null) {
            throw new ValidationException("Device not verified.");
        }
        RefreshTokenRecord revoked = new RefreshTokenRecord(
                existing.id(),
                existing.userId(),
                existing.deviceId(),
                existing.tokenHash(),
                existing.expiresAt(),
                Instant.now(),
                existing.createdAt()
        );
        refreshTokenRepository.save(revoked);
        return issueTokens(existing.userId(), deviceId);
    }

    private AuthTokens issueTokens(UUID userId, UUID deviceId) {
        String access = tokenService.createAccessToken(userId, deviceId);
        String refresh = tokenService.createRefreshToken();
        Instant refreshExpires = Instant.now().plusSeconds(tokenSettings.refreshTtlSeconds());
        RefreshTokenRecord refreshEntity = new RefreshTokenRecord(
                UUID.randomUUID(),
                userId,
                deviceId,
                refreshTokenHasher.hash(refresh),
                refreshExpires,
                null,
                Instant.now()
        );
        refreshTokenRepository.save(refreshEntity);
        return new AuthTokens(
                new AccessToken(access, tokenSettings.accessTtlSeconds()),
                new RefreshToken(refresh, tokenSettings.refreshTtlSeconds())
        );
    }

}
