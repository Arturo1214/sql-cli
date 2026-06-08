package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CatalogCompletionProvider implements CompletionProvider {

    @Override
    public List<CompletionCandidate> suggest(String text, int cursorOffset, MetadataCatalogSnapshot catalog) {
        CompletionContext context = CompletionContext.from(text, cursorOffset);
        if (!context.atTableReferencePosition()) {
            return Collections.emptyList();
        }
        MetadataCatalogSnapshot safeCatalog = catalog == null ? MetadataCatalogSnapshot.empty() : catalog;
        List<CompletionCandidate> candidates = new ArrayList<CompletionCandidate>();
        for (String tableName : safeCatalog.matchingTables(context.prefix())) {
            candidates.add(new CompletionCandidate(tableName, tableName, "table", context.replacementStart(), context.replacementEnd()));
        }
        return Collections.unmodifiableList(candidates);
    }
}
