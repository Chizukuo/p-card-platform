package com.example.pcard.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Cloudflare集成过滤器
 * 功能:
 * - 提取和验证Cloudflare请求头
 * - Bot检测和防护
 * - 地理位置信息提取
 * - 真实IP地址获取
 */
public class CloudflareFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CloudflareFilter.class);
    
    // Cloudflare Bot Management Score阈值 (1-99, 越低越可能是bot)
    private static final int BOT_SCORE_THRESHOLD = 30;
    
    // 需要严格bot检测的路径
    private static final String[] PROTECTED_PATHS = {
        "/login", "/register", "/card", "/comment", "/admin"
    };

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("CloudflareFilter 初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 提取Cloudflare信息
        String cfConnectingIp = req.getHeader("CF-Connecting-IP");
        String cfIpCountry = req.getHeader("CF-IPCountry");
        String cfRay = req.getHeader("CF-Ray");
        String cfBotScore = req.getHeader("CF-Bot-Management-Score");
    String xForwardedProto = req.getHeader("X-Forwarded-Proto");
    String cfVisitor = req.getHeader("CF-Visitor"); // JSON: {"scheme":"https"}
        
        // 获取真实IP地址 (优先使用Cloudflare提供的IP)
        String realIp = cfConnectingIp != null ? cfConnectingIp : req.getRemoteAddr();
        
        // 将信息存入request attributes供应用使用
        req.setAttribute("clientIp", realIp);
        req.setAttribute("clientCountry", cfIpCountry);
        req.setAttribute("cfRay", cfRay);
        
    // 记录详细的访问信息
        String uri = req.getRequestURI();
        String method = req.getMethod();
        String userAgent = req.getHeader("User-Agent");
        
        logger.debug("Request: {} {} | IP: {} | Country: {} | CF-Ray: {} | UA: {}", 
                method, uri, realIp, cfIpCountry, cfRay, userAgent);

        // Bot检测 - 如果有Bot Management Score
        if (cfBotScore != null && !cfBotScore.isEmpty()) {
            try {
                int botScore = Integer.parseInt(cfBotScore);
                req.setAttribute("botScore", botScore);
                
                // 检查是否访问受保护的路径
                if (isProtectedPath(uri) && botScore < BOT_SCORE_THRESHOLD) {
                    logger.warn("可疑Bot访问被阻止: IP={}, Score={}, Path={}, CF-Ray={}", 
                            realIp, botScore, uri, cfRay);
                    
                    // 可以选择直接阻止或者启用Cloudflare Challenge
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().write("{\"error\":\"Bot detected. Access denied.\"}");
                    return;
                }
                
                if (botScore < 50) {
                    logger.info("中等可疑Bot访问: IP={}, Score={}, Path={}", realIp, botScore, uri);
                }
            } catch (NumberFormatException e) {
                logger.warn("无效的Bot Score值: {}", cfBotScore);
            }
        }

        // 地理位置阻止 (示例: 阻止特定国家)
        // if (cfIpCountry != null && isBlockedCountry(cfIpCountry)) {
        //     logger.warn("来自被阻止国家的访问: Country={}, IP={}, CF-Ray={}", 
        //             cfIpCountry, realIp, cfRay);
        //     resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        //     resp.getWriter().write("Access from your country is not allowed.");
        //     return;
        // }

        // 协议/HTTPS 适配：如果经过 Cloudflare 且实际为 HTTPS，则包装 request，确保 isSecure()/getScheme() 正确
        boolean isHttps = "https".equalsIgnoreCase(xForwardedProto) ||
                (cfVisitor != null && cfVisitor.toLowerCase().contains("\"scheme\":\"https\""));
        if (isHttps && !req.isSecure()) {
            req = new SchemeAwareRequestWrapper(req, true);
        }

        // 检查是否来自Cloudflare (可选的额外安全检查)
        if (cfConnectingIp == null && isProductionEnvironment()) {
            logger.warn("请求不来自Cloudflare: IP={}, URI={}", req.getRemoteAddr(), uri);
            // 在生产环境中，所有请求应该通过Cloudflare
            // 可以选择阻止直接访问
        }

        // 将可能被包装过的 req 继续传递
        chain.doFilter(req, response);
    }

    @Override
    public void destroy() {
        logger.info("CloudflareFilter 已销毁");
    }

    /**
     * 检查路径是否需要严格的bot保护
     */
    private boolean isProtectedPath(String uri) {
        for (String path : PROTECTED_PATHS) {
            if (uri.contains(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查国家是否被阻止 (示例)
     */
    @SuppressWarnings("unused")
    private boolean isBlockedCountry(String countryCode) {
        // 示例: 添加需要阻止的国家代码
        // return Arrays.asList("XX", "YY").contains(countryCode);
        return false;
    }

    /**
     * 检查是否为生产环境
     */
    private boolean isProductionEnvironment() {
        String env = System.getenv("APP_ENV");
        return "production".equalsIgnoreCase(env);
    }

    /**
     * 根据代理头修正协议的 Request 包装器
     */
    private static class SchemeAwareRequestWrapper extends HttpServletRequestWrapper {
        private final boolean secure;

        public SchemeAwareRequestWrapper(HttpServletRequest request, boolean secure) {
            super(request);
            this.secure = secure;
        }

        @Override
        public boolean isSecure() {
            return secure || super.isSecure();
        }

        @Override
        public String getScheme() {
            return secure ? "https" : super.getScheme();
        }

        @Override
        public int getServerPort() {
            if (secure) {
                // 如果被 Cloudflare 终止 TLS，应用感知 443 更合理
                return 443;
            }
            return super.getServerPort();
        }
    }
}
