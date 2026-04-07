/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.stickers.bank.data.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Objects;

public class StickerApi implements Parcelable {

    public String image_file;
    public String image_file_name;
    public List<String> emojis;
    Uri uri;
    public long size;

    public StickerApi() {
    }

    public StickerApi(String imageFileName, List<String> emojis) {
        this.image_file = imageFileName;
        this.emojis = emojis;
    }

    protected StickerApi(Parcel in) {
        uri = Uri.parse(in.readString());
        image_file = in.readString();
        image_file_name = in.readString();
        emojis = in.createStringArrayList();
        size = in.readLong();
    }

    public static final Creator<StickerApi> CREATOR = new Creator<StickerApi>() {
        @Override
        public StickerApi createFromParcel(Parcel in) {
            return new StickerApi(in);
        }

        @Override
        public StickerApi[] newArray(int size) {
            return new StickerApi[size];
        }
    };

    public void setSize(long size) {
        this.size = size;
    }

    public String getImage_file() {
        return image_file;
    }

    public void setImage_file(String image_file) {
        this.image_file = image_file;
    }

    public List<String> getEmojis() {
        return emojis;
    }

    public void setEmojis(List<String> emojis) {
        this.emojis = emojis;
    }

    public long getSize() {
        return size;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        try {
            this.uri = uri;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getImage_file_name() {
        return image_file_name;
    }

    public void setImage_file_name(String image_file_name) {
        this.image_file_name = image_file_name;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uri.toString());
        dest.writeString(image_file);
        dest.writeString(image_file_name);
        dest.writeStringList(emojis);
        dest.writeLong(size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickerApi that = (StickerApi) o;
        return image_file_name.equals(that.image_file_name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image_file_name);
    }
}
