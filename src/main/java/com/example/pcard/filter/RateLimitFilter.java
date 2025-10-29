package com.example.pcard.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 速率限制过滤器
 * 提供应用层的速率限制，作为Cloudflare速率限制的补充
 */
@WebFilter("/*")
public class RateLimitFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    // 存储每个IP的请求计数
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    // 时间窗口 (毫秒)
    private static final long TIME_WINDOW = 60000; // 1分钟
    
    // 不同端点的限制（已大幅提高限制）
    private static final int DEFAULT_LIMIT = 1000;  // 100 -> 1000
    private static final int LOGIN_LIMIT = 50;      // 5 -> 50
    private static final int REGISTER_LIMIT = 30;   // 3 -> 30
    private static final int API_LIMIT = 500;       // 50 -> 500

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("RateLimitFilter 初始化完成");
        
        // 启动清理线程，定期清理过期的计数器
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // 每分钟清理一次
                    cleanupExpiredCounters();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 跳过静态资源
        String uri = req.getRequestURI();
        if (isStaticResource(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 获取客户端IP (优先使用Cloudflare提供的真实IP)
        String clientIp = (String) req.getAttribute("clientIp");
        if (clientIp == null) {
            clientIp = req.getHeader("CF-Connecting-IP");
            if (clientIp == null) {
                clientIp = req.getRemoteAddr();
            }
        }

        // 确定限制值
        int limit = getLimit(uri);
        
        // 检查速率限制
        if (!checkRateLimit(clientIp, limit)) {
            logger.warn("速率限制触发: IP={}, URI={}, Limit={}", clientIp, uri, limit);
            
            resp.setStatus(429); // Too Many Requests
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader("Retry-After", "60");
            resp.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        requestCounts.clear();
        logger.info("RateLimitFilter 已销毁");
    }

    /**
     * 检查速率限制
     */
    private boolean checkRateLimit(String clientIp, int limit) {
        long currentTime = System.currentTimeMillis();
        
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, 
                k -> new RequestCounter(currentTime));
        
        return counter.increment(currentTime, TIME_WINDOW, limit);
    }

    /**
     * 根据URI确定限制值
     */
    private int getLimit(String uri) {
        if (uri.contains("/login")) {
            return LOGIN_LIMIT;
        } else if (uri.contains("/register")) {
            return REGISTER_LIMIT;
        } else if (uri.contains("/api/") || uri.contains("/card") || uri.contains("/comment")) {
            return API_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    /**
     * 判断是否为静态资源
     */
    private boolean isStaticResource(String uri) {
        return uri.endsWith(".css") || uri.endsWith(".js") || 
               uri.endsWith(".png") || uri.endsWith(".jpg") || 
               uri.endsWith(".jpeg") || uri.endsWith(".gif") || 
               uri.endsWith(".ico") || uri.endsWith(".svg") ||
               uri.endsWith(".woff") || uri.endsWith(".woff2") || 
               uri.endsWith(".ttf") || uri.endsWith(".map");
    }

    /**
     * 清理过期的计数器
     */
    private void cleanupExpiredCounters() {
        long currentTime = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> 
                currentTime - entry.getValue().windowStart > TIME_WINDOW * 2);
        
        logger.debug("清理过期计数器，剩余: {}", requestCounts.size());
    }

    /**
     * 请求计数器
     */
    private static class RequestCounter {
        private volatile long windowStart;
        private final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }

        synchronized boolean increment(long currentTime, long timeWindow, int limit) {
            // 如果超出时间窗口，重置计数器
            if (currentTime - windowStart > timeWindow) {
                windowStart = currentTime;
                count.set(0);
            }

            // 检查是否超过限制
            int current = count.incrementAndGet();
            return current <= limit;
        }
    }
}
