package bo.ahosoft.sqlscript.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QueryLibraryEntry {

    private final String id;
    private final String name;
    private final String sql;
    private final String description;
    private final List<String> tags;
    private final boolean favorite;
    private final String environmentScope;
    private final String connectionScope;
    private final Instant createdAt;
    private final Instant updatedAt;

    public QueryLibraryEntry(
        String id,
        String name,
        String sql,
        String description,
        List<String> tags,
        boolean favorite,
        String environmentScope,
        String connectionScope,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = valueOrEmpty(id);
        this.name = valueOrEmpty(name);
        this.sql = valueOrEmpty(sql);
        this.description = valueOrEmpty(description);
        this.tags = Collections.unmodifiableList(copyTags(tags));
        this.favorite = favorite;
        this.environmentScope = valueOrEmpty(environmentScope);
        this.connectionScope = valueOrEmpty(connectionScope);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String sql() {
        return sql;
    }

    public String description() {
        return description;
    }

    public List<String> tags() {
        return tags;
    }

    public boolean favorite() {
        return favorite;
    }

    public String environmentScope() {
        return environmentScope;
    }

    public String connectionScope() {
        return connectionScope;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public QueryLibraryEntry withFavorite(boolean favorite, Instant updatedAt) {
        return new QueryLibraryEntry(id, name, sql, description, tags, favorite, environmentScope, connectionScope, createdAt, updatedAt);
    }

    private static List<String> copyTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(tags);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
