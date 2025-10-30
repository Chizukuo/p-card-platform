package com.example.pcard.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

        // CSP 策略：使用 unsafe-inline 以兼容 Turnstile 和 Rocket Loader
        // 在有 Cloudflare WAF 保护的情况下，这是可接受的权衡
        // img-src: 允许本地、data URL、Google Cloud Storage 图片和 placehold.co 占位图
        // script-src: 允许本地、unsafe-inline 和 Cloudflare CDN（包括 Analytics/Insights）
        // style-src: 允许本地、unsafe-inline 和 CDN (Font Awesome)
        // font-src: 允许本地和 CDN 字体
        // frame-src: 允许 Turnstile iframe
        // connect-src: 允许向 Cloudflare、Google Cloud Storage 和 Cloudflare Insights 连接
        if (!resp.containsHeader("Content-Security-Policy")) {
            String cspPolicy = "default-src 'self'; " +
                    "img-src 'self' data: https://storage.googleapis.com https://placehold.co; " +
                    "script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com https://cdnjs.cloudflare.com https://ajax.cloudflare.com https://static.cloudflareinsights.com; " +
                    "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                    "font-src 'self' https://cdnjs.cloudflare.com; " +
                    "frame-src https://challenges.cloudflare.com; " +
                    "connect-src 'self' https://challenges.cloudflare.com https://storage.googleapis.com https://cloudflareinsights.com; " +
                    "object-src 'none'; " +
                    "base-uri 'self'";
            resp.setHeader("Content-Security-Policy", cspPolicy);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
