package com.example.pcard.util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Cloudflare Turnstile 服务端验证工具
 * 文档：https://developers.cloudflare.com/turnstile/ 
 */
public class TurnstileVerifier {
    private static final Logger logger = LoggerFactory.getLogger(TurnstileVerifier.class);
    private static final String VERIFY_ENDPOINT = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final Gson GSON = new Gson();

    public static boolean isEnabled() {
        String secret = System.getenv("CF_TURNSTILE_SECRET");
        String siteKey = System.getenv("CF_TURNSTILE_SITE_KEY");
        return secret != null && !secret.isEmpty() && siteKey != null && !siteKey.isEmpty();
    }

    public static String getSiteKey() {
        return System.getenv("CF_TURNSTILE_SITE_KEY");
    }

    /**
     * 验证 Turnstile token
     * @param token 前端提交的 cf-turnstile-response
     * @param remoteIp 客户端真实 IP（可选）
     * @return true 验证通过，false 验证失败或 Cloudflare 不可用
     */
    public static boolean verify(String token, String remoteIp) {
        String secret = System.getenv("CF_TURNSTILE_SECRET");
        if (secret == null || secret.isEmpty()) {
            logger.warn("Turnstile 未配置密钥，跳过验证");
            return true; // 未启用则放行
        }
        if (token == null || token.isEmpty()) {
            logger.info("Turnstile token 缺失，IP={}", remoteIp);
            return false;
        }

        try {
            StringBuilder postData = new StringBuilder();
            postData.append("secret=").append(URLEncoder.encode(secret, StandardCharsets.UTF_8));
            postData.append("&response=").append(URLEncoder.encode(token, StandardCharsets.UTF_8));
            if (remoteIp != null && !remoteIp.isEmpty()) {
                postData.append("&remoteip=").append(URLEncoder.encode(remoteIp, StandardCharsets.UTF_8));
            }

            byte[] bytes = postData.toString().getBytes(StandardCharsets.UTF_8);
            URL url = new URL(VERIFY_ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);  // 5秒连接超时
            conn.setReadTimeout(5000);     // 5秒读取超时
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setFixedLengthStreamingMode(bytes.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warn("Turnstile 验证 HTTP 状态码: {}，IP={}", code, remoteIp);
                // 5xx 服务器错误时，考虑放行（故障转移）
                if (code >= 500 && code < 600) {
                    logger.warn("Cloudflare 服务暂时不可用（{}），降级处理：允许用户登录", code);
                    return true;  // 降级策略：服务不可用时允许用户操作
                }
                return false;
            }

            TurnstileResponse resp = GSON.fromJson(
                new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8), 
                TurnstileResponse.class
            );
            
            if (!resp.success) {
                logger.info("Turnstile 验证失败, 错误: {}, IP={}", 
                    java.util.Arrays.toString(resp.errorCodes), remoteIp);
            }
            return resp.success;
            
        } catch (java.net.SocketTimeoutException e) {
            logger.warn("Turnstile 验证超时（{}ms），IP={}，降级处理：允许用户登录", 
                5000, remoteIp, e);
            return true;  // 超时时降级放行
            
        } catch (IOException e) {
            logger.error("Turnstile 验证异常，IP={}", remoteIp, e);
            // 网络异常时的处理策略：
            // - 严格模式（当前）：return false; 
            // - 容错模式：return true;
            // 根据业务需求选择，这里采用容错模式以提高可用性
            logger.warn("网络异常导致验证失败，采用降级策略允许用户登录");
            return true;  // 降级策略：网络不可用时允许用户操作
        }
    }

    @SuppressWarnings("unused")
    private static class TurnstileResponse {
        boolean success;
        @SerializedName("error-codes")
        String[] errorCodes;
        @SerializedName("challenge_ts")
        String challengeTs;
        String hostname;
        @SerializedName("action")
        String action;
        @SerializedName("cdata")
        String cdata;
    }
}
