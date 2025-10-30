package com.example.pcard.model;

import com.example.pcard.util.TimeZoneUtil;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论实体类
 * 表示用户对名片的评论信息,支持树形结构回复
 */
public class Comment {
    private int id;
    private int cardId;
    private int userId;
    private String username;
    private String nickname;
    private String content;
    private Integer parentId;
    private String replyToUsername;
    private String replyToNickname;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<Comment> replies = new ArrayList<>();
    
    // 管理员视图使用的额外字段
    private String ownerUsername;  // 名片所有者用户名
    private String cardTitle;      // 名片标题(P名)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getReplyToUsername() {
        return replyToUsername;
    }

    public void setReplyToUsername(String replyToUsername) {
        this.replyToUsername = replyToUsername;
    }

    public String getReplyToNickname() {
        return replyToNickname;
    }

    public void setReplyToNickname(String replyToNickname) {
        this.replyToNickname = replyToNickname;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    public void addReply(Comment reply) {
        this.replies.add(reply);
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getCardTitle() {
        return cardTitle;
    }

    public void setCardTitle(String cardTitle) {
        this.cardTitle = cardTitle;
    }

    /**
     * 获取 UTC 时间戳（毫秒），供前端使用
     * @return UTC 时间戳
     */
    public long getTimestamp() {
        return createdAt != null ? createdAt.getTime() : 0;
    }

    /**
     * 获取格式化的评论时间
     * 根据时间间隔返回相对时间或绝对时间
     * 使用统一的应用时区配置
     * @return 格式化后的时间字符串
     */
    public String getFormattedTime() {
        if (createdAt == null) {
            return "";
        }

        try {
            // 使用应用配置的统一时区
            LocalDateTime commentTime = createdAt.toInstant()
                    .atZone(TimeZoneUtil.getApplicationZone())
                    .toLocalDateTime();
            LocalDateTime now = LocalDateTime.now(TimeZoneUtil.getApplicationZone());

            // 计算时间差（确保是正数）
            long minutes = java.time.Duration.between(commentTime, now).toMinutes();
            
            // 如果时间差为负数（未来时间），说明存在时区问题，直接显示绝对时间
            if (minutes < 0) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                return commentTime.format(formatter);
            }
            
            long hours = minutes / 60;
            long days = hours / 24;

            if (minutes < 1) return "刚刚";
            if (minutes < 60) return minutes + "分钟前";
            if (hours < 24) return hours + "小时前";
            if (days < 7) return days + "天前";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return commentTime.format(formatter);
        } catch (Exception e) {
            // 如果出现任何异常，返回默认格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return createdAt.toLocalDateTime().format(formatter);
        }
    }
}