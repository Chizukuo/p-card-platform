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

import com.example.pcard.util.TurnstileGate;

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
    // 触发 Turnstile 后要求验证的持续时长（毫秒）
    private static final long CHALLENGE_COOLDOWN_MS = getEnvLong("CF_TURNSTILE_COOLDOWN_MS", 10 * 60 * 1000L); // 默认10分钟
    
    // 不同端点的限制（已大幅提高限制）
    private static final int DEFAULT_LIMIT = 1000;  // 100 -> 1000
    private static final int LOGIN_LIMIT = 50;      // 5 -> 50
    private static final int REGISTER_LIMIT = 30;   // 3 -> 30
    private static final int API_LIMIT = 500;       // 50 -> 500

    // Turnstile 触发阈值（在同一窗口内达到则要求验证）
    private static final int DEFAULT_CHALLENGE_TRIGGER = getEnvInt("CF_TURNSTILE_TRIGGER_DEFAULT", 60);
    private static final int LOGIN_CHALLENGE_TRIGGER = getEnvInt("CF_TURNSTILE_TRIGGER_LOGIN", 5);
    private static final int REGISTER_CHALLENGE_TRIGGER = getEnvInt("CF_TURNSTILE_TRIGGER_REGISTER", 3);
    private static final int API_CHALLENGE_TRIGGER = getEnvInt("CF_TURNSTILE_TRIGGER_API", 40);

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
        int challengeTrigger = getChallengeTrigger(uri);

        // 检查速率限制并在达到阈值时触发 Turnstile
        RateCheckResult rate = checkRateAndMaybeTrigger(req, clientIp, limit, challengeTrigger);
        if (!rate.allowed) {
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
    private RateCheckResult checkRateAndMaybeTrigger(HttpServletRequest req, String clientIp, int limit, int challengeTrigger) {
        long currentTime = System.currentTimeMillis();

        RequestCounter counter = requestCounts.computeIfAbsent(clientIp,
                k -> new RequestCounter(currentTime));

        int current = counter.incrementAndGet(currentTime, TIME_WINDOW);

        if (current >= challengeTrigger) {
            TurnstileGate.requireForDuration(req, CHALLENGE_COOLDOWN_MS);
            logger.info("触发 Turnstile 要求: IP={}, URI={}, count={}/{}, sessionId={}", 
                clientIp, req.getRequestURI(), current, challengeTrigger, req.getSession().getId());
        }

        boolean allowed = current <= limit;
        return new RateCheckResult(allowed, current);
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
     * 根据URI确定 Turnstile 触发阈值
     */
    private int getChallengeTrigger(String uri) {
        if (uri.contains("/login")) {
            return LOGIN_CHALLENGE_TRIGGER;
        } else if (uri.contains("/register")) {
            return REGISTER_CHALLENGE_TRIGGER;
        } else if (uri.contains("/api/") || uri.contains("/card") || uri.contains("/comment")) {
            return API_CHALLENGE_TRIGGER;
        }
        return DEFAULT_CHALLENGE_TRIGGER;
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
     * 请求计数器（使用完全同步确保计数准确）
     */
    private static class RequestCounter {
        private long windowStart;
        private final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }

        /**
         * 原子性地重置计数器（如需要）
         * @return true 表示已重置，false 表示未超时
         */
        private synchronized boolean resetIfNeeded(long currentTime, long timeWindow) {
            if (currentTime - windowStart > timeWindow) {
                windowStart = currentTime;
                count.set(0);
                return true;
            }
            return false;
        }

        /**
         * 线程安全的增量计数
         * @return 当前计数值
         */
        synchronized int incrementAndGet(long currentTime, long timeWindow) {
            // 确保在同步块内检查和重置，防止竞态条件
            resetIfNeeded(currentTime, timeWindow);
            
            // 返回当前计数并递增
            return count.incrementAndGet();
        }

        /**
         * 获取当前计数值（用于指标收集）
         */
        synchronized int get() {
            return count.get();
        }
    }

    /**
     * 速率检查结果
     */
    private static class RateCheckResult {
        final boolean allowed;
        RateCheckResult(boolean allowed, int currentCount) {
            this.allowed = allowed;
        }
    }

    private static int getEnvInt(String name, int defVal) {
        try {
            String v = System.getenv(name);
            return v == null ? defVal : Integer.parseInt(v);
        } catch (Exception e) {
            return defVal;
        }
    }

    private static long getEnvLong(String name, long defVal) {
        try {
            String v = System.getenv(name);
            return v == null ? defVal : Long.parseLong(v);
        } catch (Exception e) {
            return defVal;
        }
    }
}
