package bo.ahosoft.sqlscript.sql;

import static org.junit.Assert.assertEquals;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.TextColor;
import org.junit.Test;

public class LanternaSqlStyleMapperTest {

    @Test
    public void mapsSqlStylesToReadableLanternaColors() {
        assertEquals(TextColor.ANSI.CYAN, LanternaSqlStyleMapper.foregroundFor(SqlStyle.KEYWORD));
        assertEquals(TextColor.ANSI.GREEN, LanternaSqlStyleMapper.foregroundFor(SqlStyle.LITERAL));
        assertEquals(TextColor.ANSI.YELLOW, LanternaSqlStyleMapper.foregroundFor(SqlStyle.IDENTIFIER));
    }

    @Test
    public void mapsCommentsPunctuationAndMissingStyleToSafeFallbackColors() {
        assertEquals(TextColor.ANSI.BLUE, LanternaSqlStyleMapper.foregroundFor(SqlStyle.COMMENT));
        assertEquals(TextColor.ANSI.WHITE_BRIGHT, LanternaSqlStyleMapper.foregroundFor(SqlStyle.PUNCTUATION));
        assertEquals(TextColor.ANSI.WHITE_BRIGHT, LanternaSqlStyleMapper.foregroundFor(SqlStyle.OPERATOR));
        assertEquals(TextColor.ANSI.WHITE, LanternaSqlStyleMapper.foregroundFor(null));
    }
}
