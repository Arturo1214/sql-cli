package bo.ahosoft.sqlscript.template;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class QueryTemplateParser {

    private QueryTemplateParser() {}

    public static QueryTemplate parse(String sql) {
        String source = sql == null ? "" : sql;
        Set<String> names = new LinkedHashSet<String>();
        int offset = 0;

        while (offset < source.length()) {
            int open = source.indexOf("{{", offset);
            int strayClose = source.indexOf("}}", offset);

            if (strayClose >= 0 && (open < 0 || strayClose < open)) {
                throw invalid(source, Math.max(0, strayClose - 4), Math.min(source.length(), strayClose + 2));
            }
            if (open < 0) {
                break;
            }

            int close = source.indexOf("}}", open + 2);
            if (close < 0) {
                throw invalid(source, open, source.length());
            }

            String placeholder = source.substring(open, close + 2);
            String name = source.substring(open + 2, close);
            if (!isValidName(name)) {
                throw new IllegalArgumentException("Invalid template placeholder: " + placeholder);
            }
            names.add(name);
            offset = close + 2;
        }

        List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
        for (String name : names) {
            parameters.add(new TemplateParameter(name));
        }
        return new QueryTemplate(source, parameters);
    }

    private static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!isIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int index = 1; index < name.length(); index++) {
            if (!isIdentifierPart(name.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifierStart(char value) {
        return (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z') || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
        return isIdentifierStart(value) || (value >= '0' && value <= '9');
    }

    private static IllegalArgumentException invalid(String source, int start, int end) {
        return new IllegalArgumentException("Invalid template placeholder: " + source.substring(start, end));
    }
}
