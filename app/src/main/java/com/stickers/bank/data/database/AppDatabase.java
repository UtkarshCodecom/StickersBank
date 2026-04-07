package com.stickers.bank.data.database;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.stickers.bank.data.database.dao.StickersDao;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.database.entity.StickerModelDB;

@Database(entities = {StickerModelDB.class, StickerFolder.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract StickersDao getStickerDao();
}