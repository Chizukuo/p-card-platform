package com.example.pcard.util;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

/**
 * 中文简繁体转换工具类
 * 使用OpenCC4J库实现高质量的简繁体转换
 */
public class ChineseConverter {
    
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
    
    public static String[] getSearchVariants(String text) {
        if (text == null || text.isEmpty()) {
            return new String[]{text};
        }
        
        String original = text;
        String simplified = toSimplified(text);
        String traditional = toTraditional(text);
        
        if (original.equals(simplified) && original.equals(traditional)) {
            return new String[]{original};
        } else if (original.equals(simplified)) {
            return new String[]{original, traditional};
        } else if (original.equals(traditional)) {
            return new String[]{original, simplified};
        } else if (simplified.equals(traditional)) {
            return new String[]{original, simplified};
        } else {
            return new String[]{original, simplified, traditional};
        }
    }
    
    public static boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }
}
