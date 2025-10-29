package com.example.pcard.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * 电子名片实体类
 * 表示用户创建的电子名片信息
 */
public class Card {
    private int id;
    private int userId;
    private String visibility;
    private String shareToken;
    private String producerName;
    private String region;
    private String idolName;
    private String cardFrontPath;
    private String cardBackPath;
    private String imageOrientation;
    private String uniqueLinkId;
    private String shortCode;
    private String customSns;
    private String ownerUsername;
    private transient List<SnsLink> snsLinks;

    /**
     * 社交媒体链接内部类
     */
    public static class SnsLink {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getProducerName() {
        return producerName;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getIdolName() {
        return idolName;
    }

    public void setIdolName(String idolName) {
        this.idolName = idolName;
    }

    public String getCardFrontPath() {
        return cardFrontPath;
    }

    public void setCardFrontPath(String cardFrontPath) {
        this.cardFrontPath = cardFrontPath;
    }

    public String getCardBackPath() {
        return cardBackPath;
    }

    public void setCardBackPath(String cardBackPath) {
        this.cardBackPath = cardBackPath;
    }

    public String getImageOrientation() {
        return imageOrientation;
    }

    public void setImageOrientation(String imageOrientation) {
        this.imageOrientation = imageOrientation;
    }

    public String getUniqueLinkId() {
        return uniqueLinkId;
    }

    public void setUniqueLinkId(String uniqueLinkId) {
        this.uniqueLinkId = uniqueLinkId;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getCustomSns() {
        return customSns;
    }

    public void setCustomSns(String customSns) {
        this.customSns = customSns;
        this.snsLinks = null;
    }

    public String getVisibility() {
        return visibility == null ? "PUBLIC" : visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    /**
     * 获取解析后的社交媒体链接列表
     * @return SNS链接列表
     */
    public List<SnsLink> getSnsLinks() {
        if (snsLinks == null) {
            if (customSns != null && !customSns.isEmpty()) {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<SnsLink>>() {}.getType();
                    snsLinks = gson.fromJson(customSns, type);
                } catch (Exception e) {
                    snsLinks = Collections.emptyList();
                }
            } else {
                snsLinks = Collections.emptyList();
            }
        }
        return snsLinks;
    }
}