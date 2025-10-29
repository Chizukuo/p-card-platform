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

        // CSP 策略：使用 nonce 替代 unsafe-inline，增强 XSS 防护
        // img-src 'self' data: - 允许本地和数据 URL 图像（如 base64 编码）
        // script-src 'self' 'nonce-{nonce}' - 仅允许本地脚本和带 nonce 的内联脚本
        // style-src 'self' 'nonce-{nonce}' - 仅允许本地样式和带 nonce 的内联样式
        // connect-src 'self' https://challenges.cloudflare.com - 允许向 Cloudflare Turnstile 连接
        if (!resp.containsHeader("Content-Security-Policy")) {
            String cspPolicy = "default-src 'self'; " +
                    "img-src 'self' data:; " +
                    "script-src 'self' 'nonce-" + nonce + "' https://challenges.cloudflare.com/turnstile/; " +
                    "style-src 'self' 'nonce-" + nonce + "'; " +
                    "connect-src 'self' https://challenges.cloudflare.com; " +
                    "object-src 'none'; " +
                    "base-uri 'self'";
            resp.setHeader("Content-Security-Policy", cspPolicy);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
