package com.stickers.bank.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.card.MaterialCardView;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.NewArrivalResponse;

import java.util.ArrayList;
import java.util.List;

public class NewArrivalAdapter extends RecyclerView.Adapter<NewArrivalAdapter.SimpleViewHolder> {

    private static final String TAG = NewArrivalAdapter.class.getName();
    private List<NewArrivalResponse.NewArrivalModel> newArrivalModels = new ArrayList<>();
    private final Context mContext;
    private final OnItemClickListener onItemClickListener;
    private boolean isInSelectionMode = false;

    public NewArrivalAdapter(List<NewArrivalResponse.NewArrivalModel> projects, Context context, OnItemClickListener onItemClickListener) {
        if (projects != null) {
            this.newArrivalModels = projects;
        }
        this.mContext = context;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_arrival, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        holder.bindData(newArrivalModels.get(position), position);
    }

    @Override
    public int getItemCount() {
        return newArrivalModels.size();
    }

    public class SimpleViewHolder extends RecyclerView.ViewHolder {

        private final ConstraintLayout cl_new_arrival;
        private final MaterialCardView mcv_thumb;
        //private final SimpleDraweeView iv_sticker;
        private final ImageView iv_sticker;
        private final CheckBox cb_select;

        public SimpleViewHolder(final View itemView) {
            super(itemView);
            cl_new_arrival = itemView.findViewById(R.id.cl_new_arrival);
            mcv_thumb = itemView.findViewById(R.id.mcv_thumb);
            iv_sticker = itemView.findViewById(R.id.iv_sticker);
            cb_select = itemView.findViewById(R.id.cb_select);
        }

        public void bindData(NewArrivalResponse.NewArrivalModel newArrivalModel, int position) {
            try {
                /*Glide.with(mContext).
                        load(newArrivalModel.getStickerUrl())
                        .into(iv_sticker);*/

                /*DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setUri(newArrivalModel.getStickerUrl())
                        .setAutoPlayAnimations(true)
                        .build();
                iv_sticker.setController(controller);*/

                Glide.with(mContext).
                        load(newArrivalModel.getStickerUrl())
                        .into(iv_sticker);

                if (isInSelectionMode) {
                    cb_select.setVisibility(View.VISIBLE);
                    if (newArrivalModel.isSelected()) {
                        cb_select.setChecked(true);
                    } else {
                        cb_select.setChecked(false);
                    }
                } else {
                    cb_select.setChecked(false);
                    cb_select.setVisibility(View.INVISIBLE);
                }

                cl_new_arrival.setOnClickListener(view -> {
                    if (isInSelectionMode) {
                        if (newArrivalModel.isSelected()) {
                            cb_select.setChecked(false);
                            newArrivalModels.get(position).setSelected(false);
                            newArrivalModel.setSelected(false);
                        } else {
                            cb_select.setChecked(true);
                            newArrivalModels.get(position).setSelected(true);
                            newArrivalModel.setSelected(true);
                        }
                    } else {
                        onItemClickListener.onItemClick(view, position);
                    }
                });

                cb_select.setOnClickListener(view -> {
                    if (isInSelectionMode) {
                        if (newArrivalModel.isSelected()) {
                            cb_select.setChecked(false);
                            newArrivalModels.get(position).setSelected(false);
                            newArrivalModel.setSelected(false);
                        } else {
                            cb_select.setChecked(true);
                            newArrivalModels.get(position).setSelected(true);
                            newArrivalModel.setSelected(true);
                        }
                    } else {
                        onItemClickListener.onItemClick(view, position);
                    }
                });

                cl_new_arrival.setOnLongClickListener(view -> {
                    isInSelectionMode = true;
                    newArrivalModels.get(position).setSelected(true);
                    newArrivalModel.setSelected(true);
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
        for (int i = 0; i < newArrivalModels.size(); i++) {
            newArrivalModels.get(i).setSelected(false);
        }
    }
}