package top.lvpi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private static final List<String> REQUIRED_TABLES = Arrays.asList(
        "lp_user", "lp_doc", "lp_doc_section", "lp_img", "lp_topic", "lp_doc_topic", "lp_note", "lp_search_report", "lp_note_report_relate", "lp_doc_file", "lp_file"
    );

    @Override
    public void run(String... args) throws Exception {
        String targetDb = "LPMS";
        String baseUrl = parseBaseUrl(jdbcUrl);
        try (Connection conn = DriverManager.getConnection(baseUrl, username, password)) {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?");
            ps.setString(1, targetDb);
            ResultSet rs = ps.executeQuery();
            boolean exists = false;
            if (rs.next()) {
                exists = rs.getInt(1) > 0;
            }
            if (!exists) {
                log.info("数据库 {} 不存在，正在创建数据库...", targetDb);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE DATABASE " + targetDb + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                }
            }
        }

        try {
            // 检查数据库表是否存在
            List<String> missingTables = checkMissingTables();
            if (!missingTables.isEmpty()) {
                log.info("Missing tables: {}", missingTables);
                log.info("Initializing database tables...");
                
                // 读取schema.sql文件
                ClassPathResource schemaResource = new ClassPathResource("db/schema.sql");
                String schemaSql = StreamUtils.copyToString(
                    schemaResource.getInputStream(), 
                    StandardCharsets.UTF_8
                );
                
                // 分割SQL语句并执行
                String[] sqlStatements = schemaSql.split(";");
                for (String statement : sqlStatements) {
                    if (statement.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        jdbcTemplate.execute(statement);
                    } catch (Exception e) {
                        log.error("Error executing SQL statement: {}", statement, e);
                        throw e;
                    }
                }
                log.info("Database tables initialized successfully");

                // 初始化用户数据
                initializeUserData();
            } else {
                log.info("All required database tables already exist");
                // 检查是否需要初始化用户数据
                if (isUserTableEmpty()) {
                    initializeUserData();
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize database", e);
            throw e;
        }
    }

    private String parseBaseUrl(String jdbcUrl) {
        int startIndex = jdbcUrl.indexOf("://") + 3;
        int slashIndex = jdbcUrl.indexOf("/", startIndex);
        if (slashIndex == -1) {
            return jdbcUrl; // 意外情况
        }
        int questionMarkIndex = jdbcUrl.indexOf("?", slashIndex);
        String base;
        if (questionMarkIndex == -1) {
            base = jdbcUrl.substring(0, slashIndex + 1);
        } else {
            base = jdbcUrl.substring(0, slashIndex + 1) + jdbcUrl.substring(questionMarkIndex);
        }
        return base;
    }

    private List<String> checkMissingTables() {
        return REQUIRED_TABLES.stream()
            .filter(table -> {
                try {
                    Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?",
                        Integer.class,
                        table
                    );
                    return count == null || count == 0;
                } catch (Exception e) {
                    log.warn("Error checking table {}: {}", table, e.getMessage());
                    return true;
                }
            })
            .toList();
    }

    private boolean isUserTableEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lp_user",
                Integer.class
            );
            return count == null || count == 0;
        } catch (Exception e) {
            log.warn("Error checking if user table is empty: {}", e.getMessage());
            return true;
        }
    }

    private void initializeUserData() {
        try {
            log.info("Initializing data...");
            ClassPathResource initResource = new ClassPathResource("db/init_mysql.sql");
            String initSql = StreamUtils.copyToString(
                initResource.getInputStream(),
                StandardCharsets.UTF_8
            );

            String[] initStatements = initSql.split(";");
            for (String statement : initStatements) {
                if (statement.trim().isEmpty()) {
                    continue;
                }
                try {
                    jdbcTemplate.execute(statement);
                } catch (Exception e) {
                    log.error("Error executing init SQL statement: {}", statement, e);
                    throw e;
                }
            }
            log.info("Data initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize data", e);
            throw new RuntimeException("Failed to initialize data", e);
        }

    }
} 