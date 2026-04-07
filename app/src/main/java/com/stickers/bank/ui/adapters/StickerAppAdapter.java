package com.stickers.bank.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.card.MaterialCardView;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.StickerModel;

import java.util.ArrayList;
import java.util.List;


public class StickerAppAdapter extends RecyclerView.Adapter<StickerAppAdapter.SimpleViewHolder> {

    private static final String TAG = StickerAppAdapter.class.getName();
    private List<StickerModel> stickerModels = new ArrayList<>();
    private final Context mContext;
    private final OnItemClickListener onItemClickListener;
    private boolean isInSelectionMode = false;

    public StickerAppAdapter(List<StickerModel> projects, Context context, OnItemClickListener onItemClickListener) {
        if (projects != null) {
            this.stickerModels = projects;
        }
        this.mContext = context;
        this.onItemClickListener = onItemClickListener;
        Log.e(TAG, "StickerAdapter: ");
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker, parent, false);
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
        //private final SimpleDraweeView iv_sticker;
        private final ImageView iv_sticker;
        private final CheckBox cb_select;

        public SimpleViewHolder(final View itemView) {
            super(itemView);
            cl_sticker = itemView.findViewById(R.id.cl_sticker);
            mcv_thumb = itemView.findViewById(R.id.mcv_thumb);
            iv_sticker = itemView.findViewById(R.id.iv_sticker);
            cb_select = itemView.findViewById(R.id.cb_select);
        }

        public void bindData(StickerModel stickerModel, int position) {
            try {
                Glide.with(mContext).
                        load(stickerModel.getStickerUrl())
                        .into(iv_sticker);

                /*Glide.with(mContext).asGif()
                        .load(stickerModel.getStickerUrl())
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .into(iv_sticker);*/

                /*iv_sticker.loadUrl(stickerModel.getStickerUrl());
                iv_sticker.setBackgroundColor(Color.TRANSPARENT);
                iv_sticker.getSettings().setLoadWithOverviewMode(true);
                iv_sticker.getSettings().setUseWideViewPort(true);*/


                Log.e(TAG, "TAG: " + StickerAppAdapter.class.getSimpleName());

                /*DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setUri(stickerModel.getStickerUrl())
                        .setAutoPlayAnimations(true)
                        .build();
                iv_sticker.setController(controller);*/

                if (isInSelectionMode) {
                    cb_select.setVisibility(View.VISIBLE);
                    if (stickerModel.isSelected()) {
                        cb_select.setChecked(true);
                    } else {
                        cb_select.setChecked(false);
                    }
                } else {
                    cb_select.setChecked(false);
                    cb_select.setVisibility(View.INVISIBLE);
                }

                cl_sticker.setOnClickListener(view -> {
                    if (isInSelectionMode) {
                        if (stickerModel.isSelected()) {
                            cb_select.setChecked(false);
                            stickerModels.get(position).setSelected(false);
                            stickerModel.setSelected(false);
                        } else {
                            cb_select.setChecked(true);
                            stickerModels.get(position).setSelected(true);
                            stickerModel.setSelected(true);
                        }
                    } else {
                        onItemClickListener.onItemClick(view, position);
                    }
                });
                cb_select.setOnClickListener(view -> {
                    if (isInSelectionMode) {
                        if (stickerModel.isSelected()) {
                            cb_select.setChecked(false);
                            stickerModels.get(position).setSelected(false);
                            stickerModel.setSelected(false);
                        } else {
                            cb_select.setChecked(true);
                            stickerModels.get(position).setSelected(true);
                            stickerModel.setSelected(true);
                        }
                    } else {
                        onItemClickListener.onItemClick(view, position);
                    }
                });

                cl_sticker.setOnLongClickListener(view -> {
                    isInSelectionMode = true;
                    stickerModels.get(position).setSelected(true);
                    stickerModel.setSelected(true);
                    cb_select.setVisibility(View.VISIBLE);
                    cb_select.setChecked(true);
                    notifyDataSetChanged();
                    onItemClickListener.onItemLongClick(view, position);
                    return false;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isInSelectionMode() {
        return isInSelectionMode;
    }

    public void setInSelectionMode(boolean inSelectionMode) {
        isInSelectionMode = inSelectionMode;
    }

    public void removeSelection() {
        for (int i = 0; i < stickerModels.size(); i++) {
            stickerModels.get(i).setSelected(false);
        }
    }
}