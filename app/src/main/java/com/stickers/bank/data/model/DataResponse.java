package com.stickers.bank.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class DataResponse implements Serializable {

    ArrayList<Data> data;

    public ArrayList<Data> getData() {
        return data;
    }

    public void setData(ArrayList<Data> data) {
        this.data = data;
    }

    public class Data implements Serializable {

        public int id;
        public String cat_name;
        ArrayList<SList> s_list;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public ArrayList<SList> getS_list() {
            return s_list;
        }

        public void setS_list(ArrayList<SList> s_list) {
            this.s_list = s_list;
        }

        public String getCat_name() {
            return cat_name;
        }

        public void setCat_name(String cat_name) {
            this.cat_name = cat_name;
        }

        public class SList implements Serializable {

            public String lang;
            ArrayList<SubCat> sub_cat;

            public String getLang() {
                return lang;
            }

            public void setLang(String lang) {
                this.lang = lang;
            }

            public ArrayList<SubCat> getSub_cat() {
                return sub_cat;
            }

            public void setSub_cat(ArrayList<SubCat> sub_cat) {
                this.sub_cat = sub_cat;
            }

            public class SubCat implements Serializable {

                public int sub_cat_id;
                public String sub_cat_name;
                ArrayList<StickerModel> sticker;

                public int getSub_cat_id() {
                    return sub_cat_id;
                }

                public void setSub_cat_id(int sub_cat_id) {
                    this.sub_cat_id = sub_cat_id;
                }

                public String getSub_cat_name() {
                    return sub_cat_name;
                }

                public void setSub_cat_name(String sub_cat_name) {
                    this.sub_cat_name = sub_cat_name;
                }

                public ArrayList<StickerModel> getSticker() {
                    return sticker;
                }

                public void setSticker(ArrayList<StickerModel> sticker) {
                    this.sticker = sticker;
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return id == data.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
