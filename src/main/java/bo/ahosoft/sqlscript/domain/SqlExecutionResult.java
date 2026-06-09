package bo.ahosoft.sqlscript.domain;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SqlExecutionResult {

    static final int DEFAULT_PAGE_SIZE = 100;
    public static final long PAGE_CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private final List<String> pages;
    private final int pageIndex;
    private final int pageSize;
    private final PageSource pageSource;
    private final java.util.Map<Integer, CachedPage> pageCache;
    private final Long totalRows;
    private final String heading;
    private final List<String> headers;
    private final List<List<String>> rows;

    public SqlExecutionResult(String consoleTable) {
        this(Collections.singletonList(consoleTable == null ? "" : consoleTable), 0, DEFAULT_PAGE_SIZE, null, null, null, null, null, null);
    }

    private SqlExecutionResult(List<String> pages, int pageIndex, int pageSize) {
        this(pages, pageIndex, pageSize, null, null, null, null, null, null);
    }

    private SqlExecutionResult(
        PageSource pageSource,
        java.util.Map<Integer, CachedPage> pageCache,
        Long totalRows,
        int pageIndex,
        int pageSize
    ) {
        this(null, pageIndex, pageSize, pageSource, pageCache, totalRows, null, null, null);
    }

    private SqlExecutionResult(
        List<String> pages,
        int pageIndex,
        int pageSize,
        PageSource pageSource,
        java.util.Map<Integer, CachedPage> pageCache,
        Long totalRows,
        String heading,
        List<String> headers,
        List<List<String>> rows
    ) {
        List<String> safePages = pages == null || pages.isEmpty() ? Collections.singletonList("") : pages;
        this.pages = Collections.unmodifiableList(new ArrayList<String>(safePages));
        this.pageIndex = pageSource == null ? Math.max(0, Math.min(pageIndex, this.pages.size() - 1)) : Math.max(0, pageIndex);
        this.pageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        this.pageSource = pageSource;
        this.pageCache = pageCache;
        this.totalRows = totalRows;
        this.heading = heading;
        this.headers = immutableStrings(headers);
        this.rows = immutableRows(rows);
    }

    public static SqlExecutionResult paged(String heading, List<String> headers, List<List<String>> rows) {
        List<List<String>> safeRows = rows == null ? Collections.<List<String>>emptyList() : rows;
        List<String> pages = new ArrayList<String>();
        int totalRows = safeRows.size();
        int pageCount = Math.max(1, (totalRows + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE);
        for (int page = 0; page < pageCount; page++) {
            int from = page * DEFAULT_PAGE_SIZE;
            int to = Math.min(from + DEFAULT_PAGE_SIZE, totalRows);
            List<List<String>> pageRows = safeRows.subList(from, to);
            StringBuilder rendered = new StringBuilder();
            if (heading != null && !heading.trim().isEmpty()) {
                rendered.append(heading).append(System.lineSeparator());
            }
            rendered.append(pageStatus(page, pageCount, from, to, totalRows)).append(System.lineSeparator());
            rendered.append(SqlConsoleRenderer.formatRows(headers, pageRows));
            pages.add(rendered.toString());
        }
        return new SqlExecutionResult(pages, 0, DEFAULT_PAGE_SIZE, null, null, null, heading, headers, safeRows);
    }

    public static SqlExecutionResult databasePaged(PageSource pageSource, Long totalRows) {
        SqlExecutionResult result = new SqlExecutionResult(
            pageSource,
            new java.util.HashMap<Integer, CachedPage>(),
            totalRows,
            0,
            DEFAULT_PAGE_SIZE
        );
        result.consoleTable();
        return result;
    }

    public String consoleTable() {
        if (pageSource != null) {
            return cachedPage(pageIndex).content;
        }
        return pages.get(pageIndex);
    }

    public int pageIndex() {
        return pageIndex;
    }

    public int pageCount() {
        if (pageSource != null) {
            return totalRows == null ? Math.max(1, pageIndex + 1) : (int) Math.max(1L, (totalRows.longValue() + pageSize - 1L) / pageSize);
        }
        return pages.size();
    }

    public int pageSize() {
        return pageSize;
    }

    public boolean hasNextPage() {
        if (pageSource != null) {
            if (totalRows != null) {
                return pageIndex + 1 < pageCount();
            }
            return cachedPage(pageIndex).rowCount == pageSize;
        }
        return pageIndex + 1 < pages.size();
    }

    public boolean hasPreviousPage() {
        return pageIndex > 0;
    }

    public SqlExecutionResult nextPage() {
        if (pageSource != null) {
            return new SqlExecutionResult(pageSource, pageCache, totalRows, pageIndex + 1, pageSize);
        }
        return new SqlExecutionResult(pages, pageIndex + 1, pageSize, null, null, null, heading, headers, rows);
    }

    public SqlExecutionResult previousPage() {
        if (pageSource != null) {
            return new SqlExecutionResult(pageSource, pageCache, totalRows, Math.max(0, pageIndex - 1), pageSize);
        }
        return new SqlExecutionResult(pages, pageIndex - 1, pageSize, null, null, null, heading, headers, rows);
    }

    public boolean hasTabularRows() {
        return pageSource != null || !headers.isEmpty();
    }

    public TabularPage tabularPage() {
        return tabularPage(pageIndex);
    }

    public TabularPage tabularPage(int index) {
        int safeIndex = Math.max(0, Math.min(index, pageCount() - 1));
        if (pageSource != null) {
            return new TabularPage(pageSource.headers(), cachedPage(safeIndex).rows, safeIndex, pageCount());
        }
        int from = safeIndex * pageSize;
        int to = Math.min(from + pageSize, rows.size());
        List<List<String>> pageRows = from >= to ? Collections.<List<String>>emptyList() : rows.subList(from, to);
        return new TabularPage(headers, pageRows, safeIndex, pageCount());
    }

    public String consoleTableWithRowNumbers() {
        return consoleTableWithRowNumbers(new TuiMessages(TuiLanguage.ENGLISH));
    }

    public String consoleTableWithRowNumbers(TuiMessages messages) {
        TuiMessages safeMessages = messages == null ? new TuiMessages(TuiLanguage.ENGLISH) : messages;
        if (!hasTabularRows()) {
            return consoleTable();
        }
        List<String> currentHeaders = pageSource == null ? headers : pageSource.headers();
        List<List<String>> currentRows = pageSource == null ? currentRows() : cachedPage(pageIndex).rows;
        List<String> numberedHeaders = new ArrayList<String>();
        numberedHeaders.add("#");
        numberedHeaders.addAll(currentHeaders == null ? Collections.<String>emptyList() : currentHeaders);
        List<List<String>> numberedRows = new ArrayList<List<String>>();
        long firstRowNumber = (long) pageIndex * (long) pageSize + 1L;
        for (int i = 0; i < currentRows.size(); i++) {
            List<String> row = new ArrayList<String>();
            row.add(String.valueOf(firstRowNumber + i));
            row.addAll(currentRows.get(i));
            numberedRows.add(row);
        }
        StringBuilder rendered = new StringBuilder();
        String currentHeading = pageSource == null ? heading : pageSource.heading();
        if (currentHeading != null && !currentHeading.trim().isEmpty()) {
            rendered.append(currentHeading).append(System.lineSeparator());
        }
        int from = pageIndex * pageSize;
        int to = from + currentRows.size();
        if (pageSource == null) {
            rendered.append(pageStatus(pageIndex, pageCount(), from, to, rows.size(), safeMessages)).append(System.lineSeparator());
        } else {
            rendered.append(pageStatus(pageIndex, totalRows, from, to, safeMessages)).append(System.lineSeparator());
        }
        rendered.append(SqlConsoleRenderer.formatRows(numberedHeaders, numberedRows));
        return rendered.toString();
    }

    private CachedPage cachedPage(int index) {
        long now = pageSource.currentTimeMillis();
        CachedPage cached = pageCache.get(Integer.valueOf(index));
        if (cached != null && now - cached.loadedAtMillis <= PAGE_CACHE_TTL_MILLIS) {
            return cached;
        }
        PageRows pageRows = pageSource.fetch(index, pageSize);
        String rendered = renderPage(pageSource.heading(), pageSource.headers(), pageRows.rows, index, totalRows);
        CachedPage refreshed = new CachedPage(rendered, pageRows.rows, now);
        pageCache.put(Integer.valueOf(index), refreshed);
        return refreshed;
    }

    private List<List<String>> currentRows() {
        int from = pageIndex * pageSize;
        int to = Math.min(from + pageSize, rows.size());
        if (from >= to) {
            return Collections.emptyList();
        }
        return rows.subList(from, to);
    }

    private static List<String> immutableStrings(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static List<List<String>> immutableRows(List<List<String>> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<List<String>> copy = new ArrayList<List<String>>();
        for (List<String> row : values) {
            copy.add(immutableStrings(row));
        }
        return Collections.unmodifiableList(copy);
    }

    private static String renderPage(String heading, List<String> headers, List<List<String>> rows, int page, Long totalRows) {
        int from = page * DEFAULT_PAGE_SIZE;
        int to = from + rows.size();
        StringBuilder rendered = new StringBuilder();
        if (heading != null && !heading.trim().isEmpty()) {
            rendered.append(heading).append(System.lineSeparator());
        }
        rendered.append(pageStatus(page, totalRows, from, to)).append(System.lineSeparator());
        rendered.append(SqlConsoleRenderer.formatRows(headers, rows));
        return rendered.toString();
    }

    private static String pageStatus(int page, int pageCount, int from, int to, int totalRows) {
        return pageStatus(page, pageCount, from, to, totalRows, new TuiMessages(TuiLanguage.ENGLISH));
    }

    private static String pageStatus(int page, int pageCount, int from, int to, int totalRows, TuiMessages messages) {
        String rows = totalRows == 0 ? "Rows 0" : "Rows " + (from + 1) + "-" + to;
        if (messages.language() == TuiLanguage.SPANISH) {
            rows = totalRows == 0 ? "Filas 0" : "Filas " + (from + 1) + "-" + to;
            return "Pagina " + (page + 1) + "/" + pageCount + " | " + rows + " | Siguiente: PageDown | Anterior: PageUp";
        }
        return "Page " + (page + 1) + "/" + pageCount + " | " + rows + " | Next: PageDown | Previous: PageUp";
    }

    private static String pageStatus(int page, Long totalRows, int from, int to) {
        return pageStatus(page, totalRows, from, to, new TuiMessages(TuiLanguage.ENGLISH));
    }

    private static String pageStatus(int page, Long totalRows, int from, int to, TuiMessages messages) {
        String pageCount = totalRows == null
            ? "?"
            : String.valueOf(Math.max(1L, (totalRows.longValue() + DEFAULT_PAGE_SIZE - 1L) / DEFAULT_PAGE_SIZE));
        String rows;
        if (totalRows != null && totalRows.longValue() == 0L) {
            rows = "Rows 0";
        } else if (to == from) {
            rows = "Rows 0";
        } else {
            rows = "Rows " + (from + 1) + "-" + to;
        }
        if (messages.language() == TuiLanguage.SPANISH) {
            rows = rows.replace("Rows", "Filas");
            return "Pagina " + (page + 1) + "/" + pageCount + " | " + rows + " | Siguiente: PageDown | Anterior: PageUp";
        }
        return "Page " + (page + 1) + "/" + pageCount + " | " + rows + " | Next: PageDown | Previous: PageUp";
    }

    public interface PageSource {
        String heading();

        List<String> headers();

        PageRows fetch(int pageIndex, int pageSize);

        long currentTimeMillis();
    }

    public static final class PageRows {

        private final List<List<String>> rows;

        public PageRows(List<List<String>> rows) {
            this.rows = rows == null ? Collections.<List<String>>emptyList() : rows;
        }
    }

    public static final class TabularPage {

        private final List<String> headers;
        private final List<List<String>> rows;
        private final int pageIndex;
        private final int pageCount;

        private TabularPage(List<String> headers, List<List<String>> rows, int pageIndex, int pageCount) {
            this.headers = immutableStrings(headers);
            this.rows = immutableRows(rows);
            this.pageIndex = pageIndex;
            this.pageCount = pageCount;
        }

        public List<String> headers() {
            return headers;
        }

        public List<List<String>> rows() {
            return rows;
        }

        public int pageIndex() {
            return pageIndex;
        }

        public int pageCount() {
            return pageCount;
        }
    }

    private static final class CachedPage {

        private final String content;
        private final int rowCount;
        private final List<List<String>> rows;
        private final long loadedAtMillis;

        public CachedPage(String content, List<List<String>> rows, long loadedAtMillis) {
            this.content = content;
            this.rows = immutableRows(rows);
            this.rowCount = this.rows.size();
            this.loadedAtMillis = loadedAtMillis;
        }
    }
}
