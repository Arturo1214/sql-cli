package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public enum TuiLanguage {
    ENGLISH,
    SPANISH;

    public TuiLanguage toggle() {
        return this == ENGLISH ? SPANISH : ENGLISH;
    }
}
