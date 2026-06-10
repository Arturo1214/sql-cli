package bo.ahosoft.sqlscript.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RenderedQueryTemplate {

    private final String sql;
    private final List<TemplateParameter> parameters;

    public RenderedQueryTemplate(String sql, List<TemplateParameter> parameters) {
        this.sql = sql == null ? "" : sql;
        this.parameters = Collections.unmodifiableList(
            new ArrayList<TemplateParameter>(parameters == null ? Collections.<TemplateParameter>emptyList() : parameters)
        );
    }

    public String sql() {
        return sql;
    }

    public List<TemplateParameter> parameters() {
        return parameters;
    }
}
