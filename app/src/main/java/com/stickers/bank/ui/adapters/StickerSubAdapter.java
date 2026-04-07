package com.stickers.bank.ui.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.card.MaterialCardView;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.StickerModel;

import java.util.ArrayList;
import java.util.List;


public class StickerSubAdapter extends RecyclerView.Adapter<StickerSubAdapter.SimpleViewHolder> {

    private static final String TAG = StickerSubAdapter.class.getName();
    private List<FeaturedModel> stickerModels = new ArrayList<>();
    private final Context mContext;
    private final OnItemClickListener onItemClickListener;

    public StickerSubAdapter(List<FeaturedModel> projects, Context context, OnItemClickListener onItemClickListener) {
        if (projects != null) {
            this.stickerModels = projects;
        }
        this.mContext = context;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_cat, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        holder.bindData(stickerModels.get(position), position);
    }

    @Override
    public int getItemCount() {
        return stickerModels.size();
    }

    public class SimpleViewHolder extends RecyclerView.ViewHolder {

        private final ConstraintLayout cl_sticker;
        private final MaterialCardView mcv_thumb;
        private final SimpleDraweeView iv_sticker;
        private final TextView tv_cat_name;

        public SimpleViewHolder(final View itemView) {
            super(itemView);
            cl_sticker = itemView.findViewById(R.id.cl_sticker);
            mcv_thumb = itemView.findViewById(R.id.mcv_thumb);
            iv_sticker = itemView.findViewById(R.id.iv_sticker);
            tv_cat_name = itemView.findViewById(R.id.tv_cat_name);
        }

        public void bindData(FeaturedModel featuredModel, int position) {
            try {
               /* Glide.with(mContext).
                        load(stickerModel.getStickerUrl())
                        .into(iv_sticker);*/

                Log.e(TAG, "bindData:----------------- " + StickerSubAdapter.class.getSimpleName());

                Log.e(TAG, "bindData:getImageUrl-" + featuredModel.getImageUrl());
                if (featuredModel.getImageUrl() != null || featuredModel.getImageUrl().length() != 0 || !featuredModel.getImageUrl().isEmpty()) {
                    DraweeController controller = Fresco.newDraweeControllerBuilder().setUri(featuredModel.getImageUrl()).setAutoPlayAnimations(true).build();
                    iv_sticker.setController(controller);
                    mcv_thumb.setVisibility(View.VISIBLE);
                } else {
                    mcv_thumb.setVisibility(View.GONE);
                }
                tv_cat_name.setText(featuredModel.getName());


                cl_sticker.setOnClickListener(view -> {
                    onItemClickListener.onItemClick(view, position);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}