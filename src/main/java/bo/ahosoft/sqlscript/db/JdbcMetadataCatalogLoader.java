package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JdbcMetadataCatalogLoader implements MetadataCatalogLoader {

    private final JdbcConnectionFactory connectionFactory;
    private final MetadataProvider metadataProvider;

    public JdbcMetadataCatalogLoader(JdbcConnectionFactory connectionFactory, MetadataProvider metadataProvider) {
        this.connectionFactory = connectionFactory;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public MetadataCatalogSnapshot load(ConnectionConfig connectionConfig) throws SQLException {
        if (connectionConfig == null || metadataProvider == null) {
            return MetadataCatalogSnapshot.empty();
        }
        List<String> tableNames = new ArrayList<String>();
        try (
            Connection connection = connectionFactory.open(connectionConfig);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(metadataProvider.tables(null))
        ) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString(1));
            }
        }
        return MetadataCatalogSnapshot.ofTables(tableNames);
    }
}
