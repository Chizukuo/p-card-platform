package com.example.pcard.dao;

import com.example.pcard.model.Card;
import com.example.pcard.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 名片数据访问对象
 * 处理电子名片相关的数据库操作
 */
public class CardDao {

    /**
     * 添加新名片
     * @param card 名片对象
     * @throws SQLException 数据库操作异常
     */
    public void addCard(Card card) throws SQLException {
        String sql = "INSERT INTO cards (user_id, producer_name, region, idol_name, card_front_path, card_back_path, unique_link_id, custom_sns, visibility, share_token, short_code, image_orientation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, card.getUserId());
            ps.setString(2, card.getProducerName());
            ps.setString(3, card.getRegion());
            ps.setString(4, card.getIdolName());
            ps.setString(5, card.getCardFrontPath());
            ps.setString(6, card.getCardBackPath());
            ps.setString(7, UUID.randomUUID().toString());
            ps.setString(8, card.getCustomSns());
            ps.setString(9, card.getVisibility());
            ps.setString(10, card.getShareToken());
            ps.setString(11, card.getShortCode());
            ps.setString(12, card.getImageOrientation());
            ps.executeUpdate();
        }
    }

    public Card getCardByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM cards WHERE user_id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCard(rs);
                }
            }
        }
        return null;
    }

    // New: get all cards belonging to a user
    public List<Card> getCardsByUserId(int userId) throws SQLException {
        List<Card> cards = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE c.user_id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    public Card getCardById(int cardId) throws SQLException {
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE c.id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    return card;
                }
            }
        }
        return null;
    }

    public Card getCardByUniqueLinkId(String uniqueLinkId) throws SQLException {
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE c.unique_link_id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uniqueLinkId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    return card;
                }
            }
        }
        return null;
    }

    public Card getCardByShareToken(String token) throws SQLException {
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE c.share_token = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    return card;
                }
            }
        }
        return null;
    }

    public Card getCardByShortCode(String shortCode) throws SQLException {
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE c.short_code = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shortCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    return card;
                }
            }
        }
        return null;
    }

    public void updateCard(Card card) throws SQLException {
        String sql = "UPDATE cards SET producer_name = ?, region = ?, idol_name = ?, card_front_path = ?, card_back_path = ?, custom_sns = ?, visibility = ?, share_token = ?, short_code = ?, image_orientation = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, card.getProducerName());
            ps.setString(2, card.getRegion());
            ps.setString(3, card.getIdolName());
            ps.setString(4, card.getCardFrontPath());
            ps.setString(5, card.getCardBackPath());
            ps.setString(6, card.getCustomSns());
            ps.setString(7, card.getVisibility());
            ps.setString(8, card.getShareToken());
            ps.setString(9, card.getShortCode());
            ps.setString(10, card.getImageOrientation());
            ps.setInt(11, card.getId());
            ps.executeUpdate();
        }
    }

    public void deleteCard(int cardId) throws SQLException {
        String sql = "DELETE FROM cards WHERE id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            ps.executeUpdate();
        }
    }

    public List<Card> getAllCards() throws SQLException {
        List<Card> cards = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id";
        try (Connection conn = DbUtil.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Card card = mapRowToCard(rs);
                card.setOwnerUsername(rs.getString("username"));
                cards.add(card);
            }
        }
        return cards;
    }

    public List<Card> getAllCardsPaged(int offset, int limit) throws SQLException {
        List<Card> cards = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id ORDER BY c.id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    public List<Card> getPublicCards() throws SQLException {
        List<Card> cards = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE (c.visibility IS NULL OR c.visibility = 'PUBLIC')";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    public List<Card> getPublicCardsPaged(int offset, int limit) throws SQLException {
        List<Card> cards = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id " +
                "WHERE (c.visibility IS NULL OR c.visibility = 'PUBLIC') ORDER BY c.id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    /**
     * 模糊搜索公开卡片（producer_name / idol_name / region / unique_link_id）。
     * 使用参数化查询并限制返回数量以避免扫描过多记录。
     */
    public List<Card> searchPublicCards(String query) throws SQLException {
        List<Card> cards = new ArrayList<>();
        if (query == null) query = "";
        String q = "%" + query.toLowerCase().trim() + "%";
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id " +
                "WHERE (c.visibility IS NULL OR c.visibility = 'PUBLIC') " +
                "AND (LOWER(c.producer_name) LIKE ? OR LOWER(c.idol_name) LIKE ? OR LOWER(c.region) LIKE ? OR LOWER(c.unique_link_id) LIKE ?) " +
                "ORDER BY c.id DESC LIMIT 100"; // 安全上限，避免一次返回过多
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);
            ps.setString(4, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    public List<Card> searchPublicCardsPaged(String query, int offset, int limit) throws SQLException {
        List<Card> cards = new ArrayList<>();
        if (query == null) query = "";
        String q = "%" + query.toLowerCase().trim() + "%";
        String sql = "SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id " +
                "WHERE (c.visibility IS NULL OR c.visibility = 'PUBLIC') " +
                "AND (LOWER(c.producer_name) LIKE ? OR LOWER(c.idol_name) LIKE ? OR LOWER(c.region) LIKE ? OR LOWER(c.unique_link_id) LIKE ?) " +
                "ORDER BY c.id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);
            ps.setString(4, q);
            ps.setInt(5, limit);
            ps.setInt(6, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    // Admin: search cards with optional visibility, and match owner username too
    public List<Card> adminSearchCards(String q, String visibility) throws SQLException {
        List<Card> cards = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(c.producer_name) LIKE ? OR LOWER(c.idol_name) LIKE ? OR LOWER(c.region) LIKE ? OR LOWER(c.unique_link_id) LIKE ? OR LOWER(u.username) LIKE ?)");
            params.add(like); params.add(like); params.add(like); params.add(like); params.add(like);
        }
        if (visibility != null && !visibility.trim().isEmpty() && !"all".equalsIgnoreCase(visibility)) {
            sql.append(" AND c.visibility = ?");
            params.add(visibility);
        }
        sql.append(" ORDER BY c.id DESC");

        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    public List<Card> adminSearchCardsPaged(String q, String visibility, int offset, int limit) throws SQLException {
        List<Card> cards = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT c.*, u.username FROM cards c JOIN users u ON c.user_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(c.producer_name) LIKE ? OR LOWER(c.idol_name) LIKE ? OR LOWER(c.region) LIKE ? OR LOWER(c.unique_link_id) LIKE ? OR LOWER(u.username) LIKE ?)");
            params.add(like); params.add(like); params.add(like); params.add(like); params.add(like);
        }
        if (visibility != null && !visibility.trim().isEmpty() && !"all".equalsIgnoreCase(visibility)) {
            sql.append(" AND c.visibility = ?");
            params.add(visibility);
        }
        sql.append(" ORDER BY c.id DESC LIMIT ? OFFSET ?");

        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1; for (Object p : params) ps.setObject(i++, p);
            ps.setInt(i++, limit);
            ps.setInt(i, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = mapRowToCard(rs);
                    card.setOwnerUsername(rs.getString("username"));
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    public void updateVisibility(int cardId, String visibility) throws SQLException {
        String sql = "UPDATE cards SET visibility = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, visibility);
            ps.setInt(2, cardId);
            ps.executeUpdate();
        }
    }

    public int countAdminCards(String q, String visibility) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM cards c JOIN users u ON c.user_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (q != null && !q.trim().isEmpty()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(c.producer_name) LIKE ? OR LOWER(c.idol_name) LIKE ? OR LOWER(c.region) LIKE ? OR LOWER(c.unique_link_id) LIKE ? OR LOWER(u.username) LIKE ?)");
            params.add(like); params.add(like); params.add(like); params.add(like); params.add(like);
        }
        if (visibility != null && !visibility.trim().isEmpty() && !"all".equalsIgnoreCase(visibility)) {
            sql.append(" AND c.visibility = ?");
            params.add(visibility);
        }
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1; for (Object p : params) ps.setObject(i++, p);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        }
        return 0;
    }

    private Card mapRowToCard(ResultSet rs) throws SQLException {
        Card card = new Card();
        card.setId(rs.getInt("id"));
        card.setUserId(rs.getInt("user_id"));
        card.setProducerName(rs.getString("producer_name"));
        card.setRegion(rs.getString("region"));
        card.setIdolName(rs.getString("idol_name"));
        card.setCardFrontPath(rs.getString("card_front_path"));
        card.setCardBackPath(rs.getString("card_back_path"));
        card.setUniqueLinkId(rs.getString("unique_link_id"));
        card.setCustomSns(rs.getString("custom_sns"));
        // new fields
        try {
            card.setVisibility(rs.getString("visibility"));
        } catch (SQLException ignored) {}
        try {
            card.setShareToken(rs.getString("share_token"));
        } catch (SQLException ignored) {}
        try {
            card.setShortCode(rs.getString("short_code"));
        } catch (SQLException ignored) {}
        try {
            card.setImageOrientation(rs.getString("image_orientation"));
        } catch (SQLException ignored) {}
        return card;
    }
}