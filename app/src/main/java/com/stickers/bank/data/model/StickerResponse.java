package com.stickers.bank.data.model;

import java.util.ArrayList;

public class StickerResponse extends BaseDataResponse {

    ArrayList<StickerModel> data;

    public ArrayList<StickerModel> getData() {
        return data;
    }

    public void setData(ArrayList<StickerModel> data) {
        this.data = data;
    }
}
