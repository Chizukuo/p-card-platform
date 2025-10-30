package com.example.pcard.util;

/**
 * 输入验证工具类
 * 提供各种输入验证和数据清洗功能,防止XSS、SQL注入等安全问题
 */
public class ValidationUtil {

    private ValidationUtil() {
        // 工具类不应被实例化
    }

    /**
     * HTML特殊字符转义,防止XSS攻击
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * 验证用户名格式
     * 要求:4-20位,只能包含字母、数字、下划线
     * @param username 用户名
     * @return 验证结果
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]{4,20}$");
    }

    /**
     * 获取用户名验证失败的具体原因
     * @param username 用户名
     * @return 验证失败原因,如果用户名有效则返回null
     */
    public static String getUsernameValidationError(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "用户名不能为空";
        }
        if (username.length() < 4) {
            return "用户名长度至少为4位";
        }
        if (username.length() > 20) {
            return "用户名长度不能超过20位";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "用户名只能包含字母、数字和下划线";
        }
        return null;
    }

    /**
     * 验证密码强度
     * 要求:至少8位字符,必须包含字母和数字
     * @param password 密码
     * @return 验证结果
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        // 使用单个正则表达式检查所有要求：
        // (?=.*[a-zA-Z]) - 必须包含至少一个字母
        // (?=.*[0-9]) - 必须包含至少一个数字
        // .{8,} - 最少8位字符
        return password.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$");
    }

    /**
     * 获取密码验证失败的具体原因
     * @param password 密码
     * @return 验证失败原因,如果密码有效则返回null
     */
    public static String getPasswordValidationError(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() < 8) {
            return "密码长度至少为8位";
        }
        // 避免ReDoS，使用简单的字符串遍历而不是复杂正则
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                break;
            }
        }
        if (!hasLetter) {
            return "密码必须包含至少一个字母";
        }
        if (!hasDigit) {
            return "密码必须包含至少一个数字";
        }
        return null;
    }

    /**
     * 验证邮箱格式
     * @param email 邮箱地址
     * @return 验证结果
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 清理文本内容,移除危险字符
     * @param text 原始文本
     * @return 清理后的文本
     */
    public static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        // 移除控制字符
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        // 限制长度
        if (text.length() > 10000) {
            text = text.substring(0, 10000);
        }
        return text.trim();
    }

    /**
     * 验证字符串长度是否在指定范围内
     * @param str 待验证字符串
     * @param minLen 最小长度
     * @param maxLen 最大长度
     * @return 验证结果
     */
    public static boolean isLengthValid(String str, int minLen, int maxLen) {
        if (str == null) {
            return false;
        }
        int len = str.trim().length();
        return len >= minLen && len <= maxLen;
    }

    /**
     * 验证文件扩展名是否为允许的图片格式
     * @param filename 文件名
     * @return 验证结果
     */
    public static boolean isValidImageExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return ext.matches("^(jpg|jpeg|png|gif|webp)$");
    }

    /**
     * 验证整数是否在指定范围内
     * @param str 整数字符串
     * @param min 最小值
     * @param max 最大值
     * @return 验证结果
     */
    public static boolean isIntegerInRange(String str, int min, int max) {
        try {
            int value = Integer.parseInt(str);
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 清理并规范化URL
     * 只允许http和https协议
     * @param url 原始URL
     * @return 清理后的URL,无效时返回null
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        url = url.trim();
        if (!url.matches("^https?://.*")) {
            return null;
        }
        return url;
    }
}