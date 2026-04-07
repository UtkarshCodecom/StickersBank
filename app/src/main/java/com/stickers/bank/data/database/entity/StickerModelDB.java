package com.stickers.bank.data.database.entity;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.stickers.bank.data.common.Constants;
import com.stickers.bank.data.model.StickerModel;

import java.io.Serializable;


@Entity(tableName = Constants.TABLE_NAME)
public class StickerModelDB implements Serializable {

    @PrimaryKey
    @NonNull
    private int id;

    @NonNull
    @ColumnInfo(name = "folder_id")
    private int folderId;

    @ColumnInfo(name = "main_category_id")
    private int mainCategoryId;

    @ColumnInfo(name = "sub_category_id")
    private int subCategoryId;

    @ColumnInfo(name = "sub_sub_category_id")
    private int subSubCategoryId;

    @ColumnInfo(name = "sticker_image")
    private String stickerImage;

    @NonNull
    @ColumnInfo(name = "is_active")
    private String isActive;

    @NonNull
    @ColumnInfo(name = "created_at")
    private String createdAt;

    @NonNull
    @ColumnInfo(name = "sticker_url")
    private String stickerUrl;

    @NonNull
    @ColumnInfo(name = "is_animated")
    private int is_animated;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
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

    @NonNull
    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(@NonNull String isActive) {
        this.isActive = isActive;
    }

    @NonNull
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull String createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public String getStickerUrl() {
        return stickerUrl;
    }

    public void setStickerUrl(@NonNull String stickerUrl) {
        this.stickerUrl = stickerUrl;
    }

    public int getIs_animated() {
        return is_animated;
    }

    public void setIs_animated(int is_animated) {
        this.is_animated = is_animated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickerModelDB stickerModel = (StickerModelDB) o;
        return id == stickerModel.id;
    }
}