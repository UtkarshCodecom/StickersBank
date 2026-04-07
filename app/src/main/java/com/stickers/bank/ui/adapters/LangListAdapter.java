package com.stickers.bank.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.StickerModel;

import java.util.ArrayList;
import java.util.List;


public class LangListAdapter extends RecyclerView.Adapter<LangListAdapter.SimpleViewHolder> {

    private static final String TAG = LangListAdapter.class.getName();
    private List<FeaturedModel> balanceModelArrayList = new ArrayList<>();
    private final Context mContext;
    private final OnItemClickListener onItemClickListener;

    public LangListAdapter(List<FeaturedModel> projects, Context context, OnItemClickListener onItemClickListener) {
        if (projects != null) {
            this.balanceModelArrayList = projects;
        }
        this.mContext = context;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lang_list, parent, false);
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

        private MaterialCardView mcv_featured_lan;
        private TextView tv_title;

        public SimpleViewHolder(final View itemView) {
            super(itemView);
            mcv_featured_lan = (MaterialCardView) itemView.findViewById(R.id.mcv_featured_lan);
            tv_title = (TextView) itemView.findViewById(R.id.tv_title);
        }

        public void bindData(FeaturedModel featuredModel, int position) {
            try {
                tv_title.setText(featuredModel.getName());
                /*Glide.with(mContext).
                        load(R.drawable.ic_master_card)
                        .into(iv_card_logo);*/
                mcv_featured_lan.setOnClickListener(view -> {
                    onItemClickListener.onItemClick(view, position);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}