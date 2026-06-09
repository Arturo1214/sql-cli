package bo.ahosoft.sqlscript.domain;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SqlExecutionResultTabularPageTest {

    @Test
    public void tabularPagePreservesHeadersRowsOrderAndCurrentPage() {
        List<List<String>> rows = numberedRows(105);
        SqlExecutionResult result = SqlExecutionResult.paged("SQL #1", Arrays.asList("ID", "NAME"), rows).nextPage();

        SqlExecutionResult.TabularPage page = result.tabularPage();

        assertEquals(1, page.pageIndex());
        assertEquals(2, page.pageCount());
        assertEquals(Arrays.asList("ID", "NAME"), page.headers());
        assertEquals(5, page.rows().size());
        assertEquals(Arrays.asList("101", "name-101"), page.rows().get(0));
        assertEquals(Arrays.asList("105", "name-105"), page.rows().get(4));
    }

    @Test
    public void indexedTabularPageUsesDatabasePagedCacheSemantics() {
        CountingPageSource source = new CountingPageSource(250L);
        SqlExecutionResult result = SqlExecutionResult.databasePaged(source, Long.valueOf(250L));

        SqlExecutionResult.TabularPage first = result.tabularPage(0);
        SqlExecutionResult.TabularPage third = result.tabularPage(2);
        SqlExecutionResult.TabularPage firstAgain = result.tabularPage(0);

        assertEquals(Arrays.asList("ID"), first.headers());
        assertEquals(Arrays.asList("1"), first.rows().get(0));
        assertEquals(100, first.rows().size());
        assertEquals(Arrays.asList("201"), third.rows().get(0));
        assertEquals(50, third.rows().size());
        assertEquals(3, third.pageCount());
        assertEquals(2, source.fetchCount());
        assertEquals(first.rows(), firstAgain.rows());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void tabularPageRowsAreImmutableSnapshots() {
        SqlExecutionResult result = SqlExecutionResult.paged("SQL #1", Arrays.asList("ID"), Arrays.asList(Arrays.asList("1")));

        result.tabularPage().rows().get(0).add("mutated");
    }

    private static List<List<String>> numberedRows(int count) {
        List<List<String>> rows = new ArrayList<List<String>>();
        for (int i = 1; i <= count; i++) {
            rows.add(Arrays.asList(String.valueOf(i), "name-" + i));
        }
        return rows;
    }

    private static final class CountingPageSource implements SqlExecutionResult.PageSource {

        private final long totalRows;
        private int fetchCount;

        CountingPageSource(long totalRows) {
            this.totalRows = totalRows;
        }

        public String heading() {
            return "SQL #1";
        }

        public List<String> headers() {
            return Arrays.asList("ID");
        }

        public SqlExecutionResult.PageRows fetch(int pageIndex, int pageSize) {
            fetchCount++;
            List<List<String>> rows = new ArrayList<List<String>>();
            int from = pageIndex * pageSize + 1;
            int to = (int) Math.min(totalRows, (long) from + pageSize - 1L);
            for (int i = from; i <= to; i++) {
                rows.add(Arrays.asList(String.valueOf(i)));
            }
            return new SqlExecutionResult.PageRows(rows);
        }

        public long currentTimeMillis() {
            return 1L;
        }

        int fetchCount() {
            return fetchCount;
        }
    }
}
