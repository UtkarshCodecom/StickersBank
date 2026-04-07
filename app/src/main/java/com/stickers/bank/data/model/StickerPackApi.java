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

public class StickerPackApi implements Parcelable {

    public String identifier;
    public String name;
    public String publisher;
    public String trayImageFile;
    public String publisherEmail;
    public String publisherWebsite;
    public String privacyPolicyWebsite;
    public String licenseAgreementWebsite;
    public String imageDataVersion;
    public boolean avoidCache;
    public boolean animatedStickerPack;

    public String iosAppStoreLink;
    private List<StickerApi> stickers;
    private long totalSize;
    public String androidPlayStoreLink;
    private boolean isWhitelisted;

    public Uri trayImageUri;
    public String tray_image_file;
    public String tray_image_file_name;

    public StickerPackApi() {
    }

    StickerPackApi(String identifier, String name, String publisher, String trayImageFile, String publisherEmail, String publisherWebsite, String privacyPolicyWebsite, String licenseAgreementWebsite, String imageDataVersion, boolean avoidCache, boolean animatedStickerPack) {
        this.identifier = identifier;
        this.name = name;
        this.publisher = publisher;
        this.trayImageFile = trayImageFile;
        this.publisherEmail = publisherEmail;
        this.publisherWebsite = publisherWebsite;
        this.privacyPolicyWebsite = privacyPolicyWebsite;
        this.licenseAgreementWebsite = licenseAgreementWebsite;
        this.imageDataVersion = imageDataVersion;
        this.avoidCache = avoidCache;
        this.animatedStickerPack = animatedStickerPack;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getTrayImageFile() {
        return trayImageFile;
    }

    public void setTrayImageFile(String trayImageFile) {
        this.trayImageFile = trayImageFile;
    }

    public String getPublisherEmail() {
        return publisherEmail;
    }

    public void setPublisherEmail(String publisherEmail) {
        this.publisherEmail = publisherEmail;
    }

    public String getPublisherWebsite() {
        return publisherWebsite;
    }

    public void setPublisherWebsite(String publisherWebsite) {
        this.publisherWebsite = publisherWebsite;
    }

    public String getPrivacyPolicyWebsite() {
        return privacyPolicyWebsite;
    }

    public void setPrivacyPolicyWebsite(String privacyPolicyWebsite) {
        this.privacyPolicyWebsite = privacyPolicyWebsite;
    }

    public String getLicenseAgreementWebsite() {
        return licenseAgreementWebsite;
    }

    public void setLicenseAgreementWebsite(String licenseAgreementWebsite) {
        this.licenseAgreementWebsite = licenseAgreementWebsite;
    }

    public String getImageDataVersion() {
        return imageDataVersion;
    }

    public void setImageDataVersion(String imageDataVersion) {
        this.imageDataVersion = imageDataVersion;
    }

    public boolean isAvoidCache() {
        return avoidCache;
    }

    public void setAvoidCache(boolean avoidCache) {
        this.avoidCache = avoidCache;
    }

    public boolean isAnimatedStickerPack() {
        return animatedStickerPack;
    }

    public void setAnimatedStickerPack(boolean animatedStickerPack) {
        this.animatedStickerPack = animatedStickerPack;
    }

    public String getIosAppStoreLink() {
        return iosAppStoreLink;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }


    public String getAndroidPlayStoreLink() {
        return androidPlayStoreLink;
    }

    public void setIsWhitelisted(boolean isWhitelisted) {
        this.isWhitelisted = isWhitelisted;
    }

    public boolean getIsWhitelisted() {
        return isWhitelisted;
    }

    public Uri getTrayImageUri() {
        return trayImageUri;
    }

    public void setTrayImageUri(Uri trayImageUri) {
        this.trayImageUri = trayImageUri;
    }

    public String getTray_image_file() {
        return tray_image_file;
    }

    public void setTray_image_file(String tray_image_file) {
        this.tray_image_file = tray_image_file;
    }

    public String getTray_image_file_name() {
        return tray_image_file_name;
    }

    public void setTray_image_file_name(String tray_image_file_name) {
        this.tray_image_file_name = tray_image_file_name;
    }

    private StickerPackApi(Parcel in) {
        identifier = in.readString();
        name = in.readString();
        publisher = in.readString();
        trayImageFile = in.readString();
        publisherEmail = in.readString();
        publisherWebsite = in.readString();
        privacyPolicyWebsite = in.readString();
        licenseAgreementWebsite = in.readString();
        iosAppStoreLink = in.readString();
        stickers = in.createTypedArrayList(StickerApi.CREATOR);
        totalSize = in.readLong();
        androidPlayStoreLink = in.readString();
        isWhitelisted = in.readByte() != 0;
        imageDataVersion = in.readString();
        avoidCache = in.readByte() != 0;
        animatedStickerPack = in.readByte() != 0;
    }

    public static final Creator<StickerPack> CREATOR = new Creator<StickerPack>() {
        @Override
        public StickerPack createFromParcel(Parcel in) {
            return new StickerPack(in);
        }

        @Override
        public StickerPack[] newArray(int size) {
            return new StickerPack[size];
        }
    };

    public void setStickers(List<StickerApi> stickers) {
        this.stickers = stickers;
        totalSize = 0;
        for (StickerApi sticker : stickers) {
            totalSize += sticker.size;
        }
    }

    public void setAndroidPlayStoreLink(String androidPlayStoreLink) {
        this.androidPlayStoreLink = androidPlayStoreLink;
    }

    public void setIosAppStoreLink(String iosAppStoreLink) {
        this.iosAppStoreLink = iosAppStoreLink;
    }

    public List<StickerApi> getStickers() {
        return stickers;
    }

    long getTotalSize() {
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
        dest.writeString(publisherEmail);
        dest.writeString(publisherWebsite);
        dest.writeString(privacyPolicyWebsite);
        dest.writeString(licenseAgreementWebsite);
        dest.writeString(iosAppStoreLink);
        dest.writeTypedList(stickers);
        dest.writeLong(totalSize);
        dest.writeString(androidPlayStoreLink);
        dest.writeByte((byte) (isWhitelisted ? 1 : 0));
        dest.writeString(imageDataVersion);
        dest.writeByte((byte) (avoidCache ? 1 : 0));
        dest.writeByte((byte) (animatedStickerPack ? 1 : 0));
    }

    public StickerApi getStickerById(int index) {
        for (StickerApi s : this.stickers) {
            if (s.getImage_file_name().equals(String.valueOf(index))) {
                return s;
            }
        }
        return null;
    }
}
