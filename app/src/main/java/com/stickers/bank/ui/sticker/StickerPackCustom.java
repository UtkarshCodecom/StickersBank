/*
 * Copyright 2020  Vimal CVS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stickers.bank.ui.sticker;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class StickerPackCustom implements Parcelable {

    public String identifier;
    public String name;
    public String publisher;
    public String trayImageFile;
    public String trayImageUrl;
    public String size;
    public String downloads;
    public String premium;
    public String review = "false";
    public String trused;
    public String created;
    public String username;
    public String userimage;
    public String userid;
    public final String publisherEmail;
    public final String publisherWebsite;
    public final String privacyPolicyWebsite;
    public final String licenseAgreementWebsite;
    public boolean animatedStickerPack;

    public String iosAppStoreLink;
    private List<StickerCustom> stickers;
    private long totalSize;
    public String androidPlayStoreLink;
    private boolean isWhitelisted;
    private int viewType = 1;

    public StickerPackCustom() {
        this.identifier = "";
        this.name = "";
        this.publisher = "";
        this.trayImageFile = "";
        this.publisherEmail = "";
        this.publisherWebsite = "";
        this.privacyPolicyWebsite = "";
        this.licenseAgreementWebsite = "";
    }

    public StickerPackCustom(String identifier, String name, String publisher, String trayImageFile, String publisherEmail, String publisherWebsite, String privacyPolicyWebsite, String licenseAgreementWebsite, boolean animatedStickerPack) {
        this.identifier = identifier;
        this.name = name;
        this.publisher = publisher;
        this.trayImageFile = trayImageFile;
        this.publisherEmail = publisherEmail;
        this.publisherWebsite = publisherWebsite;
        this.privacyPolicyWebsite = privacyPolicyWebsite;
        this.licenseAgreementWebsite = licenseAgreementWebsite;
        this.animatedStickerPack = animatedStickerPack;
    }

    public StickerPackCustom(
            String identifier,
            String name,
            String publisher,
            String trayImageFile,
            String trayImageUrl,
            String size,
            String downloads,
            String premium,
            String trused,
            String created,
            String username,
            String userimage,
            String userid,
            String publisherEmail,
            String publisherWebsite,
            String privacyPolicyWebsite,
            String licenseAgreementWebsite,
            boolean animatedStickerPack
    ) {
        this.identifier = identifier;
        this.name = name;
        this.publisher = publisher;
        this.trayImageFile = trayImageFile;
        this.trayImageUrl = trayImageUrl;
        this.size = size;
        this.downloads = downloads;
        this.premium = premium;
        this.trused = trused;
        this.created = created;
        this.username = username;
        this.userimage = userimage;
        this.userid = userid;
        this.publisherEmail = publisherEmail;
        this.publisherWebsite = publisherWebsite;
        this.privacyPolicyWebsite = privacyPolicyWebsite;
        this.licenseAgreementWebsite = licenseAgreementWebsite;
        this.animatedStickerPack = animatedStickerPack;
    }

    public void setIsWhitelisted(boolean isWhitelisted) {
        this.isWhitelisted = isWhitelisted;
    }

    public boolean getIsWhitelisted() {
        return isWhitelisted;
    }

    protected StickerPackCustom(Parcel in) {
        identifier = in.readString();
        name = in.readString();
        publisher = in.readString();
        trayImageFile = in.readString();
        trayImageUrl = in.readString();
        size = in.readString();
        downloads = in.readString();
        premium = in.readString();
        review = in.readString();
        trused = in.readString();
        created = in.readString();
        username = in.readString();
        userimage = in.readString();
        userid = in.readString();
        publisherEmail = in.readString();
        publisherWebsite = in.readString();
        privacyPolicyWebsite = in.readString();
        licenseAgreementWebsite = in.readString();
        iosAppStoreLink = in.readString();
        stickers = in.createTypedArrayList(StickerCustom.CREATOR);
        totalSize = in.readLong();
        androidPlayStoreLink = in.readString();
        isWhitelisted = in.readByte() != 0;
        animatedStickerPack = in.readByte() != 0;
    }

    public static final Creator<StickerPackCustom> CREATOR = new Creator<StickerPackCustom>() {
        @Override
        public StickerPackCustom createFromParcel(Parcel in) {
            return new StickerPackCustom(in);
        }

        @Override
        public StickerPackCustom[] newArray(int size) {
            return new StickerPackCustom[size];
        }
    };

    public void setStickers(List<StickerCustom> stickers) {
        this.stickers = stickers;
        totalSize = 0;
        for (StickerCustom sticker : stickers) {
            totalSize += sticker.size;
        }
    }

    public void setAndroidPlayStoreLink(String androidPlayStoreLink) {
        this.androidPlayStoreLink = androidPlayStoreLink;
    }

    public void setIosAppStoreLink(String iosAppStoreLink) {
        this.iosAppStoreLink = iosAppStoreLink;
    }

    public List<StickerCustom> getStickers() {
        return stickers;
    }

    public long getTotalSize() {
        return totalSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(identifier);
        dest.writeString(name);
        dest.writeString(publisher);
        dest.writeString(trayImageFile);
        dest.writeString(trayImageUrl);
        dest.writeString(size);
        dest.writeString(downloads);
        dest.writeString(premium);
        dest.writeString(review);
        dest.writeString(trused);
        dest.writeString(created);
        dest.writeString(username);
        dest.writeString(userimage);
        dest.writeString(userid);

        dest.writeString(publisherEmail);
        dest.writeString(publisherWebsite);
        dest.writeString(privacyPolicyWebsite);
        dest.writeString(licenseAgreementWebsite);
        dest.writeString(iosAppStoreLink);
        dest.writeTypedList(stickers);
        dest.writeLong(totalSize);
        dest.writeString(androidPlayStoreLink);
        dest.writeByte((byte) (isWhitelisted ? 1 : 0));
        dest.writeByte((byte) (animatedStickerPack ? 1 : 0));
    }

    public int getViewType() {
        return viewType;
    }

    public StickerPackCustom setViewType(int viewType) {
        this.viewType = viewType;
        return this;
    }
}
