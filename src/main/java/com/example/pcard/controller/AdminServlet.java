package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
import com.example.pcard.dao.UserDao;
import com.example.pcard.model.Card;
import com.example.pcard.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final CardDao cardDao = new CardDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }
        try {
            // Filters
            String userQ = safe(request.getParameter("userQ"));
            String role = safe(request.getParameter("role"));
            String status = safe(request.getParameter("status"));
            String cardQ = safe(request.getParameter("cardQ"));
            String visibility = safe(request.getParameter("visibility"));

            // Pagination (separate for users and cards)
            int userPage = parsePage(request.getParameter("uPage"));
            int cardPage = parsePage(request.getParameter("cPage"));
            int pageSize = 10; // fixed for now; can be made configurable later
            int userOffset = (userPage - 1) * pageSize;
            int cardOffset = (cardPage - 1) * pageSize;

            List<User> allUsers;
            List<Card> allCards;
            int userTotal = 0;
            int cardTotal = 0;

            if ((userQ != null && !userQ.isEmpty()) || (role != null && !role.isEmpty() && !"all".equalsIgnoreCase(role)) || (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status))) {
                userTotal = userDao.countUsers(userQ, role, status);
                allUsers = userDao.searchUsersPaged(userQ, role, status, userOffset, pageSize);
            } else {
                userTotal = userDao.countUsers(null, null, null);
                allUsers = userDao.getAllUsersPaged(userOffset, pageSize);
            }

            if ((cardQ != null && !cardQ.isEmpty()) || (visibility != null && !visibility.isEmpty() && !"all".equalsIgnoreCase(visibility))) {
                cardTotal = cardDao.countAdminCards(cardQ, visibility);
                allCards = cardDao.adminSearchCardsPaged(cardQ, visibility, cardOffset, pageSize);
            } else {
                cardTotal = cardDao.countAdminCards(null, null);
                allCards = cardDao.getAllCardsPaged(cardOffset, pageSize);
            }

            // total pages
            int userTotalPages = (userTotal + pageSize - 1) / pageSize;
            int cardTotalPages = (cardTotal + pageSize - 1) / pageSize;

            // keep filters
            request.setAttribute("userQ", userQ);
            request.setAttribute("role", role);
            request.setAttribute("status", status);
            request.setAttribute("cardQ", cardQ);
            request.setAttribute("visibility", visibility);

            request.setAttribute("allUsers", allUsers);
            request.setAttribute("allCards", allCards);
            request.setAttribute("userPage", userPage);
            request.setAttribute("cardPage", cardPage);
            request.setAttribute("userTotal", userTotal);
            request.setAttribute("cardTotal", cardTotal);
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("userTotalPages", userTotalPages);
            request.setAttribute("cardTotalPages", cardTotalPages);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error in admin panel", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        String action = request.getParameter("action");
        if ("updateUserStatus".equals(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");
                userDao.updateUserStatus(userId, status);
            } catch (SQLException | NumberFormatException e) {
                throw new ServletException("Failed to update user status", e);
            }
        } else if ("deleteUser".equals(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                User target = userDao.getUserById(userId);
                if (target != null && "admin".equals(target.getRole())) {
                    int admins = userDao.getAdminCount();
                    if (admins <= 1) {
                        request.getSession().setAttribute("adminError", "必须至少保留一个管理员，无法删除最后一位管理员");
                        response.sendRedirect("admin");
                        return;
                    }
                }
                userDao.deleteUser(userId);
            } catch (SQLException | NumberFormatException e) {
                throw new ServletException("Failed to delete user", e);
            }
        } else if ("updateUserRole".equals(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String role = request.getParameter("role");
                if (!"admin".equals(role) && !"user".equals(role)) {
                    throw new IllegalArgumentException("Invalid role");
                }
                User target = userDao.getUserById(userId);
                User actor = (User) request.getSession().getAttribute("user");

                if (target != null && actor != null) {
                    boolean isSuperAdmin = "admin".equalsIgnoreCase(actor.getUsername());
                    boolean selfDemote = actor.getId() == target.getId();
                    
                    // 超级管理员 admin 不能修改自己的权限
                    if ("admin".equalsIgnoreCase(target.getUsername()) && selfDemote) {
                        request.getSession().setAttribute("adminError", "超级管理员 admin 不能修改自己的权限");
                        response.sendRedirect("admin");
                        return;
                    }
                    
                    // 如果不是超级管理员,则应用普通管理员的限制
                    if (!isSuperAdmin) {
                        // 普通管理员不能给其他用户升权
                        if ("user".equals(target.getRole()) && "admin".equals(role)) {
                            request.getSession().setAttribute("adminError", "只有超级管理员 admin 可以给其他用户升权");
                            response.sendRedirect("admin");
                            return;
                        }
                        
                        // 普通管理员不能降级其他管理员
                        if ("admin".equals(target.getRole()) && "user".equals(role) && !selfDemote) {
                            request.getSession().setAttribute("adminError", "只有超级管理员 admin 可以降级其他管理员");
                            response.sendRedirect("admin");
                            return;
                        }
                        
                        // 普通管理员给自己降权时,确保至少保留一个管理员
                        if (selfDemote && "admin".equals(target.getRole()) && "user".equals(role)) {
                            int admins = userDao.getAdminCount();
                            if (admins <= 1) {
                                request.getSession().setAttribute("adminError", "必须至少保留一个管理员，无法将最后一位管理员降级");
                                response.sendRedirect("admin");
                                return;
                            }
                        }
                    }
                    
                    // 超级管理员在降级其他管理员时,也要确保至少保留一个管理员
                    if (isSuperAdmin && "admin".equals(target.getRole()) && "user".equals(role) && !selfDemote) {
                        int admins = userDao.getAdminCount();
                        if (admins <= 1) {
                            request.getSession().setAttribute("adminError", "必须至少保留一个管理员，无法将最后一位管理员降级");
                            response.sendRedirect("admin");
                            return;
                        }
                    }
                }

                userDao.updateUserRole(userId, role);
            } catch (SQLException | IllegalArgumentException e) {
                throw new ServletException("Failed to update user role", e);
            }
        } else if ("setCardVisibility".equals(action)) {
            try {
                int cardId = Integer.parseInt(request.getParameter("cardId"));
                String visibility = request.getParameter("visibility");
                if (!"PUBLIC".equals(visibility) && !"LINK_ONLY".equals(visibility) && !"PRIVATE".equals(visibility)) {
                    throw new IllegalArgumentException("Invalid visibility");
                }
                Card card = cardDao.getCardById(cardId);
                if (card != null) {
                    card.setVisibility(visibility);
                    if ("LINK_ONLY".equals(visibility)) {
                        if (card.getShareToken() == null || card.getShareToken().isEmpty()) {
                            card.setShareToken(java.util.UUID.randomUUID().toString());
                        }
                    }
                    if ("PUBLIC".equals(visibility) || "LINK_ONLY".equals(visibility)) {
                        if (card.getShortCode() == null || card.getShortCode().isEmpty()) {
                            card.setShortCode(generateShortCode());
                        }
                    }
                    cardDao.updateCard(card);
                }
            } catch (SQLException | IllegalArgumentException e) {
                throw new ServletException("Failed to update card visibility", e);
            }
        }
        response.sendRedirect("admin");
    }

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return false;
        }
        User user = (User) session.getAttribute("user");
        return user.isAdmin();
    }

    private String safe(String s) { return s == null ? null : s.trim(); }

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private String generateShortCode() {
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        int len = 7;
        for (int i = 0; i < len; i++) {
            sb.append(BASE62.charAt(rnd.nextInt(BASE62.length())));
        }
        return sb.toString();
    }

    private int parsePage(String p) {
        try {
            int n = Integer.parseInt(p);
            return n >= 1 ? n : 1;
        } catch (Exception e) {
            return 1;
        }
    }
}