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
import java.util.List;

@WebServlet("")
public class HomeServlet extends HttpServlet {
    private final CardDao cardDao = new CardDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // --- 彻底修复乱码：同时设置请求和响应编码 ---
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        // ------------------------------------------

        String q = request.getParameter("q");
        int limit = parseIntOrDefault(request.getParameter("limit"), 12);
        int offset = parseIntOrDefault(request.getParameter("offset"), 0);
        boolean asJson = "1".equals(request.getParameter("format"));
        try {
            List<Card> cards;
            if (q != null && !q.trim().isEmpty()) {
                cards = cardDao.searchPublicCardsPaged(q.trim(), offset, limit);
                request.setAttribute("query", q.trim());
            } else {
                cards = cardDao.getPublicCardsPaged(offset, limit);
            }

            if (asJson) {
                // 输出 JSON 供动态加载使用
                response.setContentType("application/json; charset=UTF-8");
                com.google.gson.Gson gson = new com.google.gson.Gson();
                // 仅返回前端需要的字段
                java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
                for (Card c : cards) {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", c.getId());
                    m.put("uniqueLinkId", c.getUniqueLinkId());
                    m.put("producerName", c.getProducerName());
                    m.put("idolName", c.getIdolName());
                    m.put("region", c.getRegion());
                    m.put("cardFrontPath", c.getCardFrontPath());
                    list.add(m);
                }
                response.getWriter().write(gson.toJson(list));
                return;
            }

            request.setAttribute("cards", cards);
            request.getRequestDispatcher("index.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error fetching public cards", e);
        }
    }

    private int parseIntOrDefault(String s, int d) {
        try { return Integer.parseInt(s); } catch (Exception e) { return d; }
    }
}