package com.stickers.bank.data.model;

public class BaseDataResponse {

    boolean status_code;
    String message;

    public boolean isStatus_code() {
        return status_code;
    }

    public void setStatus_code(boolean status_code) {
        this.status_code = status_code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return status_code;
    }
}
