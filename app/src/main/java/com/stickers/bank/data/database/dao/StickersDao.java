package com.stickers.bank.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.stickers.bank.data.common.Constants;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.database.entity.StickerModelDB;

import java.util.List;


@Dao
public interface StickersDao {

    @Query("SELECT * FROM " + Constants.TABLE_NAME)
    List<StickerModelDB> getAllStickers();

    @Query("SELECT * FROM sticker_folder")
    List<StickerFolder> getAllFolders();

    @Query("SELECT * FROM " + Constants.TABLE_NAME + " WHERE folder_id =:folder_id")
    List<StickerModelDB> getStickerByID(int folder_id);

    @Update
    void updateProject(StickerModelDB storages);

    @Query("SELECT COUNT(*) from stickers")
    int countKathaData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StickerModelDB stickerModel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFolder(StickerFolder stickerFolder);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<StickerModelDB> stickerModelList);

    @Query("DELETE FROM stickers WHERE folder_id = :id")
    abstract void deleteStickerByFolderId(int id);

    @Query("DELETE FROM sticker_folder WHERE id = :id")
    abstract void deleteFolderById(int id);
}