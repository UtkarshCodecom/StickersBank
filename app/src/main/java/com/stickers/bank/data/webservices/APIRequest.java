package com.stickers.bank.data.webservices;


import com.stickers.bank.BuildConfig;
import com.stickers.bank.data.model.FeaturedResponse;
import com.stickers.bank.data.model.NewArrivalResponse;
import com.stickers.bank.data.model.StickerResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIRequest {

    //private String baseURL = "http://stickerapp.kanadtech.com/api/";
    private String baseURL = "https://www.stickersbank.in/api/";

    private Retrofit retrofit;
    private APIInterface apiInterface;

    public APIRequest(final String token) {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();

        if (BuildConfig.DEBUG)
            builder.addInterceptor(logging);

        builder.readTimeout(60, TimeUnit.SECONDS);
        builder.connectTimeout(60, TimeUnit.SECONDS);
        builder.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request;
                if (token != null) {
                    //request = chain.request().newBuilder().addHeader("key", "d494f494da133429b93abe139d324bfb").addHeader("token", token).build();
                    request = chain.request().newBuilder().build();
                } else {
                    //request = chain.request().newBuilder().addHeader("key", "d494f494da133429b93abe139d324bfb").build();
                    request = chain.request().newBuilder().build();
                }

                return chain.proceed(request);
            }
        });

        OkHttpClient httpClient = builder.build();
        retrofit = new Retrofit.Builder().baseUrl(baseURL).addConverterFactory(GsonConverterFactory.create())
                //.addConverterFactory(ScalarsConverterFactory.create())
                .client(httpClient).build();

        apiInterface = retrofit.create(APIInterface.class);
    }

    public void getFeatured(final ResponseCallback callback) {
        try {
            Call<FeaturedResponse> requestCall = apiInterface.getFeatured();
            requestCall.enqueue(new Callback<FeaturedResponse>() {
                @Override
                public void onResponse(Call<FeaturedResponse> call, Response<FeaturedResponse> response) {
                    if (response.isSuccessful()) {
                        callback.ResponseSuccessCallBack(response.body());
                    } else {
                        try {
                            String s = response.errorBody().string().toString().trim();
                            callback.onResponseFail(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<FeaturedResponse> call, Throwable t) {
                    callback.ResponseFailCallBack(t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getSubCategory(Map<String, Object> params, final ResponseCallback callback) {
        try {
            Call<FeaturedResponse> requestCall = apiInterface.getSubCategory(params);
            requestCall.enqueue(new Callback<FeaturedResponse>() {
                @Override
                public void onResponse(Call<FeaturedResponse> call, Response<FeaturedResponse> response) {
                    if (response.isSuccessful()) {
                        callback.ResponseSuccessCallBack(response.body());
                    } else {
                        try {
                            String s = response.errorBody().string().toString().trim();
                            callback.onResponseFail(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<FeaturedResponse> call, Throwable t) {
                    callback.ResponseFailCallBack(t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getSubSubCategory(Map<String, Object> params, final ResponseCallback callback) {
        try {
            Call<FeaturedResponse> requestCall = apiInterface.getSubSubCategory(params);
            requestCall.enqueue(new Callback<FeaturedResponse>() {
                @Override
                public void onResponse(Call<FeaturedResponse> call, Response<FeaturedResponse> response) {
                    if (response.isSuccessful()) {
                        callback.ResponseSuccessCallBack(response.body());
                    } else {
                        try {
                            String s = response.errorBody().string().toString().trim();
                            callback.onResponseFail(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<FeaturedResponse> call, Throwable t) {
                    callback.ResponseFailCallBack(t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getStickers(Map<String, Object> params, final ResponseCallback callback) {
        try {
            Call<StickerResponse> requestCall = apiInterface.getStickers(params);
            requestCall.enqueue(new Callback<StickerResponse>() {
                @Override
                public void onResponse(Call<StickerResponse> call, Response<StickerResponse> response) {
                    if (response.isSuccessful()) {
                        callback.ResponseSuccessCallBack(response.body());
                    } else {
                        try {
                            String s = response.errorBody().string().toString().trim();
                            callback.onResponseFail(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<StickerResponse> call, Throwable t) {
                    callback.ResponseFailCallBack(t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getNewArrivalStickers(final ResponseCallback callback) {
        try {
            Call<NewArrivalResponse> requestCall = apiInterface.getNewArrivalStickers();
            requestCall.enqueue(new Callback<NewArrivalResponse>() {
                @Override
                public void onResponse(Call<NewArrivalResponse> call, Response<NewArrivalResponse> response) {
                    if (response.isSuccessful()) {
                        callback.ResponseSuccessCallBack(response.body());
                    } else {
                        try {
                            String s = response.errorBody().string().toString().trim();
                            callback.onResponseFail(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<NewArrivalResponse> call, Throwable t) {
                    callback.ResponseFailCallBack(t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendToken(Map<String, Object> params, final ResponseCallback callback) {
        try {
            Call<StickerResponse> requestCall = apiInterface.postFCMToken(params);
            requestCall.enqueue(new Callback<StickerResponse>() {
                @Override
                public void onResponse(Call<StickerResponse> call, Response<StickerResponse> response) {
                    if (response.isSuccessful()) {
                        callback.ResponseSuccessCallBack(response.body());
                    } else {
                        try {
                            String s = response.errorBody().string().toString().trim();
                            callback.onResponseFail(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<StickerResponse> call, Throwable t) {
                    callback.ResponseFailCallBack(t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
