package com.stickers.bank.data.model;

import java.util.ArrayList;

public class BankResponse {

    ArrayList<StickerModel> bank;

    public ArrayList<StickerModel> getData() {
        return bank;
    }

    public void setData(ArrayList<StickerModel> data) {
        this.bank = data;
    }
}
