package com.example.pcard.controller;

import com.example.pcard.dao.UserDao;
import com.example.pcard.model.User;
import com.example.pcard.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 用户登录控制器
 * 处理用户登录请求和验证
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 保存 redirect 参数到请求属性,传递给登录页面
        String redirect = req.getParameter("redirect");
        if (redirect != null && !redirect.isEmpty()) {
            req.setAttribute("redirect", redirect);
        }
        req.getRequestDispatcher("login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // 基础验证
        if (username == null || password == null ||
                username.trim().isEmpty() || password.trim().isEmpty()) {
            logger.warn("登录尝试失败：用户名或密码为空");
            request.setAttribute("errorMessage", "用户名和密码不能为空!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // 用户名格式验证
        if (!ValidationUtil.isValidUsername(username)) {
            logger.warn("登录尝试失败：无效的用户名格式 - {}", username);
            request.setAttribute("errorMessage", "用户名格式不正确!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        try {
            User user = userDao.getUserByUsername(username);
            if (user != null && BCrypt.checkpw(password, user.getPassword())) {
                // 检查用户状态
                if ("banned".equals(user.getStatus())) {
                    logger.info("被封禁用户尝试登录：{}", username);
                    request.setAttribute("errorMessage", "您的账户已被封禁!");
                    request.getRequestDispatcher("login.jsp").forward(request, response);
                    return;
                }

                // 登录成功
                HttpSession session = request.getSession();
                session.setAttribute("user", user);
                logger.info("用户登录成功：{} (角色: {})", username, user.getRole());
                
                // 获取 redirect 参数
                String redirect = request.getParameter("redirect");
                
                // 优先跳转到 redirect 参数指定的页面
                if (redirect != null && !redirect.isEmpty() && isValidRedirect(redirect)) {
                    logger.info("登录后重定向到：{}", redirect);
                    response.sendRedirect(redirect);
                }
                // 否则根据角色跳转
                else if ("admin".equals(user.getRole())) {
                    response.sendRedirect("admin");
                } else {
                    response.sendRedirect(request.getContextPath() + "/");
                }
            } else {
                logger.warn("登录尝试失败：用户名或密码错误 - {}", username);
                request.setAttribute("errorMessage", "用户名或密码错误!");
                request.getRequestDispatcher("login.jsp").forward(request, response);
            }
        } catch (SQLException e) {
            logger.error("登录时数据库错误", e);
            throw new ServletException("Database error during login", e);
        }
    }

    /**
     * 验证 redirect URL 是否安全
     * 只允许相对路径,防止开放重定向漏洞
     */
    private boolean isValidRedirect(String redirect) {
        if (redirect == null || redirect.isEmpty()) {
            return false;
        }
        // 只允许相对路径(不以 http:// 或 https:// 或 // 开头)
        if (redirect.startsWith("http://") || redirect.startsWith("https://") || redirect.startsWith("//")) {
            logger.warn("拒绝不安全的重定向URL：{}", redirect);
            return false;
        }
        // 允许以 / 或字母开头的相对路径
        return redirect.startsWith("/") || redirect.matches("^[a-zA-Z].*");
    }
}