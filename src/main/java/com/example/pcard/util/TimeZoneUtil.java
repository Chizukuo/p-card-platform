package com.example.pcard.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Properties;

/**
 * 时区工具类
 * 统一管理应用的时区配置
 */
public class TimeZoneUtil {
    private static final Logger logger = LoggerFactory.getLogger(TimeZoneUtil.class);
    private static final ZoneId APPLICATION_ZONE;
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    static {
        String timezone = DEFAULT_TIMEZONE;
        
        // 1. 优先从环境变量读取
        String envTimezone = System.getenv("APP_TIMEZONE");
        if (envTimezone != null && !envTimezone.trim().isEmpty()) {
            timezone = envTimezone.trim();
            logger.info("从环境变量读取时区配置: {}", timezone);
        } else {
            // 2. 从配置文件读取
            Properties props = new Properties();
            try (InputStream input = TimeZoneUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (input != null) {
                    props.load(input);
                    String propTimezone = props.getProperty("app.timezone");
                    if (propTimezone != null && !propTimezone.trim().isEmpty()) {
                        timezone = propTimezone.trim();
                        logger.info("从配置文件读取时区配置: {}", timezone);
                    }
                }
            } catch (IOException e) {
                logger.warn("读取时区配置失败，使用默认时区: {}", DEFAULT_TIMEZONE);
            }
        }

        try {
            APPLICATION_ZONE = ZoneId.of(timezone);
            logger.info("应用时区设置为: {}", APPLICATION_ZONE);
        } catch (Exception e) {
            logger.error("无效的时区配置: {}，使用默认时区: {}", timezone, DEFAULT_TIMEZONE, e);
            throw new RuntimeException("时区配置错误: " + timezone, e);
        }
    }

    /**
     * 获取应用配置的时区
     * @return 时区对象
     */
    public static ZoneId getApplicationZone() {
        return APPLICATION_ZONE;
    }

    /**
     * 获取时区ID字符串
     * @return 时区ID（如 "Asia/Shanghai"）
     */
    public static String getTimeZoneId() {
        return APPLICATION_ZONE.getId();
    }
}
