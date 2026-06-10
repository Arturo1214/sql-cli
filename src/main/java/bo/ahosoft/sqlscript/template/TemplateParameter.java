package bo.ahosoft.sqlscript.template;

import java.util.Objects;

public final class TemplateParameter {

    private final String name;

    public TemplateParameter(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Template parameter name is required");
        }
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TemplateParameter)) {
            return false;
        }
        TemplateParameter that = (TemplateParameter) other;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
