package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
import com.example.pcard.model.Card;
import com.example.pcard.model.User;
import com.example.pcard.util.ChineseConverter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
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
                cards = cardDao.searchPublicCardsPaged(q.trim(), offset, limit * 2); // 获取更多数据用于排序
                request.setAttribute("query", q.trim());
            } else {
                cards = cardDao.getPublicCardsPaged(offset, limit * 2); // 获取更多数据用于排序
            }

            // 获取用户偏好地区并进行智能排序
            String preferredRegion = getUserPreferredRegion(request);
            if (preferredRegion != null && !preferredRegion.isEmpty()) {
                cards = sortCardsByRegionPreference(cards, preferredRegion);
                request.setAttribute("preferredRegion", preferredRegion);
            }
            
            // 截取所需数量
            if (cards.size() > limit) {
                cards = cards.subList(0, limit);
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
    
    /**
     * 获取用户偏好的地区
     * 优先级：1. 用户名片地区 2. IP地址地区
     */
    private String getUserPreferredRegion(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            try {
                // 获取用户创建的名片，从中提取地区信息
                List<Card> userCards = cardDao.getCardsByUserId(user.getId());
                if (userCards != null && !userCards.isEmpty()) {
                    // 使用用户最新创建的名片的地区
                    String region = userCards.get(0).getRegion();
                    if (region != null && !region.trim().isEmpty()) {
                        return region.trim();
                    }
                }
            } catch (SQLException e) {
                // 静默失败，继续尝试其他方法
            }
        }
        
        // 如果用户未登录或没有名片，尝试从IP获取地区
        return getRegionFromIP(request);
    }
    
    /**
     * 根据IP地址获取地区
     * 简化实现：从请求头中提取可能的地区信息
     */
    private String getRegionFromIP(HttpServletRequest request) {
        // 从Cloudflare或其他CDN的请求头中获取地区信息
        String cfRegion = request.getHeader("CF-IPCountry");
        if (cfRegion != null && !cfRegion.isEmpty()) {
            return mapCountryCodeToRegion(cfRegion);
        }
        
        // 尝试从其他可能的请求头获取城市信息
        String cfCity = request.getHeader("CF-IPCity");
        if (cfCity != null && !cfCity.isEmpty()) {
            return cfCity;
        }
        
        // 从X-Forwarded-For获取真实IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        
        // 简单的IP地区判断 - 可以后续集成IP地理位置库
        // 如果是局域网IP，尝试从其他来源推断
        if (isPrivateIP(ip)) {
            // 对于本地开发，可以通过其他方式获取
            return null;
        }
        
        return null;
    }
    
    /**
     * 判断是否为私有IP地址
     */
    private boolean isPrivateIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        
        return ip.startsWith("127.") || 
               ip.startsWith("192.168.") || 
               ip.startsWith("10.") ||
               ip.equals("::1") ||
               ip.equals("0:0:0:0:0:0:0:1");
    }
    
    /**
     * 将国家代码映射到中文地区名称
     */
    private String mapCountryCodeToRegion(String countryCode) {
        if (countryCode == null) return null;
        
        switch (countryCode.toUpperCase()) {
            case "CN": return "大陆";
            case "HK": return "香港";
            case "TW": return "台湾";
            case "MO": return "澳门";
            case "JP": return "日本";
            case "KR": return "韩国";
            case "US": return "美国";
            case "GB": return "英国";
            case "CA": return "加拿大";
            case "AU": return "澳大利亚";
            case "SG": return "新加坡";
            case "MY": return "马来西亚";
            case "TH": return "泰国";
            default: return null;
        }
    }
    
    /**
     * 根据地区偏好对名片进行排序
     * 相同地区的名片排在前面，支持精确匹配和模糊匹配
     */
    private List<Card> sortCardsByRegionPreference(List<Card> cards, String preferredRegion) {
        if (cards == null || cards.isEmpty() || preferredRegion == null) {
            return cards;
        }
        
        List<Card> result = new ArrayList<>(cards);
        result.sort(new Comparator<Card>() {
            @Override
            public int compare(Card c1, Card c2) {
                String r1 = c1.getRegion();
                String r2 = c2.getRegion();
                
                // 计算匹配度：2=精确匹配，1=同国家/地区，0=不匹配
                int score1 = getMatchScore(r1, preferredRegion);
                int score2 = getMatchScore(r2, preferredRegion);
                
                // 分数高的排前面
                return Integer.compare(score2, score1);
            }
        });
        
        return result;
    }
    
    /**
     * 计算地区匹配分数（支持简繁体）
     * 2 = 精确匹配（完全相同或互相包含）
     * 1 = 同国家/地区（如都是中国城市）
     * 0 = 不匹配
     */
    private int getMatchScore(String cardRegion, String preferredRegion) {
        if (cardRegion == null || preferredRegion == null) {
            return 0;
        }
        
        // 获取简繁体变体
        String[] crVariants = getRegionVariants(cardRegion);
        String[] prVariants = getRegionVariants(preferredRegion);
        
        // 检查精确匹配（包括简繁体变体）
        for (String crVar : crVariants) {
            for (String prVar : prVariants) {
                // 完全匹配或包含匹配
                if (crVar.equals(prVar) || crVar.contains(prVar) || prVar.contains(crVar)) {
                    return 2;
                }
            }
        }
        
        // 同国家/地区匹配
        if (isSameCountry(cardRegion, preferredRegion)) {
            return 1;
        }
        
        return 0;
    }
    
    /**
     * 判断地区是否匹配（支持模糊匹配、层级匹配和简繁体匹配）
     */
    private boolean matchesRegion(String cardRegion, String preferredRegion) {
        if (cardRegion == null || preferredRegion == null) {
            return false;
        }
        
        // 获取简繁体变体
        String[] crVariants = getRegionVariants(cardRegion);
        String[] prVariants = getRegionVariants(preferredRegion);
        
        // 对所有变体进行匹配
        for (String crVar : crVariants) {
            for (String prVar : prVariants) {
                // 完全匹配
                if (crVar.equals(prVar)) {
                    return true;
                }
                
                // 包含匹配
                if (crVar.contains(prVar) || prVar.contains(crVar)) {
                    return true;
                }
            }
        }
        
        // 同国家/地区匹配（次优先级）
        // 例如：武汉 vs 大陆，都属于中国
        if (isSameCountry(cardRegion, preferredRegion)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取地区的简繁体变体（用于匹配）
     * 返回统一为小写的变体数组
     */
    private String[] getRegionVariants(String region) {
        if (region == null || region.isEmpty()) {
            return new String[]{""};
        }
        
        String trimmed = region.trim();
        
        // 如果包含中文，生成简繁体变体
        if (ChineseConverter.containsChinese(trimmed)) {
            List<String> variantsList = ChineseConverter.getSearchVariants(trimmed);
            // 转换为小写（保持中文不变，只转换可能的英文）
            String[] result = new String[variantsList.size()];
            for (int i = 0; i < variantsList.size(); i++) {
                result[i] = variantsList.get(i).toLowerCase();
            }
            return result;
        } else {
            // 非中文，只返回小写版本
            return new String[]{trimmed.toLowerCase()};
        }
    }
    
    /**
     * 判断两个地区是否属于同一国家/大区域（支持简繁体）
     */
    private boolean isSameCountry(String region1, String region2) {
        // 获取简繁体变体用于匹配
        String[] r1Variants = getRegionVariants(region1);
        String[] r2Variants = getRegionVariants(region2);
        
        // 中国大陆城市列表（简体）
        String[] chinaCities = {"北京", "上海", "广州", "深圳", "武汉", "成都", "重庆", 
                                "杭州", "南京", "天津", "西安", "郑州", "长沙", "济南", 
                                "青岛", "大连", "沈阳", "哈尔滨", "福州", "厦门", "南昌",
                                "合肥", "石家庄", "太原", "兰州", "西宁", "乌鲁木齐", 
                                "昆明", "贵阳", "南宁", "海口", "拉萨", "呼和浩特", "银川",
                                "长春", "苏州", "宁波", "无锡", "佛山", "东莞", "珠海",
                                "中山", "惠州", "汕头", "温州", "泉州", "常州", "徐州"};
        
        // 检查是否为中国（支持简繁体）
        boolean r1IsChina = false;
        boolean r2IsChina = false;
        
        for (String r1 : r1Variants) {
            if (isInArray(r1, chinaCities) || 
                r1.contains("大陆") || r1.contains("大陸") || 
                r1.contains("中国") || r1.contains("中國") || 
                r1.contains("cn") || r1.contains("china")) {
                r1IsChina = true;
                break;
            }
        }
        
        for (String r2 : r2Variants) {
            if (isInArray(r2, chinaCities) || 
                r2.contains("大陆") || r2.contains("大陸") || 
                r2.contains("中国") || r2.contains("中國") || 
                r2.contains("cn") || r2.contains("china")) {
                r2IsChina = true;
                break;
            }
        }
        
        // 如果两者都是中国，返回true（弱匹配）
        if (r1IsChina && r2IsChina) {
            return true;
        }
        
        // 港澳台匹配（支持简繁体）
        boolean r1IsHK = containsAnyVariant(r1Variants, new String[]{"香港", "hk", "hong kong"});
        boolean r2IsHK = containsAnyVariant(r2Variants, new String[]{"香港", "hk", "hong kong"});
        if (r1IsHK && r2IsHK) return true;
        
        boolean r1IsTW = containsAnyVariant(r1Variants, new String[]{"台湾", "臺灣", "台灣", "tw", "taiwan"});
        boolean r2IsTW = containsAnyVariant(r2Variants, new String[]{"台湾", "臺灣", "台灣", "tw", "taiwan"});
        if (r1IsTW && r2IsTW) return true;
        
        boolean r1IsMO = containsAnyVariant(r1Variants, new String[]{"澳门", "澳門", "mo", "macao", "macau"});
        boolean r2IsMO = containsAnyVariant(r2Variants, new String[]{"澳门", "澳門", "mo", "macao", "macau"});
        if (r1IsMO && r2IsMO) return true;
        
        return false;
    }
    
    /**
     * 检查变体数组中是否包含任何关键词
     */
    private boolean containsAnyVariant(String[] variants, String[] keywords) {
        for (String variant : variants) {
            for (String keyword : keywords) {
                if (variant.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 判断字符串是否在数组中（不区分大小写）
     */
    private boolean isInArray(String str, String[] array) {
        for (String item : array) {
            if (str.contains(item.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private int parseIntOrDefault(String s, int d) {
        try { return Integer.parseInt(s); } catch (Exception e) { return d; }
    }
}