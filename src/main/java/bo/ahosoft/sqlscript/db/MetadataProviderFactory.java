package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class MetadataProviderFactory {

    private MetadataProviderFactory() {}

    public static MetadataProvider forConfig(ConnectionConfig config) {
        if (config.databaseType() == DatabaseType.POSTGRESQL) {
            return new PostgresMetadataProvider(config.schemas());
        }
        return new OracleMetadataProvider();
    }
}
