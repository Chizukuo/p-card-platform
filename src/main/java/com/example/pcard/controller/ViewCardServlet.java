package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
import com.example.pcard.dao.CommentDao;
import com.example.pcard.model.Card;
import com.example.pcard.model.User;
import com.example.pcard.model.Comment;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/card/*")
public class ViewCardServlet extends HttpServlet {
    private final CardDao cardDao = new CardDao();
    private final CommentDao commentDao = new CommentDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String uniqueLinkId = pathInfo.substring(1);

        try {
            Card card = cardDao.getCardByUniqueLinkId(uniqueLinkId);
            if (card != null) {
                // visibility enforcement
                String visibility = card.getVisibility();
                boolean allowed = false;
                // session user (may be null)
                User sessionUser = null;
                if (request.getSession(false) != null) {
                    sessionUser = (User) request.getSession(false).getAttribute("user");
                }

                if ("PUBLIC".equalsIgnoreCase(visibility)) {
                    allowed = true;
                } else if ("LINK_ONLY".equalsIgnoreCase(visibility)) {
                    // allow if token matches or owner/admin
                    String token = request.getParameter("token");
                    if (token != null && token.equals(card.getShareToken())) {
                        allowed = true;
                    } else if (sessionUser != null && (sessionUser.isAdmin() || sessionUser.getId() == card.getUserId())) {
                        allowed = true;
                    }
                } else if ("PRIVATE".equalsIgnoreCase(visibility)) {
                    if (sessionUser != null && (sessionUser.isAdmin() || sessionUser.getId() == card.getUserId())) {
                        allowed = true;
                    }
                }

                if (!allowed) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to view this card.");
                    return;
                }
                List<Comment> comments = commentDao.getCommentsByCardId(card.getId());
                request.setAttribute("card", card);
                request.setAttribute("comments", comments);
                request.getRequestDispatcher("/viewCard.jsp").forward(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Card not found");
            }
        } catch (SQLException e) {
            throw new ServletException("Database error retrieving card", e);
        }
    }
}