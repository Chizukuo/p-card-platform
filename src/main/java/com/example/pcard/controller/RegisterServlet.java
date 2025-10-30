package com.example.pcard.controller;

import com.example.pcard.dao.UserDao;
import com.example.pcard.model.User;
import com.example.pcard.util.TurnstileVerifier;
import com.example.pcard.util.TurnstileGate;
import com.example.pcard.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (TurnstileVerifier.isEnabled() && TurnstileGate.isRequired(req)) {
            req.setAttribute("turnstileSiteKey", TurnstileVerifier.getSiteKey());
        }
        req.getRequestDispatcher("register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String nickname = request.getParameter("nickname");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String agreePolicy = request.getParameter("agreePolicy");
        
        // 验证用户名格式
        String usernameError = ValidationUtil.getUsernameValidationError(username);
        if (usernameError != null) {
            request.setAttribute("errorMessage", usernameError);
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        // 验证昵称不为空且长度合理
        if (nickname == null || nickname.trim().isEmpty()) {
            request.setAttribute("errorMessage", "昵称不能为空");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        if (nickname.trim().length() > 50) {
            request.setAttribute("errorMessage", "昵称长度不能超过50位");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        // 验证密码强度
        String passwordError = ValidationUtil.getPasswordValidationError(password);
        if (passwordError != null) {
            request.setAttribute("errorMessage", passwordError);
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        // 验证密码是否匹配
        if (!password.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "两次输入的密码不一致!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        // 必须勾选隐私政策
        if (agreePolicy == null) {
            request.setAttribute("errorMessage", "请先阅读并勾选同意隐私政策");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        try {
            // Turnstile 校验（仅当被要求时）
            if (TurnstileVerifier.isEnabled() && TurnstileGate.isRequired(request)) {
                String token = request.getParameter("cf-turnstile-response");
                String clientIp = (String) request.getAttribute("clientIp");
                // 降级处理：如果 clientIp 为 null，使用请求远程地址
                if (clientIp == null) {
                    clientIp = request.getRemoteAddr();
                }
                
                if (!TurnstileVerifier.verify(token, clientIp)) {
                    request.setAttribute("errorMessage", "验证失败，请重试。");
                    if (TurnstileVerifier.isEnabled()) {
                        request.setAttribute("turnstileSiteKey", TurnstileVerifier.getSiteKey());
                    }
                    request.getRequestDispatcher("register.jsp").forward(request, response);
                    return;
                }
                // 验证成功后清除要求
                TurnstileGate.clear(request);
            }
            if (userDao.getUserByUsername(username) != null) {
                request.setAttribute("errorMessage", "用户名已存在!");
                request.getRequestDispatcher("register.jsp").forward(request, response);
                return;
            }
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setNickname(nickname.trim());
            // Hash the password before storing
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            newUser.setPassword(hashed);
            userDao.addUser(newUser);
            response.sendRedirect("login");
        } catch (SQLException e) {
            throw new ServletException("Database error during registration", e);
        }
    }
}