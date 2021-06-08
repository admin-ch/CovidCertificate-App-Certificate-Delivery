package ch.admin.bag.covidcertificate.backend.delivery.data.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class PostgresDbCleaner implements BeforeEachCallback {

    private final List<String> tablesToIgnore =
            List.of(
                    "public.databasechangelog",
                    "public.databasechangeloglock",
                    "public.flyway_schema_history");

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        DataSource dataSource =
                SpringExtension.getApplicationContext(extensionContext).getBean(DataSource.class);
        cleanDatabase(dataSource.getConnection());
    }

    private void cleanDatabase(Connection connection) throws SQLException {
        PreparedStatement preparedStatement =
                connection.prepareStatement(
                        "truncate " + String.join(",", loadTablesToClean(connection)));
        preparedStatement.execute();
    }

    private List<String> loadTablesToClean(Connection connection) throws SQLException {
        List<String> tablesToClean = new ArrayList<>();
        ResultSet rs =
                connection
                        .getMetaData()
                        .getTables(connection.getCatalog(), null, null, new String[] {"TABLE"});
        while (rs.next()) {
            String table = rs.getString("TABLE_SCHEM") + "." + rs.getString("TABLE_NAME");
            if (!tablesToIgnore.contains(table)) {
                tablesToClean.add(table);
            }
        }
        return tablesToClean;
    }
}
