package com.stickers.bank.data.database.entity;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.stickers.bank.data.common.Constants;

import java.io.Serializable;
import java.util.UUID;


@Entity(tableName = "sticker_folder")
public class StickerFolder implements Serializable {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;

    @ColumnInfo(name = "folder_name")
    private String folderName;

    @ColumnInfo(name = "is_animated")
    private boolean is_animated;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public boolean isIs_animated() {
        return is_animated;
    }

    public void setIs_animated(boolean is_animated) {
        this.is_animated = is_animated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickerFolder stickerFolder = (StickerFolder) o;
        return id == stickerFolder.id;
    }
}