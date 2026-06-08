package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CompositeCompletionProvider implements CompletionProvider {

    private final List<CompletionProvider> providers;

    public CompositeCompletionProvider(List<CompletionProvider> providers) {
        this.providers = providers == null
            ? Collections.<CompletionProvider>emptyList()
            : Collections.unmodifiableList(new ArrayList<CompletionProvider>(providers));
    }

    @Override
    public List<CompletionCandidate> suggest(String text, int cursorOffset, MetadataCatalogSnapshot catalog) {
        List<CompletionCandidate> candidates = new ArrayList<CompletionCandidate>();
        for (CompletionProvider provider : providers) {
            candidates.addAll(provider.suggest(text, cursorOffset, catalog));
        }
        Collections.sort(
            candidates,
            new Comparator<CompletionCandidate>() {
                @Override
                public int compare(CompletionCandidate left, CompletionCandidate right) {
                    int byValue = left.value().compareToIgnoreCase(right.value());
                    if (byValue != 0) {
                        return byValue;
                    }
                    return left.kind().compareToIgnoreCase(right.kind());
                }
            }
        );
        return Collections.unmodifiableList(candidates);
    }
}
