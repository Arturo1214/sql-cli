package bo.ahosoft.sqlscript.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class QueryTemplateRendererTest {

    @Test
    public void rendersRawSubstitutionWithoutEscapingOrPreparedStatementSemantics() {
        QueryTemplate template = QueryTemplateParser.parse("select * from users where name = '{{name}}' and id = {{id}}");
        Map<String, String> values = values("name", "O'Reilly", "id", "42");

        RenderedQueryTemplate rendered = QueryTemplateRenderer.render(template, values);

        assertEquals("select * from users where name = 'O'Reilly' and id = 42", rendered.sql());
        assertEquals(Arrays.asList("name", "id"), parameterNames(rendered.parameters()));
    }

    @Test
    public void reusesOneValueForRepeatedPlaceholders() {
        QueryTemplate template = QueryTemplateParser.parse("select * from tickets where owner = {{user}} or reviewer = {{user}}");

        RenderedQueryTemplate rendered = QueryTemplateRenderer.render(template, values("user", "ana"));

        assertEquals("select * from tickets where owner = ana or reviewer = ana", rendered.sql());
    }

    @Test
    public void failsWithExplicitMissingValueMessage() {
        QueryTemplate template = QueryTemplateParser.parse("select * from orders where customer_id = {{customerId}}");

        try {
            QueryTemplateRenderer.render(template, new LinkedHashMap<String, String>());
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("customerId"));
            return;
        }
        throw new AssertionError("Expected missing value error");
    }

    @Test
    public void keepsSqlWithoutPlaceholdersUnchanged() {
        QueryTemplate template = QueryTemplateParser.parse("select 1 from dual");

        RenderedQueryTemplate rendered = QueryTemplateRenderer.render(template, new LinkedHashMap<String, String>());

        assertEquals("select 1 from dual", rendered.sql());
        assertTrue(rendered.parameters().isEmpty());
    }

    @Test
    public void rendersMultilineSql() {
        QueryTemplate template = QueryTemplateParser.parse(
            "select *\nfrom invoices\nwhere issued_at >= {{fromDate}}\n  and issued_at < {{toDate}}"
        );

        RenderedQueryTemplate rendered = QueryTemplateRenderer.render(
            template,
            values("fromDate", "date '2026-06-01'", "toDate", "date '2026-07-01'")
        );

        assertEquals("select *\nfrom invoices\nwhere issued_at >= date '2026-06-01'\n  and issued_at < date '2026-07-01'", rendered.sql());
    }

    private static Map<String, String> values(String... pairs) {
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }
        return values;
    }

    private static List<String> parameterNames(List<TemplateParameter> parameters) {
        java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        for (TemplateParameter parameter : parameters) {
            names.add(parameter.name());
        }
        return names;
    }
}
