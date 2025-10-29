package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
// import com.example.pcard.model.Card; // unused - dashboard now uses list of cards
import com.example.pcard.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    private final CardDao cardDao = new CardDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }
        User user = (User) session.getAttribute("user");
        try {
            // Fetch all cards for this user and optionally a selected card
            java.util.List<com.example.pcard.model.Card> myCards = cardDao.getCardsByUserId(user.getId());
            request.setAttribute("myCards", myCards);

            String cardIdParam = request.getParameter("cardId");
            String createParam = request.getParameter("create");
            // Default: do NOT show edit form on login. Only show when user explicitly selects a card or clicks create.
            request.setAttribute("createMode", "1".equals(createParam));
            if (cardIdParam != null) {
                try {
                    int cardId = Integer.parseInt(cardIdParam);
                    com.example.pcard.model.Card selected = null;
                    for (com.example.pcard.model.Card c : myCards) {
                        if (c.getId() == cardId) { selected = c; break; }
                    }
                    request.setAttribute("myCard", selected);
                } catch (NumberFormatException ignored) {
                    request.setAttribute("myCard", null);
                }
            } else {
                // do not set a default myCard — keep it null unless explicitly requested
                request.setAttribute("myCard", null);
            }
            request.getRequestDispatcher("dashboard.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error fetching card data", e);
        }
    }
}