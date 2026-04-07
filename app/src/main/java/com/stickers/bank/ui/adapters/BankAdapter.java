package com.stickers.bank.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.card.MaterialCardView;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.StickerModel;

import java.util.ArrayList;
import java.util.List;


public class BankAdapter extends RecyclerView.Adapter<BankAdapter.SimpleViewHolder> {

    private static final String TAG = BankAdapter.class.getName();
    private List<StickerModel> stickerModels = new ArrayList<>();
    private final Context mContext;
    private final OnItemClickListener onItemClickListener;
    private boolean isInSelectionMode = false;

    public BankAdapter(List<StickerModel> projects, Context context, OnItemClickListener onItemClickListener) {
        if (projects != null) {
            this.stickerModels = projects;
        }
        this.mContext = context;
        this.onItemClickListener = onItemClickListener;
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

        private ConstraintLayout cl_sticker;
        private MaterialCardView mcv_thumb;
        private ImageView iv_sticker;
        private CheckBox cb_select;

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

               /* DraweeController controller = Fresco.newDraweeControllerBuilder()
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

                //as no need to selection bank fragment
                /*cl_sticker.setOnLongClickListener(view -> {
                    isInSelectionMode = true;
                    stickerModels.get(position).setSelected(true);
                    stickerModel.setSelected(true);
                    cb_select.setVisibility(View.VISIBLE);
                    cb_select.setChecked(true);
                    notifyDataSetChanged();
                    onItemClickListener.onItemLongClick(view, position);
                    return false;
                });*/
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

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }
}