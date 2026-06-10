package bo.ahosoft.sqlscript.config;

import bo.ahosoft.sqlscript.domain.QueryLibraryEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class QueryLibraryStore {

    public static final String PRIVACY_WARNING =
        "Saved SQL may contain sensitive data. Review query text before storing shared or regulated information.";
    public static final String RAW_SUBSTITUTION_WARNING =
        "Raw substitution warning: template values are inserted as text. Quote and escape values in the template or input before running.";
    public static final Set<PosixFilePermission> USER_ONLY_DIRECTORY_PERMISSIONS = Collections.unmodifiableSet(
        new HashSet<PosixFilePermission>(
            Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
        )
    );
    public static final Set<PosixFilePermission> USER_ONLY_FILE_PERMISSIONS = Collections.unmodifiableSet(
        new HashSet<PosixFilePermission>(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
    );

    private final File file;
    private final Clock clock;

    public QueryLibraryStore() {
        this(defaultLibraryFile(), Clock.systemUTC());
    }

    public QueryLibraryStore(File file, Clock clock) {
        this.file = file;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public static File defaultLibraryFile() {
        return new File(new File(System.getProperty("user.home"), ".oracle-script-cli"), "query-library.properties");
    }

    public QueryLibraryEntry save(String name, String sql, String description, List<String> tags, boolean overwrite) throws IOException {
        return save(name, sql, description, tags, false, "", "", overwrite);
    }

    public QueryLibraryEntry save(
        String name,
        String sql,
        String description,
        List<String> tags,
        boolean favorite,
        String environmentScope,
        String connectionScope,
        boolean overwrite
    ) throws IOException {
        return saveEntry(
            name,
            sql,
            description,
            tags,
            favorite,
            environmentScope,
            connectionScope,
            false,
            Collections.<String>emptyList(),
            overwrite
        );
    }

    public QueryLibraryEntry saveTemplate(
        String name,
        String sql,
        String description,
        List<String> tags,
        boolean favorite,
        String environmentScope,
        String connectionScope,
        List<String> templateParameters,
        boolean overwrite
    ) throws IOException {
        return saveEntry(name, sql, description, tags, favorite, environmentScope, connectionScope, true, templateParameters, overwrite);
    }

    private QueryLibraryEntry saveEntry(
        String name,
        String sql,
        String description,
        List<String> tags,
        boolean favorite,
        String environmentScope,
        String connectionScope,
        boolean template,
        List<String> templateParameters,
        boolean overwrite
    ) throws IOException {
        String normalizedName = requireText(name, "name");
        String id = slug(normalizedName);
        Properties properties = readProperties();
        List<String> ids = readIds(properties);
        QueryLibraryEntry existing = readEntry(properties, id);
        if (existing != null && !overwrite) {
            throw new IllegalArgumentException("Query already exists: " + id);
        }
        if (!ids.contains(id)) {
            ids.add(id);
        }
        Instant now = Instant.now(clock);
        Instant createdAt = existing == null ? now : existing.createdAt();
        QueryLibraryEntry entry = new QueryLibraryEntry(
            id,
            normalizedName,
            sql,
            description,
            normalizeTags(tags),
            favorite,
            environmentScope,
            connectionScope,
            createdAt,
            now,
            template,
            normalizeTags(templateParameters),
            template ? now : null
        );
        writeEntry(properties, entry);
        writeIds(properties, ids);
        writeProperties(properties);
        return entry;
    }

    public List<QueryLibraryEntry> list() throws IOException {
        Properties properties = readProperties();
        List<QueryLibraryEntry> entries = new ArrayList<>();
        for (String id : readIds(properties)) {
            QueryLibraryEntry entry = readEntry(properties, id);
            if (entry != null) {
                entries.add(entry);
            }
        }
        Collections.sort(
            entries,
            new Comparator<QueryLibraryEntry>() {
                public int compare(QueryLibraryEntry left, QueryLibraryEntry right) {
                    return left.name().compareToIgnoreCase(right.name());
                }
            }
        );
        return entries;
    }

    public List<QueryLibraryEntry> search(String text) throws IOException {
        String needle = normalizeSearch(text);
        if (needle.isEmpty()) {
            return list();
        }
        List<QueryLibraryEntry> matches = new ArrayList<>();
        for (QueryLibraryEntry entry : list()) {
            if (matchesMetadata(entry, needle)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    public QueryLibraryEntry load(String idOrSlug) throws IOException {
        String id = slug(idOrSlug);
        QueryLibraryEntry entry = readEntry(readProperties(), id);
        if (entry == null) {
            throw new IllegalArgumentException("Query not found: " + id);
        }
        return entry;
    }

    public boolean delete(String idOrSlug) throws IOException {
        String id = slug(idOrSlug);
        Properties properties = readProperties();
        List<String> ids = readIds(properties);
        if (!ids.remove(id)) {
            return false;
        }
        removeEntry(properties, id);
        writeIds(properties, ids);
        writeProperties(properties);
        return true;
    }

    public QueryLibraryEntry setFavorite(String idOrSlug, boolean favorite) throws IOException {
        QueryLibraryEntry current = load(idOrSlug);
        QueryLibraryEntry updated = current.withFavorite(favorite, Instant.now(clock));
        Properties properties = readProperties();
        writeEntry(properties, updated);
        writeProperties(properties);
        return updated;
    }

    public static String slug(String value) {
        String text = requireText(value, "id").toLowerCase(Locale.ROOT).trim();
        text = text.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "").replaceAll("-+", "-");
        if (text.isEmpty()) {
            throw new IllegalArgumentException("id must contain letters or numbers");
        }
        return text;
    }

    public static String encodeText(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private Properties readProperties() throws IOException {
        Properties properties = new Properties();
        if (!file.isFile() || file.length() == 0) {
            return properties;
        }
        FileInputStream input = new FileInputStream(file);
        try {
            properties.load(input);
        } finally {
            input.close();
        }
        return properties;
    }

    private void writeProperties(Properties properties) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create query library directory: " + parent.getAbsolutePath());
        }
        applyDirectoryPermissions(parent);
        FileOutputStream output = new FileOutputStream(file);
        try {
            properties.store(output, "Oracle Script CLI query library");
        } finally {
            output.close();
        }
        applyFilePermissions(file);
    }

    private static List<String> readIds(Properties properties) {
        String entries = properties.getProperty("entries", "");
        List<String> ids = new ArrayList<>();
        for (String entry : entries.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                ids.add(trimmed);
            }
        }
        return ids;
    }

    private static void writeIds(Properties properties, List<String> ids) {
        properties.setProperty("entries", join(ids));
    }

    private static QueryLibraryEntry readEntry(Properties properties, String id) {
        String prefix = "entry." + id + ".";
        if (!properties.containsKey(prefix + "name") || !properties.containsKey(prefix + "sql")) {
            return null;
        }
        try {
            int tagCount = parseInt(properties.getProperty(prefix + "tags.count"));
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < tagCount; i++) {
                tags.add(decodeText(properties.getProperty(prefix + "tags." + i, "")));
            }
            Instant createdAt = parseInstant(properties.getProperty(prefix + "createdAt"));
            Instant updatedAt = parseInstant(properties.getProperty(prefix + "updatedAt"));
            int parameterCount = parseInt(properties.getProperty(prefix + "templateParameters.count"));
            List<String> templateParameters = new ArrayList<>();
            for (int i = 0; i < parameterCount; i++) {
                templateParameters.add(decodeText(properties.getProperty(prefix + "templateParameters." + i, "")));
            }
            boolean template = Boolean.parseBoolean(properties.getProperty(prefix + "template", "false"));
            Instant templateUpdatedAt = template ? parseInstant(properties.getProperty(prefix + "templateUpdatedAt")) : null;
            return new QueryLibraryEntry(
                id,
                decodeText(properties.getProperty(prefix + "name")),
                decodeText(properties.getProperty(prefix + "sql")),
                decodeText(properties.getProperty(prefix + "description", "")),
                tags,
                Boolean.parseBoolean(properties.getProperty(prefix + "favorite", "false")),
                decodeText(properties.getProperty(prefix + "environmentScope", "")),
                decodeText(properties.getProperty(prefix + "connectionScope", "")),
                createdAt,
                updatedAt,
                template,
                templateParameters,
                templateUpdatedAt
            );
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static void writeEntry(Properties properties, QueryLibraryEntry entry) {
        String prefix = "entry." + entry.id() + ".";
        properties.setProperty(prefix + "name", encodeText(entry.name()));
        properties.setProperty(prefix + "sql", encodeText(entry.sql()));
        properties.setProperty(prefix + "description", encodeText(entry.description()));
        properties.setProperty(prefix + "tags.count", String.valueOf(entry.tags().size()));
        for (int i = 0; i < entry.tags().size(); i++) {
            properties.setProperty(prefix + "tags." + i, encodeText(entry.tags().get(i)));
        }
        properties.setProperty(prefix + "favorite", String.valueOf(entry.favorite()));
        properties.setProperty(prefix + "environmentScope", encodeText(entry.environmentScope()));
        properties.setProperty(prefix + "connectionScope", encodeText(entry.connectionScope()));
        properties.setProperty(prefix + "createdAt", entry.createdAt().toString());
        properties.setProperty(prefix + "updatedAt", entry.updatedAt().toString());
        properties.setProperty(prefix + "template", String.valueOf(entry.template()));
        properties.setProperty(prefix + "templateParameters.count", String.valueOf(entry.templateParameters().size()));
        for (int i = 0; i < entry.templateParameters().size(); i++) {
            properties.setProperty(prefix + "templateParameters." + i, encodeText(entry.templateParameters().get(i)));
        }
        if (entry.templateUpdatedAt() != null) {
            properties.setProperty(prefix + "templateUpdatedAt", entry.templateUpdatedAt().toString());
        } else {
            properties.remove(prefix + "templateUpdatedAt");
        }
    }

    private static void removeEntry(Properties properties, String id) {
        String prefix = "entry." + id + ".";
        List<String> keys = new ArrayList<>();
        for (Object key : properties.keySet()) {
            String text = String.valueOf(key);
            if (text.startsWith(prefix)) {
                keys.add(text);
            }
        }
        for (String key : keys) {
            properties.remove(key);
        }
    }

    private static boolean matchesMetadata(QueryLibraryEntry entry, String needle) {
        if (normalizeSearch(entry.name()).contains(needle) || normalizeSearch(entry.description()).contains(needle)) {
            return true;
        }
        for (String tag : entry.tags()) {
            if (normalizeSearch(tag).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag != null && !tag.trim().isEmpty()) {
                normalized.add(tag.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String decodeText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Instant.EPOCH;
        }
        return Instant.parse(value);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String normalizeSearch(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return value.trim();
    }

    private static void applyDirectoryPermissions(File directory) {
        if (directory == null) {
            return;
        }
        try {
            Files.setPosixFilePermissions(directory.toPath(), USER_ONLY_DIRECTORY_PERMISSIONS);
        } catch (UnsupportedOperationException ignored) {} catch (IOException ignored) {}
    }

    private static void applyFilePermissions(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), USER_ONLY_FILE_PERMISSIONS);
        } catch (UnsupportedOperationException ignored) {} catch (IOException ignored) {}
    }
}
