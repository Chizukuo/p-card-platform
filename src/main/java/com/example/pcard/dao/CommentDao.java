package com.example.pcard.dao;

import com.example.pcard.model.Comment;
import com.example.pcard.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论数据访问对象
 * 处理评论相关的数据库操作
 */
public class CommentDao {

    /**
     * 添加评论
     * @param comment 评论对象
     * @throws SQLException 数据库操作异常
     */
    public void addComment(Comment comment) throws SQLException {
        String sql = "INSERT INTO comments (card_id, user_id, username, nickname, content, parent_id, reply_to_username, reply_to_nickname) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, comment.getCardId());
            ps.setInt(2, comment.getUserId());
            ps.setString(3, comment.getUsername());
            ps.setString(4, comment.getNickname());
            ps.setString(5, comment.getContent());
            if (comment.getParentId() != null) {
                ps.setInt(6, comment.getParentId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, comment.getReplyToUsername());
            ps.setString(8, comment.getReplyToNickname());
            ps.executeUpdate();
        }
    }

    /**
     * 获取指定名片的所有评论(树形结构)
     * @param cardId 名片ID
     * @return 顶级评论列表(包含子评论)
     * @throws SQLException 数据库操作异常
     */
    public List<Comment> getCommentsByCardId(int cardId) throws SQLException {
        List<Comment> allComments = new ArrayList<>();
        String sql = "SELECT * FROM comments WHERE card_id = ? ORDER BY created_at ASC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allComments.add(mapRowToComment(rs));
                }
            }
        }

        return buildCommentTree(allComments);
    }

    /**
     * 构建评论树形结构
     * @param allComments 所有评论的扁平列表
     * @return 树形结构的顶级评论列表
     */
    private List<Comment> buildCommentTree(List<Comment> allComments) {
        Map<Integer, Comment> commentMap = new HashMap<>();
        List<Comment> rootComments = new ArrayList<>();

        // 将所有评论放入映射表
        for (Comment comment : allComments) {
            commentMap.put(comment.getId(), comment);
        }

        // 构建树形结构
        for (Comment comment : allComments) {
            if (comment.getParentId() == null) {
                rootComments.add(comment);
            } else {
                Comment parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    parent.addReply(comment);
                }
            }
        }

        return rootComments;
    }

    /**
     * 删除评论(级联删除子评论)
     * @param commentId 评论ID
     * @throws SQLException 数据库操作异常
     */
    public void deleteComment(int commentId) throws SQLException {
        String sql = "DELETE FROM comments WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.executeUpdate();
        }
    }

    /**
     * 根据ID获取评论
     * @param commentId 评论ID
     * @return 评论对象,不存在返回null
     * @throws SQLException 数据库操作异常
     */
    public Comment getCommentById(int commentId) throws SQLException {
        String sql = "SELECT * FROM comments WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToComment(rs);
                }
            }
        }
        return null;
    }

    /**
     * 将ResultSet行映射为Comment对象
     * @param rs 结果集
     * @return Comment对象
     * @throws SQLException 数据库操作异常
     */
    private Comment mapRowToComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getInt("id"));
        comment.setCardId(rs.getInt("card_id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setUsername(rs.getString("username"));
        
        // 尝试获取nickname字段，如果不存在则使用username
        try {
            String nickname = rs.getString("nickname");
            comment.setNickname(nickname != null ? nickname : rs.getString("username"));
        } catch (SQLException e) {
            comment.setNickname(rs.getString("username"));
        }
        
        comment.setContent(rs.getString("content"));

        int parentId = rs.getInt("parent_id");
        comment.setParentId(rs.wasNull() ? null : parentId);

        comment.setReplyToUsername(rs.getString("reply_to_username"));
        
        // 尝试获取reply_to_nickname字段
        try {
            String replyToNickname = rs.getString("reply_to_nickname");
            comment.setReplyToNickname(replyToNickname != null ? replyToNickname : rs.getString("reply_to_username"));
        } catch (SQLException e) {
            comment.setReplyToNickname(rs.getString("reply_to_username"));
        }
        
        comment.setCreatedAt(rs.getTimestamp("created_at"));
        comment.setUpdatedAt(rs.getTimestamp("updated_at"));
        return comment;
    }
}