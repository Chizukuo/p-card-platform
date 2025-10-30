package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
import com.example.pcard.dao.CommentDao;
import com.example.pcard.dao.UserDao;
import com.example.pcard.model.Card;
import com.example.pcard.model.Comment;
import com.example.pcard.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final CardDao cardDao = new CardDao();
    private final CommentDao commentDao = new CommentDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }
        
        String view = safe(request.getParameter("view"));
        
        // 处理导出请求
        if ("exportUsers".equals(view)) {
            exportUsers(request, response);
            return;
        } else if ("exportCards".equals(view)) {
            exportCards(request, response);
            return;
        }
        
        try {
            // 统计信息
            int totalUsers = userDao.countUsers(null, null, null);
            int totalAdmins = userDao.getAdminCount();
            int totalActiveUsers = userDao.countUsers(null, null, "active");
            int totalBannedUsers = userDao.countUsers(null, null, "banned");
            int totalCards = cardDao.countAdminCards(null, null);
            int totalPublicCards = cardDao.countAdminCards(null, "PUBLIC");
            int totalPrivateCards = cardDao.countAdminCards(null, "PRIVATE");
            
            request.setAttribute("totalUsers", totalUsers);
            request.setAttribute("totalAdmins", totalAdmins);
            request.setAttribute("totalActiveUsers", totalActiveUsers);
            request.setAttribute("totalBannedUsers", totalBannedUsers);
            request.setAttribute("totalCards", totalCards);
            request.setAttribute("totalPublicCards", totalPublicCards);
            request.setAttribute("totalPrivateCards", totalPrivateCards);
            
            // 根据视图显示不同内容
            if ("comments".equals(view)) {
                handleCommentsView(request, response);
                return;
            } else if ("stats".equals(view)) {
                handleStatsView(request, response);
                return;
            }
            
            // 默认视图：用户和名片管理
            handleDefaultView(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error in admin panel", e);
        }
    }
    
    private void handleDefaultView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
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
    }
    
    private void handleCommentsView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        String searchQuery = safe(request.getParameter("commentQ"));
        int page = parsePage(request.getParameter("page"));
        int pageSize = 20;
        int offset = (page - 1) * pageSize;
        
        List<Comment> allComments = getAllCommentsFlat(searchQuery, offset, pageSize);
        int totalComments = countAllComments(searchQuery);
        int totalPages = (totalComments + pageSize - 1) / pageSize;
        
        request.setAttribute("allComments", allComments);
        request.setAttribute("page", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalComments", totalComments);
        request.setAttribute("commentQ", searchQuery);
        request.getRequestDispatcher("admin-comments.jsp").forward(request, response);
    }
    
    private void handleStatsView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        // 获取统计数据已在doGet中设置
        request.getRequestDispatcher("admin-stats.jsp").forward(request, response);
    }
    
    private List<Comment> getAllCommentsFlat(String searchQuery, int offset, int limit) throws SQLException {
        return commentDao.getAllCommentsFlat(searchQuery, offset, limit);
    }
    
    private int countAllComments(String searchQuery) throws SQLException {
        return commentDao.countAllComments(searchQuery);
    }
    
    private void exportUsers(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<User> users = userDao.getAllUsers();
            
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"users_" + System.currentTimeMillis() + ".csv\"");
            
            PrintWriter writer = response.getWriter();
            writer.println("ID,用户名,昵称,角色,状态");
            
            for (User user : users) {
                writer.println(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\"",
                    user.getId(),
                    escapeCsv(user.getUsername()),
                    escapeCsv(user.getNickname()),
                    escapeCsv(user.getRole()),
                    escapeCsv(user.getStatus())));
            }
            
            writer.flush();
        } catch (SQLException e) {
            throw new ServletException("Failed to export users", e);
        }
    }
    
    private void exportCards(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<Card> cards = cardDao.getAllCards();
            
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"cards_" + System.currentTimeMillis() + ".csv\"");
            
            PrintWriter writer = response.getWriter();
            writer.println("ID,所有者,P名,担当,地区,可见性,链接ID");
            
            for (Card card : cards) {
                writer.println(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    card.getId(),
                    escapeCsv(card.getOwnerUsername()),
                    escapeCsv(card.getProducerName()),
                    escapeCsv(card.getIdolName()),
                    escapeCsv(card.getRegion()),
                    escapeCsv(card.getVisibility()),
                    escapeCsv(card.getUniqueLinkId())));
            }
            
            writer.flush();
        } catch (SQLException e) {
            throw new ServletException("Failed to export cards", e);
        }
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        String action = request.getParameter("action");
        
        try {
            // 批量操作
            if ("batchBanUsers".equals(action)) {
                handleBatchBanUsers(request, response);
                return;
            } else if ("batchDeleteUsers".equals(action)) {
                handleBatchDeleteUsers(request, response);
                return;
            } else if ("batchDeleteCards".equals(action)) {
                handleBatchDeleteCards(request, response);
                return;
            } else if ("batchSetCardVisibility".equals(action)) {
                handleBatchSetCardVisibility(request, response);
                return;
            } else if ("deleteComment".equals(action)) {
                handleDeleteComment(request, response);
                return;
            } else if ("updateUserNickname".equals(action)) {
                handleUpdateUserNickname(request, response);
                return;
            }
            
            // 原有的单个操作
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
            
            // 重定向回原视图
            String view = safe(request.getParameter("view"));
            if (view != null && !view.isEmpty()) {
                response.sendRedirect("admin?view=" + view);
            } else {
                response.sendRedirect("admin");
            }
        } catch (SQLException e) {
            throw new ServletException("Database error in admin panel", e);
        }
    }
    
    private void handleBatchBanUsers(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        String[] userIds = request.getParameterValues("userIds");
        if (userIds != null && userIds.length > 0) {
            for (String id : userIds) {
                try {
                    int userId = Integer.parseInt(id);
                    User user = userDao.getUserById(userId);
                    if (user != null && !"admin".equals(user.getRole())) {
                        userDao.updateUserStatus(userId, "banned");
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid ID
                }
            }
            request.getSession().setAttribute("adminSuccess", "批量封禁成功，共处理 " + userIds.length + " 个用户");
        }
    }
    
    private void handleBatchDeleteUsers(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        String[] userIds = request.getParameterValues("userIds");
        int deleted = 0;
        if (userIds != null && userIds.length > 0) {
            for (String id : userIds) {
                try {
                    int userId = Integer.parseInt(id);
                    User user = userDao.getUserById(userId);
                    if (user != null && !"admin".equals(user.getRole())) {
                        userDao.deleteUser(userId);
                        deleted++;
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid ID
                }
            }
            request.getSession().setAttribute("adminSuccess", "批量删除成功，共删除 " + deleted + " 个用户");
        }
    }
    
    private void handleBatchDeleteCards(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        String[] cardIds = request.getParameterValues("cardIds");
        int deleted = 0;
        if (cardIds != null && cardIds.length > 0) {
            for (String id : cardIds) {
                try {
                    int cardId = Integer.parseInt(id);
                    cardDao.deleteCard(cardId);
                    deleted++;
                } catch (NumberFormatException e) {
                    // Skip invalid ID
                }
            }
            request.getSession().setAttribute("adminSuccess", "批量删除成功，共删除 " + deleted + " 张名片");
        }
    }
    
    private void handleBatchSetCardVisibility(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        String[] cardIds = request.getParameterValues("cardIds");
        String visibility = request.getParameter("batchVisibility");
        int updated = 0;
        
        if (cardIds != null && cardIds.length > 0 && visibility != null) {
            for (String id : cardIds) {
                try {
                    int cardId = Integer.parseInt(id);
                    Card card = cardDao.getCardById(cardId);
                    if (card != null) {
                        card.setVisibility(visibility);
                        if ("LINK_ONLY".equals(visibility)) {
                            if (card.getShareToken() == null || card.getShareToken().isEmpty()) {
                                card.setShareToken(UUID.randomUUID().toString());
                            }
                        }
                        if ("PUBLIC".equals(visibility) || "LINK_ONLY".equals(visibility)) {
                            if (card.getShortCode() == null || card.getShortCode().isEmpty()) {
                                card.setShortCode(generateShortCode());
                            }
                        }
                        cardDao.updateCard(card);
                        updated++;
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid ID
                }
            }
            request.getSession().setAttribute("adminSuccess", "批量更新可见性成功，共更新 " + updated + " 张名片");
        }
    }
    
    private void handleDeleteComment(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int commentId = Integer.parseInt(request.getParameter("commentId"));
        commentDao.deleteComment(commentId);
        request.getSession().setAttribute("adminSuccess", "评论删除成功");
    }
    
    private void handleUpdateUserNickname(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int userId = Integer.parseInt(request.getParameter("userId"));
        String nickname = request.getParameter("nickname");
        if (nickname != null && !nickname.trim().isEmpty()) {
            userDao.updateUserNickname(userId, nickname.trim());
            request.getSession().setAttribute("adminSuccess", "昵称更新成功");
        }
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