package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
import com.example.pcard.model.Card;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/s/*")
public class ShortLinkServlet extends HttpServlet {
    private final CardDao cardDao = new CardDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null || path.length() <= 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String code = path.substring(1);
        try {
            Card card = cardDao.getCardByShortCode(code);
            if (card == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String token = card.getShareToken();
            String redirectUrl = String.format("%s/card/%s", request.getContextPath(), card.getUniqueLinkId());
            if (token != null && !token.isEmpty()) {
                redirectUrl += "?token=" + token;
            }
            // send 302 redirect (temporary)
            response.sendRedirect(redirectUrl);
        } catch (SQLException e) {
            throw new ServletException("DB error resolving short link", e);
        }
    }
}
