package com.stickers.bank.data.webservices;


import com.stickers.bank.data.model.FeaturedResponse;
import com.stickers.bank.data.model.NewArrivalResponse;
import com.stickers.bank.data.model.StickerResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface APIInterface {

    @GET("get-category")
    Call<FeaturedResponse> getFeatured();

    @FormUrlEncoded
    @POST("get-sub-category")
    Call<FeaturedResponse> getSubCategory(@FieldMap Map<String, Object> params);

    @FormUrlEncoded
    @POST("get-sub-sub-category")
    Call<FeaturedResponse> getSubSubCategory(@FieldMap Map<String, Object> params);

    @FormUrlEncoded
    @POST("get-stickers")
    Call<StickerResponse> getStickers(@FieldMap Map<String, Object> params);

    @GET("get-new-arrival-stickers")
    Call<NewArrivalResponse> getNewArrivalStickers();

    @FormUrlEncoded
    @POST("device-token-update")
    Call<StickerResponse> postFCMToken(@FieldMap Map<String, Object> params);
}


