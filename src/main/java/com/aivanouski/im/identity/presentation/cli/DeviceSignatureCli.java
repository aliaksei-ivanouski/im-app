package com.aivanouski.im.identity.presentation.cli;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;

public final class DeviceSignatureCli {
    private DeviceSignatureCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("  DeviceSignatureCli --generate [--alg EC|RSA]");
            System.err.println("  DeviceSignatureCli --sign <challenge> --private-key <base64> [--alg EC|RSA]");
            System.exit(2);
        }

        if ("--generate".equals(args[0])) {
            String keyAlg = parseAlg(args);
            KeyPair keyPair = generateKeyPair(keyAlg);
            printKeyMaterial(keyAlg, keyPair);
            return;
        }

        if ("--sign".equals(args[0])) {
            String challenge = args[1];
            String privateKeyBase64 = readArg(args, "--private-key");
            String keyAlg = parseAlg(args);
            String signature = signChallenge(keyAlg, privateKeyBase64, challenge);
            System.out.println("challenge: " + challenge);
            System.out.println("signature: " + signature);
            return;
        }

        System.err.println("Unknown command.");
        System.exit(2);
    }

    private static String parseAlg(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--alg".equals(args[i])) {
                return args[i + 1];
            }
        }
        return "EC";
    }

    private static KeyPair generateKeyPair(String keyAlg) throws Exception {
        if ("RSA".equalsIgnoreCase(keyAlg)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String signChallenge(String keyAlg, String privateKeyBase64, String challenge) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        var keySpec = new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
        var keyFactory = java.security.KeyFactory.getInstance(keyAlg);
        var privateKey = keyFactory.generatePrivate(keySpec);
        String signatureAlg = "RSA".equalsIgnoreCase(keyAlg) ? "SHA256withRSA" : "SHA256withECDSA";
        Signature signature = Signature.getInstance(signatureAlg);
        signature.initSign(privateKey);
        signature.update(challenge.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static String readArg(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        throw new IllegalArgumentException("Missing " + name);
    }

    private static void printKeyMaterial(String keyAlg, KeyPair keyPair) {
        Base64.Encoder base64 = Base64.getEncoder();
        String publicKey = base64.encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = base64.encodeToString(keyPair.getPrivate().getEncoded());

        System.out.println("deviceId: " + UUID.randomUUID());
        System.out.println("publicKeyAlg: " + keyAlg);
        System.out.println("devicePublicKey: " + publicKey);
        System.out.println("privateKeyPkcs8: " + privateKey);
    }
}
