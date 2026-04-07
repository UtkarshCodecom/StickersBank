package com.stickers.bank.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class StickerModel implements Serializable {

    public StickerModel() {
    }

    public StickerModel(int id, int mainCategoryId, int subCategoryId, int subSubCategoryId, String stickerImage, String isActive, String createdAt, String stickerUrl, boolean isSelected, int isAnimated) {
        this.id = id;
        this.mainCategoryId = mainCategoryId;
        this.subCategoryId = subCategoryId;
        this.subSubCategoryId = subSubCategoryId;
        this.stickerImage = stickerImage;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.stickerUrl = stickerUrl;
        this.isSelected = isSelected;
        this.isAnimated = isAnimated;
    }

    @SerializedName("id")
    @Expose
    private int id;
    @SerializedName("main_category_id")
    @Expose
    private int mainCategoryId;
    @SerializedName("sub_category_id")
    @Expose
    private int subCategoryId;
    @SerializedName("sub_sub_category_id")
    @Expose
    private int subSubCategoryId;
    @SerializedName("sticker_image")
    @Expose
    private String stickerImage;
    @SerializedName("is_active")
    @Expose
    private String isActive;
    @SerializedName("created_at")
    @Expose
    private String createdAt;
    @SerializedName("sticker_url")
    @Expose
    private String stickerUrl;
    @SerializedName("is_animated")
    @Expose
    private int isAnimated;

    private boolean isSelected;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMainCategoryId() {
        return mainCategoryId;
    }

    public void setMainCategoryId(int mainCategoryId) {
        this.mainCategoryId = mainCategoryId;
    }

    public int getSubCategoryId() {
        return subCategoryId;
    }

    public void setSubCategoryId(int subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public int getSubSubCategoryId() {
        return subSubCategoryId;
    }

    public void setSubSubCategoryId(int subSubCategoryId) {
        this.subSubCategoryId = subSubCategoryId;
    }

    public String getStickerImage() {
        return stickerImage;
    }

    public void setStickerImage(String stickerImage) {
        this.stickerImage = stickerImage;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStickerUrl() {
        return stickerUrl;
    }

    public void setStickerUrl(String stickerUrl) {
        this.stickerUrl = stickerUrl;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getIsAnimated() {
        return isAnimated;
    }

    public void setIsAnimated(int isAnimated) {
        this.isAnimated = isAnimated;
    }
}
