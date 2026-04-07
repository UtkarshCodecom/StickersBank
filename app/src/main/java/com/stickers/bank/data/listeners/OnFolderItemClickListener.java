package com.stickers.bank.data.listeners;

import android.view.View;

import com.stickers.bank.data.database.entity.StickerFolder;

public interface OnFolderItemClickListener {

    public void onItemClick(View view, int position, StickerFolder stickerFolder);

    public void onItemLongClick(View view, int position);
}
