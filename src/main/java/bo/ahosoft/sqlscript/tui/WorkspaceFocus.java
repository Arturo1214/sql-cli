package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public enum WorkspaceFocus {
    CONNECTIONS,
    EDITOR,
    RESULTS,
    HELP;

    public WorkspaceFocus next() {
        WorkspaceFocus[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public WorkspaceFocus previous() {
        WorkspaceFocus[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }
}
