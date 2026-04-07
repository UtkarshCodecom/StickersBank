package com.stickers.bank.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.stickers.bank.R;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.listeners.OnFolderItemClickListener;

import java.util.ArrayList;
import java.util.List;


public class BankFolderAdapter extends RecyclerView.Adapter<BankFolderAdapter.SimpleViewHolder> {

    private static final String TAG = BankFolderAdapter.class.getName();
    private List<StickerFolder> stickerFolders = new ArrayList<>();
    private final Context mContext;
    private final OnFolderItemClickListener onItemClickListener;

    public BankFolderAdapter(List<StickerFolder> projects, Context context, OnFolderItemClickListener onItemClickListener) {
        if (projects != null) {
            this.stickerFolders = projects;
        }
        this.mContext = context;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bank_folder, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        holder.bindData(stickerFolders.get(position), position);
    }

    @Override
    public int getItemCount() {
        return stickerFolders.size();
    }

    public class SimpleViewHolder extends RecyclerView.ViewHolder {

        private MaterialCardView mcv_folder;
        private TextView tv_folder;
        private ImageView iv_anim;

        public SimpleViewHolder(final View itemView) {
            super(itemView);
            mcv_folder = itemView.findViewById(R.id.mcv_folder);
            tv_folder = itemView.findViewById(R.id.tv_folder);
            iv_anim = itemView.findViewById(R.id.iv_anim);
        }

        public void bindData(StickerFolder stickerFolder, int position) {
            try {
                tv_folder.setText(stickerFolder.getFolderName());
                iv_anim.setVisibility(stickerFolder.isIs_animated() ? View.VISIBLE : View.INVISIBLE);
                mcv_folder.setOnClickListener(view -> {
                    onItemClickListener.onItemClick(view, position, stickerFolder);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
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