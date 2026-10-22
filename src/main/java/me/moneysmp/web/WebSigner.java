package me.moneysmp.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Signs every leaderboard update so the website can tell real updates from fake ones.
 *
 * The ntfy.sh topic is public, so anyone could post to it. Only this server holds the
 * private key, and the website only accepts messages that verify against the matching
 * public key. Uses ECDSA P-256 / SHA-256 in the raw (r||s) format WebCrypto expects.
 *
 * Key file: plugins/WillyMoneySMP/web-signing-key.properties (keep it private!).
 */
public class WebSigner {
    public static final String KEY_FILE_NAME = "web-signing-key.properties";

    private final PrivateKey privateKey;
    private final String publicKeyBase64;
    private final boolean generatedNewKey;

    private WebSigner(PrivateKey privateKey, String publicKeyBase64, boolean generatedNewKey) {
        this.privateKey = privateKey;
        this.publicKeyBase64 = publicKeyBase64;
        this.generatedNewKey = generatedNewKey;
    }

    /** Loads the key pair from the data folder, generating and saving a new one if missing. */
    public static WebSigner loadOrCreate(File dataFolder, Logger logger) throws Exception {
        File keyFile = new File(dataFolder, KEY_FILE_NAME);
        if (keyFile.exists()) {
            Properties props = new Properties();
            try (InputStream in = new FileInputStream(keyFile)) {
                props.load(in);
            }
            String priv = props.getProperty("private-key", "").trim();
            String pub = props.getProperty("public-key", "").trim();
            if (!priv.isEmpty() && !pub.isEmpty()) {
                KeyFactory kf = KeyFactory.getInstance("EC");
                PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(priv)));
                // Validate the public key parses too.
                kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pub)));
                return new WebSigner(privateKey, pub, false);
            }
            logger.warning("[WebSync] " + KEY_FILE_NAME + " is missing a key, generating a new one.");
        }

        KeyPair pair = generateKeyPair();
        String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String priv = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        save(keyFile, priv, pub);
        return new WebSigner(pair.getPrivate(), pub, true);
    }

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        return gen.generateKeyPair();
    }

    public static void save(File keyFile, String privateKeyBase64, String publicKeyBase64) throws IOException {
        File parent = keyFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        Properties props = new Properties();
        props.setProperty("private-key", privateKeyBase64);
        props.setProperty("public-key", publicKeyBase64);
        try (OutputStream out = new FileOutputStream(keyFile)) {
            props.store(out, "Willy Money SMP website signing key. KEEP THIS FILE PRIVATE - never share private-key.\n"
                    + "The website must contain the matching public-key (src/config.ts in the website project).");
        }
    }

    /** Signs the UTF-8 payload and returns base64url (no padding) of the 64-byte r||s signature. */
    public String sign(String payload) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSAinP1363Format");
        sig.initSign(privateKey);
        sig.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
    }

    public String getPublicKeyBase64() { return publicKeyBase64; }
    public boolean isNewlyGenerated() { return generatedNewKey; }

    /** Helper to create a key file offline: java -cp MoneySMP.jar me.moneysmp.web.WebSigner <outputFile> */
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : KEY_FILE_NAME);
        KeyPair pair = generateKeyPair();
        String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        save(out, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()), pub);
        System.out.println("Wrote " + out.getAbsolutePath());
        System.out.println("Public key (put this in the website's src/config.ts):");
        System.out.println(pub);
    }
}
