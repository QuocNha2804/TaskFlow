package com.taskflow.config;

import com.zaxxer.hikari.*;
import com.taskflow.exception.AppException;
import java.nio.file.*;
import java.io.*;
import java.util.Properties;

public final class DatabaseConfig {
    private DatabaseConfig() {}
    public static HikariDataSource open() {
        Properties p = new Properties();
        Path path = Path.of(System.getProperty("taskflow.config", "db.properties"));
        if (!Files.exists(path) && System.getProperty("taskflow.config") == null) {
            Path executable = ProcessHandle.current().info().command().map(Path::of).orElse(null);
            if (executable != null && executable.getParent() != null) {
                Path besideExecutable = executable.getParent().resolve("db.properties");
                if (Files.exists(besideExecutable)) path = besideExecutable;
            }
        }
        try (InputStream in = Files.exists(path) ? Files.newInputStream(path)
                : DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) p.load(in);
            String url = System.getenv().getOrDefault("TASKFLOW_DB_URL", p.getProperty("db.url", ""));
            if (url.isBlank()) throw new AppException("Chưa có db.properties. Sao chép db.properties.example và cấu hình MariaDB theo README.");
            HikariConfig c = new HikariConfig();
            c.setJdbcUrl(url);
            c.setUsername(System.getenv().getOrDefault("TASKFLOW_DB_USER", p.getProperty("db.username", "taskflow")));
            c.setPassword(System.getenv().getOrDefault("TASKFLOW_DB_PASSWORD", p.getProperty("db.password", "")));
            c.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.poolSize", "5")));
            c.setConnectionTimeout(8000); c.setInitializationFailTimeout(8000);
            return new HikariDataSource(c);
        } catch (IOException | IllegalArgumentException e) { throw new AppException("Không đọc được cấu hình database.", e); }
    }
}
