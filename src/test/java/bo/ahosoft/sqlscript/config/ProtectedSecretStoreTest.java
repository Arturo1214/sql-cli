package bo.ahosoft.sqlscript.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProtectedSecretStoreTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void encryptsAndDecryptsSecretWithoutPlainText() throws Exception {
        ProtectedSecretStore store = new ProtectedSecretStore(temporaryFolder.newFolder("secrets"));

        String protectedValue = store.protect("qa-password-123");

        assertFalse(protectedValue.contains("qa-password-123"));
        assertTrue(store.isProtectedValue(protectedValue));
        assertEquals("qa-password-123", store.reveal(protectedValue));
    }

    @Test
    public void usesStableLocalKeyForRoundTripAcrossInstances() throws Exception {
        File directory = temporaryFolder.newFolder("stable-secrets");
        ProtectedSecretStore firstStore = new ProtectedSecretStore(directory);
        String protectedValue = firstStore.protect("another-secret");

        ProtectedSecretStore secondStore = new ProtectedSecretStore(directory);

        assertEquals("another-secret", secondStore.reveal(protectedValue));
    }

    @Test
    public void failsStorageWriteWithoutExposingSecret() throws Exception {
        ProtectedSecretStore store = new ProtectedSecretStore(temporaryFolder.newFolder("write-failure"));
        File parentThatIsAFile = temporaryFolder.newFile("not-a-directory");
        File target = new File(parentThatIsAFile, "secret.txt");

        try {
            store.writeProtectedSecret(target, "never-print-me");
        } catch (Exception ex) {
            assertFalse(ex.getMessage().contains("never-print-me"));
            assertFalse(target.exists());
            return;
        }

        throw new AssertionError("Expected write failure");
    }

    @Test
    public void writesOnlyProtectedValueToStorage() throws Exception {
        ProtectedSecretStore store = new ProtectedSecretStore(temporaryFolder.newFolder("write-secret"));
        File target = temporaryFolder.newFile("stored-secret.txt");

        store.writeProtectedSecret(target, "stored-password");

        String stored = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        assertFalse(stored.contains("stored-password"));
        assertEquals("stored-password", store.reveal(stored.trim()));
    }
}
