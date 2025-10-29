package com.example.pcard.controller;

import com.example.pcard.dao.UserDao;
import com.example.pcard.model.User;
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
        req.getRequestDispatcher("register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String nickname = request.getParameter("nickname");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String agreePolicy = request.getParameter("agreePolicy");
        
        // 必须勾选隐私政策
        if (agreePolicy == null) {
            request.setAttribute("errorMessage", "请先阅读并勾选同意隐私政策");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        // 验证密码是否匹配
        if (!password.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "两次输入的密码不一致!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        try {
            if (userDao.getUserByUsername(username) != null) {
                request.setAttribute("errorMessage", "用户名已存在!");
                request.getRequestDispatcher("register.jsp").forward(request, response);
                return;
            }
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setNickname(nickname);
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