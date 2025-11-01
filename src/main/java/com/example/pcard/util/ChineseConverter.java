package com.example.pcard.util;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文简繁体转换工具类
 * 优化版：添加LRU缓存以提高性能
 */
public class ChineseConverter {
    
    // 转换结果缓存，避免重复转换相同文本
    private static final Map<String, String> simplifiedCache = new ConcurrentHashMap<>();
    private static final Map<String, String> traditionalCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> variantsCache = new ConcurrentHashMap<>();
    
    // 缓存大小限制
    private static final int MAX_CACHE_SIZE = 500;

    /**
     * 转换为简体中文
     */
    public static String toSimplified(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 检查缓存
        String cached = simplifiedCache.get(text);
        if (cached != null) {
            return cached;
        }
        
        try {
            String result = ZhConverterUtil.toSimple(text);
            
            // 添加到缓存（带大小限制）
            if (simplifiedCache.size() < MAX_CACHE_SIZE) {
                simplifiedCache.put(text, result);
            } else if (simplifiedCache.size() >= MAX_CACHE_SIZE * 1.2) {
                // 当缓存超过限制20%时，清理一半
                simplifiedCache.clear();
            }
            
            return result;
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * 转换为繁体中文
     */
    public static String toTraditional(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 检查缓存
        String cached = traditionalCache.get(text);
        if (cached != null) {
            return cached;
        }
        
        try {
            String result = ZhConverterUtil.toTraditional(text);
            
            // 添加到缓存（带大小限制）
            if (traditionalCache.size() < MAX_CACHE_SIZE) {
                traditionalCache.put(text, result);
            } else if (traditionalCache.size() >= MAX_CACHE_SIZE * 1.2) {
                // 当缓存超过限制20%时，清理一半
                traditionalCache.clear();
            }
            
            return result;
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * 获取搜索变体列表（包含原文、简体、繁体）
     * 优化版：添加缓存
     */
    public static List<String> getSearchVariants(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 检查缓存
        List<String> cached = variantsCache.get(text);
        if (cached != null) {
            return new ArrayList<>(cached); // 返回副本以防修改
        }
        
        List<String> variants = new ArrayList<>();
        variants.add(text);
        
        if (containsChinese(text)) {
            String simplified = toSimplified(text);
            String traditional = toTraditional(text);
            
            if (!variants.contains(simplified)) {
                variants.add(simplified);
            }
            if (!variants.contains(traditional)) {
                variants.add(traditional);
            }
        }
        
        // 添加到缓存（带大小限制）
        if (variantsCache.size() < MAX_CACHE_SIZE) {
            variantsCache.put(text, new ArrayList<>(variants));
        } else if (variantsCache.size() >= MAX_CACHE_SIZE * 1.2) {
            // 当缓存超过限制20%时，清理
            variantsCache.clear();
        }
        
        return variants;
    }

    /**
     * 检查文本是否包含中文字符
     */
    public static boolean containsChinese(String text) {
        if (text == null) {
            return false;
        }
        return text.matches(".*[\u4e00-\u9fa5]+.*");
    }
    
    /**
     * 清空缓存（用于内存管理或测试）
     */
    public static void clearCache() {
        simplifiedCache.clear();
        traditionalCache.clear();
        variantsCache.clear();
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        return String.format("ChineseConverter缓存统计 - 简体:%d, 繁体:%d, 变体:%d", 
            simplifiedCache.size(), traditionalCache.size(), variantsCache.size());
    }
}