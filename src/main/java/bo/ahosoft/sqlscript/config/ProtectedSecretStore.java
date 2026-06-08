package bo.ahosoft.sqlscript.config;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ProtectedSecretStore {

    private static final String KEY_FILE_NAME = "secret.key";
    private static final String PREFIX = "enc:v1:";
    private static final int AES_KEY_BYTES = 16;
    private static final int IV_BYTES = 16;

    private final File directory;
    private final SecureRandom random;

    public ProtectedSecretStore(File directory) {
        this(directory, new SecureRandom());
    }

    public ProtectedSecretStore(File directory, SecureRandom random) {
        this.directory = directory;
        this.random = random;
    }

    public String protect(String secret) throws IOException {
        requireSecret(secret);
        try {
            byte[] iv = randomBytes(IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return PREFIX + encode(iv) + ":" + encode(encrypted);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Could not protect secret", ex);
        }
    }

    public String reveal(String protectedValue) throws IOException {
        if (!isProtectedValue(protectedValue)) {
            throw new IllegalArgumentException("Value is not protected");
        }
        try {
            String payload = protectedValue.substring(PREFIX.length());
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid protected value format");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Could not reveal protected secret", ex);
        }
    }

    public boolean isProtectedValue(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public void writeProtectedSecret(File target, String secret) throws IOException {
        String protectedValue = protect(secret);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create secret directory: " + parent.getAbsolutePath());
        }
        FileOutputStream output = new FileOutputStream(target);
        try {
            output.write(protectedValue.getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }
    }

    private SecretKeySpec key() throws IOException {
        File keyFile = new File(directory, KEY_FILE_NAME);
        if (keyFile.isFile()) {
            byte[] existing = Files.readAllBytes(keyFile.toPath());
            return new SecretKeySpec(Arrays.copyOf(existing, AES_KEY_BYTES), "AES");
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create secret directory: " + directory.getAbsolutePath());
        }
        byte[] generated = randomBytes(AES_KEY_BYTES);
        FileOutputStream output = new FileOutputStream(keyFile);
        try {
            output.write(generated);
        } finally {
            output.close();
        }
        keyFile.setReadable(false, false);
        keyFile.setWritable(false, false);
        keyFile.setReadable(true, true);
        keyFile.setWritable(true, true);
        return new SecretKeySpec(generated, "AES");
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        return bytes;
    }

    private static void requireSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret is required");
        }
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }
}
