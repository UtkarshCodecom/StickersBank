package com.stickers.bank.ui.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.card.MaterialCardView;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;

import java.util.ArrayList;
import java.util.List;

public class FeaturedAdapter extends RecyclerView.Adapter<FeaturedAdapter.SimpleViewHolder> {

    private static final String TAG = FeaturedAdapter.class.getName();
    private List<FeaturedModel> balanceModelArrayList = new ArrayList<>();
    private final Context mContext;
    private final OnItemClickListener onItemClickListener;

    public FeaturedAdapter(List<FeaturedModel> projects, Context context, OnItemClickListener onItemClickListener) {
        if (projects != null) {
            this.balanceModelArrayList = projects;
        }
        this.mContext = context;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        holder.bindData(balanceModelArrayList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return balanceModelArrayList.size();
    }

    public class SimpleViewHolder extends RecyclerView.ViewHolder {

        private MaterialCardView mcv_featured;
        private MaterialCardView mcv_thumb;
        private TextView tv_title;
        private SimpleDraweeView iv_sticker;

        public SimpleViewHolder(final View itemView) {
            super(itemView);
            mcv_featured = (MaterialCardView) itemView.findViewById(R.id.mcv_featured);
            mcv_thumb = (MaterialCardView) itemView.findViewById(R.id.mcv_thumb);
            tv_title = (TextView) itemView.findViewById(R.id.tv_title);
            iv_sticker = (SimpleDraweeView) itemView.findViewById(R.id.iv_sticker);
        }

        public void bindData(FeaturedModel featuredModel, int position) {
            try {
                tv_title.setText(featuredModel.getName());
                Log.e(TAG, "bindData: --------------" + FeaturedAdapter.class.getSimpleName());
                /*Glide.with(mContext).
                        load(featuredModel.getImageUrl())
                        .into(iv_sticker);*/

                /*DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setUri(featuredModel.getImageUrl())
                        .setAutoPlayAnimations(true)
                        .build();
                iv_sticker.setController(controller);*/

                mcv_featured.setOnClickListener(view -> {
                    onItemClickListener.onItemClick(view, position);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}