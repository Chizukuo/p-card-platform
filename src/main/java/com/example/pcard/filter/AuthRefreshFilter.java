package com.example.pcard.filter;

import com.example.pcard.dao.UserDao;
import com.example.pcard.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 用户认证刷新过滤器
 * 功能:
 * - 每次请求时从数据库刷新用户信息到session
 * - 检测用户封禁状态,封禁用户自动登出
 * - 防止非管理员直接访问admin.jsp
 */
public class AuthRefreshFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthRefreshFilter.class);
    private UserDao userDao;

    @Override
    public void init(FilterConfig filterConfig) {
        this.userDao = new UserDao();
        logger.info("AuthRefreshFilter 初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(contextPath.length());

        // 跳过静态资源
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User sessionUser = getSessionUser(session);

        if (sessionUser != null) {
            try {
                // 从数据库获取最新用户信息
                User latest = userDao.getUserById(sessionUser.getId());
                if (latest == null || "banned".equalsIgnoreCase(latest.getStatus())) {
                    // 用户已删除或被封禁
                    handleBannedUser(session, req, resp, contextPath, path);
                    return;
                } else {
                    // 更新session中的用户信息
                    session.setAttribute("user", latest);
                    req.setAttribute("currentUser", latest);

                    // 保护admin.jsp的直接访问
                    if ("/admin.jsp".equals(path) && !latest.isAdmin()) {
                        logger.warn("非管理员用户尝试访问admin.jsp: {}", latest.getUsername());
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                        return;
                    }
                }
            } catch (SQLException e) {
                logger.error("刷新用户信息时数据库错误", e);
                throw new ServletException("Failed to refresh user from database", e);
            }
        } else {
            // 未登录用户也要保护admin.jsp
            if ("/admin.jsp".equals(path)) {
                logger.warn("未登录用户尝试访问admin.jsp");
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("AuthRefreshFilter 已销毁");
    }

    /**
     * 从session获取用户对象
     */
    private User getSessionUser(HttpSession session) {
        if (session != null) {
            Object u = session.getAttribute("user");
            if (u instanceof User) {
                return (User) u;
            }
        }
        return null;
    }

    /**
     * 处理被封禁的用户
     */
    private void handleBannedUser(HttpSession session, HttpServletRequest req,
                                   HttpServletResponse resp, String contextPath, String path)
            throws IOException {
        if (session != null) {
            session.invalidate();
        }
        if (!isAuthPath(path)) {
            logger.info("封禁用户访问被拦截,跳转到登录页");
            resp.sendRedirect(contextPath + "/login?error=banned");
        }
    }

    /**
     * 判断是否为静态资源
     */
    private boolean isStaticResource(String path) {
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/uploads/")
                || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png")
                || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".gif")
                || path.endsWith(".ico") || path.endsWith(".svg") || path.endsWith(".woff")
                || path.endsWith(".woff2") || path.endsWith(".ttf") || path.endsWith(".map");
    }

    /**
     * 判断是否为认证相关路径
     */
    private boolean isAuthPath(String path) {
        return "/login".equals(path) || "/register".equals(path) || "/logout".equals(path);
    }
}