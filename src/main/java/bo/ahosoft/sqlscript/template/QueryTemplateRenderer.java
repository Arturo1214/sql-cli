package bo.ahosoft.sqlscript.template;

import java.util.Map;

public final class QueryTemplateRenderer {

    private QueryTemplateRenderer() {}

    public static RenderedQueryTemplate render(QueryTemplate template, Map<String, String> values) {
        if (template == null) {
            throw new IllegalArgumentException("Template is required");
        }
        String renderedSql = template.sql();
        for (TemplateParameter parameter : template.parameters()) {
            String name = parameter.name();
            if (values == null || !values.containsKey(name) || values.get(name) == null) {
                throw new IllegalArgumentException("Missing value for template parameter: " + name);
            }
            renderedSql = renderedSql.replace("{{" + name + "}}", values.get(name));
        }
        return new RenderedQueryTemplate(renderedSql, template.parameters());
    }
}
