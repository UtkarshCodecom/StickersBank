package com.stickers.bank.data.webservices;

public interface ResponseCallback {

    public void ResponseSuccessCallBack(Object object);

    public void ResponseFailCallBack(String message);

    public void onResponseFail(String msg);
}
