package com.example.pcard.model;

/**
 * 用户实体类
 * 表示系统中的用户账户信息
 */
public class User {
    private int id;
    private String username;
    private String nickname;
    private String password;
    private String role = "user";
    private String status = "active";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 判断当前用户是否为管理员
     * @return true 如果是管理员
     */
    public boolean isAdmin() {
        return "admin".equals(this.role);
    }
}