package com.example.pcard.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 统一添加安全相关响应头（与 Cloudflare 配合）
 */
@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // HSTS 仅在 HTTPS 有效；在 CF 终止 TLS 的场景，已通过 Cloudflare 下发也可；这里兜底
        if (req.isSecure()) {
            // 一年，包含子域，预加载（按需）
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }

        // 基础安全头
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("X-XSS-Protection", "0"); // 现代浏览器无需该保护

        // 生成 nonce 用于内联脚本和样式，提高安全性同时避免 unsafe-inline
        String nonce = UUID.randomUUID().toString();
        req.setAttribute("nonce", nonce);

        // CSP 策略：允许必要的外部资源和 Turnstile
        // img-src: 允许本地、data URL 和 Google Cloud Storage 图片
        // script-src: 允许本地、nonce、unsafe-inline 和 Cloudflare
        // style-src: 允许本地、nonce、unsafe-inline 和 CDN (Font Awesome)
        // font-src: 允许本地和 CDN 字体
        // frame-src: 允许 Turnstile iframe
        // connect-src: 允许向 Cloudflare Turnstile 和 Google Cloud Storage 连接
        if (!resp.containsHeader("Content-Security-Policy")) {
            String cspPolicy = "default-src 'self'; " +
                    "img-src 'self' data: https://storage.googleapis.com; " +
                    "script-src 'self' 'nonce-" + nonce + "' 'unsafe-inline' https://challenges.cloudflare.com https://cdnjs.cloudflare.com; " +
                    "style-src 'self' 'nonce-" + nonce + "' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                    "font-src 'self' https://cdnjs.cloudflare.com; " +
                    "frame-src https://challenges.cloudflare.com; " +
                    "connect-src 'self' https://challenges.cloudflare.com https://storage.googleapis.com; " +
                    "object-src 'none'; " +
                    "base-uri 'self'";
            resp.setHeader("Content-Security-Policy", cspPolicy);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
