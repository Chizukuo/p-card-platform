package com.example.pcard.dao;

import com.example.pcard.model.User;
import com.example.pcard.util.DbUtil;
import com.example.pcard.util.ChineseConverter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问对象
 * 处理用户相关的数据库操作
 */
public class UserDao {

    /**
     * 添加新用户
     * @param user 用户对象
     * @throws SQLException 数据库操作异常
     */
    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, nickname, password) VALUES (?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getNickname());
            ps.setString(3, user.getPassword());
            ps.executeUpdate();
        }
    }

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户对象,不存在返回null
     * @throws SQLException 数据库操作异常
     */
    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * 根据ID获取用户
     * @param id 用户ID
     * @return 用户对象,不存在返回null
     * @throws SQLException 数据库操作异常
     */
    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * 获取所有用户列表
     * @return 用户列表
     * @throws SQLException 数据库操作异常
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    /**
     * 分页获取用户列表
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 用户列表
     * @throws SQLException 数据库操作异常
     */
    public List<User> getAllUsersPaged(int offset, int limit) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    /**
     * 搜索用户
     * 支持简繁体互相匹配
     * @param q 搜索关键词
     * @param role 角色筛选
     * @param status 状态筛选
     * @return 符合条件的用户列表
     * @throws SQLException 数据库操作异常
     */
    public List<User> searchUsers(String q, String role, String status) throws SQLException {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            q = q.trim();
            
            // 检查是否包含中文，如果包含则生成简繁体变体进行搜索
            String[] searchVariants;
            if (ChineseConverter.containsChinese(q)) {
                searchVariants = ChineseConverter.getSearchVariants(q);
            } else {
                searchVariants = new String[]{q};
            }
            
            // 构建动态搜索条件
            List<String> conditions = new ArrayList<>();
            for (String variant : searchVariants) {
                conditions.add("LOWER(username) LIKE ?");
                params.add("%" + variant.toLowerCase() + "%");
            }
            sql.append(" AND (").append(String.join(" OR ", conditions)).append(")");
        }
        
        if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY id DESC");

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    /**
     * 分页搜索用户
     * 支持简繁体互相匹配
     * @param q 搜索关键词
     * @param role 角色筛选
     * @param status 状态筛选
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 符合条件的用户列表
     * @throws SQLException 数据库操作异常
     */
    public List<User> searchUsersPaged(String q, String role, String status, int offset, int limit) throws SQLException {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            q = q.trim();
            
            // 检查是否包含中文，如果包含则生成简繁体变体进行搜索
            String[] searchVariants;
            if (ChineseConverter.containsChinese(q)) {
                searchVariants = ChineseConverter.getSearchVariants(q);
            } else {
                searchVariants = new String[]{q};
            }
            
            // 构建动态搜索条件
            List<String> conditions = new ArrayList<>();
            for (String variant : searchVariants) {
                conditions.add("LOWER(username) LIKE ?");
                params.add("%" + variant.toLowerCase() + "%");
            }
            sql.append(" AND (").append(String.join(" OR ", conditions)).append(")");
        }
        
        if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            for (Object p : params) {
                ps.setObject(i++, p);
            }
            ps.setInt(i++, limit);
            ps.setInt(i, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    /**
     * 统计符合条件的用户数量
     * 支持简繁体互相匹配
     * @param q 搜索关键词
     * @param role 角色筛选
     * @param status 状态筛选
     * @return 用户数量
     * @throws SQLException 数据库操作异常
     */
    public int countUsers(String q, String role, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            q = q.trim();
            
            // 检查是否包含中文，如果包含则生成简繁体变体进行搜索
            String[] searchVariants;
            if (ChineseConverter.containsChinese(q)) {
                searchVariants = ChineseConverter.getSearchVariants(q);
            } else {
                searchVariants = new String[]{q};
            }
            
            // 构建动态搜索条件
            List<String> conditions = new ArrayList<>();
            for (String variant : searchVariants) {
                conditions.add("LOWER(username) LIKE ?");
                params.add("%" + variant.toLowerCase() + "%");
            }
            sql.append(" AND (").append(String.join(" OR ", conditions)).append(")");
        }
        
        if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            for (Object p : params) {
                ps.setObject(i++, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * 获取管理员数量
     * @return 管理员数量
     * @throws SQLException 数据库操作异常
     */
    public int getAdminCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'admin'";
        try (Connection conn = DbUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 新状态
     * @throws SQLException 数据库操作异常
     */
    public void updateUserStatus(int userId, String status) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param newPassword 新密码(已加密)
     * @throws SQLException 数据库操作异常
     */
    public void updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 更新用户角色
     * @param userId 用户ID
     * @param role 新角色
     * @throws SQLException 数据库操作异常
     */
    public void updateUserRole(int userId, String role) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 删除用户
     * @param userId 用户ID
     * @throws SQLException 数据库操作异常
     */
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 将ResultSet行映射为User对象
     * @param rs 结果集
     * @return User对象
     * @throws SQLException 数据库操作异常
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        // 尝试获取nickname字段，如果不存在则使用username
        try {
            String nickname = rs.getString("nickname");
            user.setNickname(nickname != null ? nickname : rs.getString("username"));
        } catch (SQLException e) {
            user.setNickname(rs.getString("username"));
        }
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        return user;
    }
}