package com.stickers.bank.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class NewArrivalResponse extends BaseDataResponse {

    ArrayList<NewArrivalModel> data;

    public ArrayList<NewArrivalModel> getData() {
        return data;
    }

    public void setData(ArrayList<NewArrivalModel> data) {
        this.data = data;
    }

    public class NewArrivalModel implements Serializable {

        @SerializedName("id")
        @Expose
        private int id;
        @SerializedName("main_category_id")
        @Expose
        private int mainCategoryId;
        @SerializedName("sub_category_id")
        @Expose
        private int subCategoryId;
        @SerializedName("sub_sub_category_id")
        @Expose
        private int subSubCategoryId;
        @SerializedName("sticker_image")
        @Expose
        private String stickerImage;
        @SerializedName("is_active")
        @Expose
        private String isActive;
        @SerializedName("created_at")
        @Expose
        private String createdAt;
        @SerializedName("sticker_url")
        @Expose
        private String stickerUrl;
        @SerializedName("main_category")
        @Expose
        private MainCategory mainCategory;
        @SerializedName("sub_category")
        @Expose
        private SubCategory subCategory;
        @SerializedName("sub_sub_category")
        @Expose
        private SubSubCategory subSubCategory;

        @SerializedName("is_animated")
        @Expose
        private int isAnimated;

        private boolean isSelected;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getMainCategoryId() {
            return mainCategoryId;
        }

        public void setMainCategoryId(int mainCategoryId) {
            this.mainCategoryId = mainCategoryId;
        }

        public int getSubCategoryId() {
            return subCategoryId;
        }

        public void setSubCategoryId(int subCategoryId) {
            this.subCategoryId = subCategoryId;
        }

        public int getSubSubCategoryId() {
            return subSubCategoryId;
        }

        public void setSubSubCategoryId(int subSubCategoryId) {
            this.subSubCategoryId = subSubCategoryId;
        }

        public String getStickerImage() {
            return stickerImage;
        }

        public void setStickerImage(String stickerImage) {
            this.stickerImage = stickerImage;
        }

        public String getIsActive() {
            return isActive;
        }

        public void setIsActive(String isActive) {
            this.isActive = isActive;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getStickerUrl() {
            return stickerUrl;
        }

        public void setStickerUrl(String stickerUrl) {
            this.stickerUrl = stickerUrl;
        }

        public MainCategory getMainCategory() {
            return mainCategory;
        }

        public void setMainCategory(MainCategory mainCategory) {
            this.mainCategory = mainCategory;
        }

        public SubCategory getSubCategory() {
            return subCategory;
        }

        public void setSubCategory(SubCategory subCategory) {
            this.subCategory = subCategory;
        }

        public SubSubCategory getSubSubCategory() {
            return subSubCategory;
        }

        public void setSubSubCategory(SubSubCategory subSubCategory) {
            this.subSubCategory = subSubCategory;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public int getIsAnimated() {
            return isAnimated;
        }

        public void setIsAnimated(int isAnimated) {
            this.isAnimated = isAnimated;
        }

        public class MainCategory {

            @SerializedName("id")
            @Expose
            private int id;
            @SerializedName("name")
            @Expose
            private String name;
            @SerializedName("image_url")
            @Expose
            private String imageUrl;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getImageUrl() {
                return imageUrl;
            }

            public void setImageUrl(String imageUrl) {
                this.imageUrl = imageUrl;
            }

        }

        public class SubCategory {

            @SerializedName("id")
            @Expose
            private int id;
            @SerializedName("name")
            @Expose
            private String name;
            @SerializedName("image_url")
            @Expose
            private String imageUrl;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getImageUrl() {
                return imageUrl;
            }

            public void setImageUrl(String imageUrl) {
                this.imageUrl = imageUrl;
            }

        }

        public class SubSubCategory {

            @SerializedName("id")
            @Expose
            private int id;
            @SerializedName("name")
            @Expose
            private String name;
            @SerializedName("image_url")
            @Expose
            private String imageUrl;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getImageUrl() {
                return imageUrl;
            }

            public void setImageUrl(String imageUrl) {
                this.imageUrl = imageUrl;
            }

        }
    }
}
