package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
import com.example.pcard.dao.CommentDao;
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
import java.sql.SQLException;

@WebServlet("/commentAction")
public class CommentServlet extends HttpServlet {
    private final CommentDao commentDao = new CommentDao();
    private final CardDao cardDao = new CardDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        String action = request.getParameter("action");
        String cardLink = request.getParameter("cardLink");

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        if (action == null || cardLink == null) {
            response.sendRedirect(request.getContextPath());
            return;
        }

        try {
            switch(action) {
                case "add":
                    addComment(request, response, user, cardLink);
                    break;
                case "reply":
                    replyComment(request, response, user, cardLink);
                    break;
                case "delete":
                    deleteComment(request, response, user, cardLink);
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException("Database error with comments", e);
        }
    }

    private void addComment(HttpServletRequest request, HttpServletResponse response, User user, String cardLink) throws SQLException, IOException {
        int cardId = Integer.parseInt(request.getParameter("cardId"));
        String content = request.getParameter("content");

        if (content != null && !content.trim().isEmpty()) {
            Comment comment = new Comment();
            comment.setCardId(cardId);
            comment.setUserId(user.getId());
            comment.setUsername(user.getUsername());
            comment.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            comment.setContent(content.trim());
            comment.setParentId(null);  // 顶级评论
            commentDao.addComment(comment);
        }
        response.sendRedirect("card/" + cardLink);
    }
    
    private void replyComment(HttpServletRequest request, HttpServletResponse response, User user, String cardLink) throws SQLException, IOException {
        int cardId = Integer.parseInt(request.getParameter("cardId"));
        int parentId = Integer.parseInt(request.getParameter("parentId"));
        String replyToUsername = request.getParameter("replyToUsername");
        String replyToNickname = request.getParameter("replyToNickname");
        String content = request.getParameter("content");

        if (content != null && !content.trim().isEmpty()) {
            Comment comment = new Comment();
            comment.setCardId(cardId);
            comment.setUserId(user.getId());
            comment.setUsername(user.getUsername());
            comment.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            comment.setContent(content.trim());
            comment.setParentId(parentId);
            comment.setReplyToUsername(replyToUsername);
            comment.setReplyToNickname(replyToNickname != null ? replyToNickname : replyToUsername);
            commentDao.addComment(comment);
        }
        response.sendRedirect("card/" + cardLink);
    }

    private void deleteComment(HttpServletRequest request, HttpServletResponse response, User user, String cardLink) throws SQLException, IOException {
        int commentId = Integer.parseInt(request.getParameter("commentId"));
        int cardId = Integer.parseInt(request.getParameter("cardId"));

        Comment comment = commentDao.getCommentById(commentId);
        Card card = cardDao.getCardById(cardId);

        if (comment != null && card != null) {
            // Allow deletion if user is admin, card owner, or comment owner
            if (user.isAdmin() || user.getId() == card.getUserId() || user.getId() == comment.getUserId()) {
                commentDao.deleteComment(commentId);
            }
        }
        response.sendRedirect("card/" + cardLink);
    }
}