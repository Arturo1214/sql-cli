package bo.ahosoft.sqlscript.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class CompletionProviderTest {

    @Test
    public void suggestsSqlKeywordsForCurrentPrefix() {
        CompletionProvider provider = new SqlKeywordCompletionProvider();

        List<CompletionCandidate> candidates = provider.suggest("sel", 3, MetadataCatalogSnapshot.empty());

        assertEquals(1, candidates.size());
        assertCandidate(candidates.get(0), "SELECT", "keyword", 0, 3);
    }

    @Test
    public void suggestsTableNamesAtTableReferencePosition() {
        MetadataCatalogSnapshot catalog = MetadataCatalogSnapshot.ofTables(Arrays.asList("orders", "users"));
        CompletionProvider provider = new CatalogCompletionProvider();

        List<CompletionCandidate> candidates = provider.suggest("select * from us", 16, catalog);

        assertEquals(1, candidates.size());
        assertCandidate(candidates.get(0), "users", "table", 14, 16);
    }

    @Test
    public void combinesProvidersWithDeterministicOrdering() {
        MetadataCatalogSnapshot catalog = MetadataCatalogSnapshot.ofTables(Arrays.asList("users", "user_audit"));
        CompletionProvider provider = new CompositeCompletionProvider(
            Arrays.<CompletionProvider>asList(new CatalogCompletionProvider(), new SqlKeywordCompletionProvider())
        );

        List<CompletionCandidate> candidates = provider.suggest("select * from u", 15, catalog);

        assertEquals(Arrays.asList("user_audit", "users"), values(candidates));
    }

    @Test
    public void doesNotSuggestColumnsOrFunctionsAndDoesNotRewriteSql() {
        MetadataCatalogSnapshot catalog = MetadataCatalogSnapshot.ofTables(Collections.singletonList("users"));
        CompletionProvider provider = new CompositeCompletionProvider(
            Arrays.<CompletionProvider>asList(new SqlKeywordCompletionProvider(), new CatalogCompletionProvider())
        );
        String sql = "select us from users";

        List<CompletionCandidate> candidates = provider.suggest(sql, 9, catalog);

        assertTrue(candidates.isEmpty());
        assertEquals("select us from users", sql);
    }

    private static void assertCandidate(
        CompletionCandidate candidate,
        String value,
        String kind,
        int replacementStart,
        int replacementEnd
    ) {
        assertEquals(value, candidate.value());
        assertEquals(value, candidate.label());
        assertEquals(kind, candidate.kind());
        assertEquals(replacementStart, candidate.replacementStart());
        assertEquals(replacementEnd, candidate.replacementEnd());
    }

    private static List<String> values(List<CompletionCandidate> candidates) {
        List<String> values = new java.util.ArrayList<String>();
        for (CompletionCandidate candidate : candidates) {
            values.add(candidate.value());
        }
        return values;
    }
}
