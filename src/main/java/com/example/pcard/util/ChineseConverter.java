package com.example.pcard.util;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * 中文简繁体转换工具类
 */
public class ChineseConverter {

    /**
     * 转换为简体中文
     */
    public static String toSimplified(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            return ZhConverterUtil.toSimple(text);
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
        try {
            return ZhConverterUtil.toTraditional(text);
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * 获取搜索变体列表（包含原文、简体、繁体）
     */
    public static List<String> getSearchVariants(String text) {
        List<String> variants = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return variants;
        }
        
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
}