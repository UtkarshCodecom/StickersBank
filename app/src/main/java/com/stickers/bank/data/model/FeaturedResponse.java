package com.stickers.bank.data.model;

import java.util.ArrayList;

public class FeaturedResponse extends BaseDataResponse {

    ArrayList<FeaturedModel> data;

    public ArrayList<FeaturedModel> getData() {
        return data;
    }

    public void setData(ArrayList<FeaturedModel> data) {
        this.data = data;
    }
}
