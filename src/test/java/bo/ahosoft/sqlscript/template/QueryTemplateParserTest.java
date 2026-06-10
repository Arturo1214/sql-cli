package bo.ahosoft.sqlscript.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class QueryTemplateParserTest {

    @Test
    public void discoversParametersInFirstOccurrenceOrderAndDeduplicatesDuplicates() {
        QueryTemplate template = QueryTemplateParser.parse(
            "select * from tickets where owner = {{owner}} and status = {{status}} or backup = {{owner}}"
        );

        assertEquals(Arrays.asList("owner", "status"), parameterNames(template.parameters()));
        assertEquals("select * from tickets where owner = {{owner}} and status = {{status}} or backup = {{owner}}", template.sql());
    }

    @Test
    public void acceptsIdentifierLikePlaceholderNamesOnly() {
        QueryTemplate template = QueryTemplateParser.parse("select * from {{schema_name}}.users where id = {{id2}}");

        assertEquals(Arrays.asList("schema_name", "id2"), parameterNames(template.parameters()));
    }

    @Test
    public void returnsEmptyParameterListForSqlWithoutPlaceholders() {
        QueryTemplate template = QueryTemplateParser.parse("select '{not a template}' as sample from dual");

        assertEquals("select '{not a template}' as sample from dual", template.sql());
        assertTrue(template.parameters().isEmpty());
    }

    @Test
    public void treatsQuotedAndMultilinePlaceholdersAsPlainTemplateText() {
        QueryTemplate template = QueryTemplateParser.parse("select '{{name}}' as label\nfrom users\nwhere id = {{id}}");

        assertEquals(Arrays.asList("name", "id"), parameterNames(template.parameters()));
    }

    @Test
    public void rejectsMalformedOrInvalidPlaceholdersExplicitly() {
        assertInvalid("select {{1bad}} from dual", "{{1bad}}");
        assertInvalid("select {{first-name}} from dual", "{{first-name}}");
        assertInvalid("select {{ }} from dual", "{{ }}");
        assertInvalid("select {{name from dual", "{{name");
        assertInvalid("select name}} from dual", "name}}");
    }

    private static List<String> parameterNames(List<TemplateParameter> parameters) {
        java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        for (TemplateParameter parameter : parameters) {
            names.add(parameter.name());
        }
        return names;
    }

    private static void assertInvalid(String sql, String expectedFragment) {
        try {
            QueryTemplateParser.parse(sql);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(expectedFragment));
            return;
        }
        throw new AssertionError("Expected invalid template for: " + sql);
    }
}
