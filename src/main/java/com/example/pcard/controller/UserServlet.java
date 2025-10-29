package com.example.pcard.controller;

import com.example.pcard.dao.UserDao;
import com.example.pcard.model.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/userAction")
public class UserServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String action = request.getParameter("action");
        if ("changePassword".equals(action)) {
            changePassword(request, response, user);
        } else {
            response.sendRedirect("dashboard");
        }
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, User user) throws IOException, ServletException {
        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        HttpSession session = request.getSession();

        try {
            // 重新从数据库获取用户信息以确保密码是最新
            User currentUser = userDao.getUserByUsername(user.getUsername());

            if (currentUser == null || !BCrypt.checkpw(oldPassword, currentUser.getPassword())) {
                session.setAttribute("passwordError", "旧密码不正确！");
            } else if (newPassword == null || newPassword.isEmpty()) {
                session.setAttribute("passwordError", "新密码不能为空！");
            } else if (!newPassword.equals(confirmPassword)) {
                session.setAttribute("passwordError", "两次输入的新密码不一致！");
            } else {
                // hash the new password before storing
                String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
                userDao.updatePassword(user.getId(), hashed);
                session.setAttribute("passwordSuccess", "密码修改成功！");
                // 更新session中的用户信息
                user.setPassword(hashed);
                session.setAttribute("user", user);
            }
        } catch (SQLException e) {
            session.setAttribute("passwordError", "数据库错误，密码修改失败。");
        }

        response.sendRedirect("dashboard");
    }
}