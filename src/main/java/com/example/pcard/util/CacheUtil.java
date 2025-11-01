package com.example.pcard.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 简单的内存缓存工具类
 * 使用LRU策略和TTL机制
 */
public class CacheUtil {
    private static final Logger logger = LoggerFactory.getLogger(CacheUtil.class);
    
    private static final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 默认缓存过期时间（毫秒）
    private static final long DEFAULT_TTL = 5 * 60 * 1000; // 5分钟
    
    // 最大缓存条目数
    private static final int MAX_CACHE_SIZE = 1000;
    
    static {
        // 定期清理过期缓存
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanExpiredEntries();
            } catch (Exception e) {
                logger.error("清理缓存时发生错误", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
        
        logger.info("缓存工具初始化完成，默认TTL: {}ms, 最大条目数: {}", DEFAULT_TTL, MAX_CACHE_SIZE);
    }
    
    /**
     * 缓存条目
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long expiryTime;
        private volatile long lastAccessTime;
        
        public CacheEntry(T value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public T getValue() {
            this.lastAccessTime = System.currentTimeMillis();
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
    
    /**
     * 添加缓存
     * @param key 缓存键
     * @param value 缓存值
     */
    public static <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL);
    }
    
    /**
     * 添加缓存，指定TTL
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间（毫秒）
     */
    public static <T> void put(String key, T value, long ttl) {
        if (key == null || value == null) {
            return;
        }
        
        // 如果缓存已满，清理最旧的条目
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictLRU();
        }
        
        cache.put(key, new CacheEntry<>(value, ttl));
    }
    
    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        if (key == null) {
            return null;
        }
        
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        
        return (T) entry.getValue();
    }
    
    /**
     * 移除缓存
     * @param key 缓存键
     */
    public static void remove(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }
    
    /**
     * 清空缓存
     */
    public static void clear() {
        cache.clear();
        logger.info("缓存已清空");
    }
    
    /**
     * 清理过期的缓存条目
     */
    private static void cleanExpiredEntries() {
        int removed = 0;
        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            logger.debug("清理了 {} 个过期缓存条目，剩余: {}", removed, cache.size());
        }
    }
    
    /**
     * LRU驱逐策略 - 移除最久未访问的条目
     */
    private static void evictLRU() {
        String oldestKey = null;
        long oldestAccessTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestAccessTime) {
                oldestAccessTime = accessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
            logger.debug("LRU驱逐缓存条目: {}", oldestKey);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getStats() {
        return String.format("缓存统计: 当前条目数=%d, 最大条目数=%d", cache.size(), MAX_CACHE_SIZE);
    }
    
    /**
     * 关闭缓存工具（通常在应用关闭时调用）
     */
    public static void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clear();
        logger.info("缓存工具已关闭");
    }
}
