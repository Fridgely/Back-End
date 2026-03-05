package soon.fridgely.global.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.List;

public class TruncateTablesListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestExecution(TestContext testContext) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(testContext);
        List<String> truncateQueries = getTruncateQueries(jdbcTemplate);
        truncateTables(jdbcTemplate, truncateQueries);
    }

    private List<String> getTruncateQueries(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("SELECT Concat('TRUNCATE TABLE ', TABLE_NAME, ';') AS q FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'", String.class);
    }

    private JdbcTemplate getJdbcTemplate(TestContext testContext) {
        return testContext.getApplicationContext().getBean(JdbcTemplate.class);
    }

    private void truncateTables(JdbcTemplate jdbcTemplate, List<String> truncateQueries) {
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY FALSE");
        truncateQueries.forEach(v -> execute(jdbcTemplate, v));
        resetAutoIncrement(jdbcTemplate);
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void resetAutoIncrement(JdbcTemplate jdbcTemplate) {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
            String.class
        );

        tables.forEach(tableName -> {
            execute(jdbcTemplate, "ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1");
        });
    }

    private void execute(JdbcTemplate jdbcTemplate, String query) {
        jdbcTemplate.execute(query);
    }

}