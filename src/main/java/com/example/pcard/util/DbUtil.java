package com.example.pcard.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接池工具类
 * 使用HikariCP管理数据库连接
 */
public class DbUtil {
    private static final Logger logger = LoggerFactory.getLogger(DbUtil.class);
    private static final HikariDataSource dataSource;

    static {
        logger.info("初始化数据库连接池...");
        HikariConfig config = new HikariConfig();

        Properties props = new Properties();
        
        // 尝试加载 db.properties，但在 Cloud 环境中允许不存在
        try (InputStream input = DbUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                props.load(input);
                logger.info("已加载 db.properties 配置文件");
            } else {
                logger.warn("未找到 db.properties，将完全依赖环境变量");
            }
        } catch (IOException e) {
            logger.warn("读取 db.properties 失败，将完全依赖环境变量", e);
        }

        // 优先从环境变量读取配置（Cloud Run 部署必需）
        config.setJdbcUrl(getConfigValue(props, "db.url", "DB_URL"));
        config.setUsername(getConfigValue(props, "db.username", "DB_USERNAME"));
        config.setPassword(getConfigValue(props, "db.password", "DB_PASSWORD"));
        config.setDriverClassName(getConfigValue(props, "db.driver", "DB_DRIVER"));

        // 验证必需配置
        if (config.getJdbcUrl() == null || config.getUsername() == null || config.getPassword() == null) {
            throw new RuntimeException("数据库连接配置不完整！请检查环境变量: DB_URL, DB_USERNAME, DB_PASSWORD");
        }

        // 连接池参数配置
        config.setMaximumPoolSize(Integer.parseInt(getConfigValue(props, "db.pool.maximumPoolSize", "DB_POOL_MAX_SIZE", "10")));
        config.setMinimumIdle(Integer.parseInt(getConfigValue(props, "db.pool.minimumIdle", "DB_POOL_MIN_IDLE", "2")));
        config.setConnectionTimeout(Long.parseLong(getConfigValue(props, "db.pool.connectionTimeout", "DB_POOL_CONN_TIMEOUT", "30000")));
        config.setIdleTimeout(Long.parseLong(getConfigValue(props, "db.pool.idleTimeout", "DB_POOL_IDLE_TIMEOUT", "600000")));
        config.setMaxLifetime(Long.parseLong(getConfigValue(props, "db.pool.maxLifetime", "DB_POOL_MAX_LIFETIME", "1800000")));
        
        // 强制设置 MySQL 会话时区为东八区（解决 CURRENT_TIMESTAMP 时区问题）
        config.setConnectionInitSql("SET time_zone = '+08:00'");

        logger.info("数据库连接池配置完成: JDBC URL={}", config.getJdbcUrl());

        dataSource = new HikariDataSource(config);

        // JVM关闭钩子,确保连接池正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (dataSource != null && !dataSource.isClosed()) {
                logger.info("关闭数据库连接池...");
                dataSource.close();
            }
        }));
        
        logger.info("数据库连接池初始化成功");
    }

    /**
     * 获取配置值,优先从环境变量读取
     * @param props 配置文件属性
     * @param propKey 配置文件键名
     * @param envKey 环境变量键名
     * @return 配置值
     */
    private static String getConfigValue(Properties props, String propKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }
        return props != null ? props.getProperty(propKey) : null;
    }

    /**
     * 获取配置值,优先从环境变量读取,支持默认值
     * @param props 配置文件属性
     * @param propKey 配置文件键名
     * @param envKey 环境变量键名
     * @param defaultValue 默认值
     * @return 配置值
     */
    private static String getConfigValue(Properties props, String propKey, String envKey, String defaultValue) {
        String value = getConfigValue(props, propKey, envKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取数据库连接
     * @return 数据库连接对象
     * @throws SQLException 获取连接失败
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 获取数据源实例(用于监控或测试)
     * @return HikariCP数据源
     */
    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}