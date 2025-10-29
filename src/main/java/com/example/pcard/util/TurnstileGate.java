package com.example.pcard.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 控制是否需要显示/验证 Cloudflare Turnstile 的网关工具。
 * 逻辑：
 * - 当触发条件发生（例如请求过于频繁）时，调用 requireForDuration 标记一段时间内需要 Turnstile。
 * - 在页面渲染时，isRequired 返回 true 则显示 Turnstile 小部件。
 * - 在后端验证通过后，调用 clear 清除标记。
 */
public class TurnstileGate {
    private static final String ATTR_REQUIRED_UNTIL = "TURNSTILE_REQUIRED_UNTIL";

    /**
     * 是否需要 Turnstile 验证（会同时检查是否过期，过期则清理）
     */
    public static boolean isRequired(HttpServletRequest request) {
        // 获取现有会话，如果不存在则返回 false（不创建）
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        Object val = session.getAttribute(ATTR_REQUIRED_UNTIL);
        if (!(val instanceof Long)) return false;
        
        long until = (Long) val;
        long now = System.currentTimeMillis();
        
        if (now >= until) {
            // 过期清理
            session.removeAttribute(ATTR_REQUIRED_UNTIL);
            return false;
        }
        return true;
    }

    /**
     * 在给定的持续时间内要求 Turnstile 验证
     */
    public static void requireForDuration(HttpServletRequest request, long durationMs) {
        if (durationMs <= 0) return;
        HttpSession session = request.getSession(true);
        long until = System.currentTimeMillis() + durationMs;
        Object existing = session.getAttribute(ATTR_REQUIRED_UNTIL);
        if (existing instanceof Long) {
            long prev = (Long) existing;
            // 取更大的过期时间，避免反复缩短
            if (until < prev) {
                until = prev;
            }
        }
        session.setAttribute(ATTR_REQUIRED_UNTIL, until);
    }

    /**
     * 清除 Turnstile 要求标记（例如验证成功后）
     */
    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(ATTR_REQUIRED_UNTIL);
        }
    }
}
