package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.TextColor;

public final class LanternaSqlStyleMapper {

    private LanternaSqlStyleMapper() {}

    public static TextColor foregroundFor(SqlStyle style) {
        if (style == SqlStyle.KEYWORD) {
            return TextColor.ANSI.CYAN;
        }
        if (style == SqlStyle.LITERAL || style == SqlStyle.STRING) {
            return TextColor.ANSI.GREEN;
        }
        if (style == SqlStyle.NUMBER) {
            return TextColor.ANSI.MAGENTA;
        }
        if (style == SqlStyle.IDENTIFIER) {
            return TextColor.ANSI.YELLOW;
        }
        if (style == SqlStyle.COMMENT) {
            return TextColor.ANSI.BLUE;
        }
        if (style == SqlStyle.OPERATOR || style == SqlStyle.PUNCTUATION) {
            return TextColor.ANSI.WHITE_BRIGHT;
        }
        return TextColor.ANSI.WHITE;
    }
}
