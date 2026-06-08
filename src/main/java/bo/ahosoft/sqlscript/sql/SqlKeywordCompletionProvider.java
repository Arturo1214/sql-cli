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

public final class SqlKeywordCompletionProvider implements CompletionProvider {

    private static final String[] KEYWORDS = new String[] {
        "SELECT",
        "FROM",
        "WHERE",
        "JOIN",
        "LEFT",
        "RIGHT",
        "INNER",
        "OUTER",
        "GROUP",
        "ORDER",
        "BY",
        "HAVING",
        "INSERT",
        "UPDATE",
        "DELETE",
        "CREATE",
        "ALTER",
        "DROP",
        "TABLE",
        "VIEW",
        "INDEX",
        "VALUES",
        "INTO",
    };

    @Override
    public List<CompletionCandidate> suggest(String text, int cursorOffset, MetadataCatalogSnapshot catalog) {
        CompletionContext context = CompletionContext.from(text, cursorOffset);
        if (context.prefix().isEmpty() || context.afterSelectKeyword() || context.atTableReferencePosition()) {
            return Collections.emptyList();
        }
        List<CompletionCandidate> candidates = new ArrayList<CompletionCandidate>();
        String prefix = context.prefix().toUpperCase();
        for (String keyword : KEYWORDS) {
            if (keyword.startsWith(prefix)) {
                candidates.add(new CompletionCandidate(keyword, keyword, "keyword", context.replacementStart(), context.replacementEnd()));
            }
        }
        return Collections.unmodifiableList(candidates);
    }
}
