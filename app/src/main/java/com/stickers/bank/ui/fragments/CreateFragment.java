package com.stickers.bank.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.stickers.bank.MainActivity;
import com.stickers.bank.R;
import com.stickers.bank.data.database.DatabaseClient;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.listeners.OnFolderItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.databinding.FragmentBankFolderBinding;
import com.stickers.bank.databinding.FragmentCreateBinding;
import com.stickers.bank.ui.activity.BankFolderDetailsActivity;
import com.stickers.bank.ui.activity.CreateAnimatedStickerActivity;
import com.stickers.bank.ui.activity.CreateStickerActivity;
import com.stickers.bank.ui.adapters.BankFolderAdapter;
import com.stickers.bank.ui.base.BaseFragment;
import com.stickers.bank.utils.ParamArgus;
import com.stickers.bank.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class CreateFragment extends BaseFragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private FragmentCreateBinding binding;
    FeaturedModel data;
    ArrayList<StickerFolder> stickerModels = new ArrayList<>();
    BankFolderAdapter folderAdapter;

    public CreateFragment() {
        // Required empty public constructor
    }

    public static CreateFragment newInstance(FeaturedModel data) {
        CreateFragment fragment = new CreateFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, data);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initViews();
        setListeners();

        return root;
    }

    @Override
    public void initViews() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            data = (FeaturedModel) bundle.getSerializable(ARG_PARAM1);
            if (data != null) {
            }
        }

    }

    @Override
    public void setListeners() {
        binding.btnCustomSticker.setOnClickListener(this);
        binding.btnAnimatedSticker.setOnClickListener(this);
    }

    @Override
    protected Context getActContext() {
        return requireActivity();
    }

    @Override
    protected Fragment getFragmentContext() {
        return this;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_custom_sticker:
                startActivity(new Intent(getActivity(), CreateStickerActivity.class));
                break;
            case R.id.btn_animated_sticker:
                startActivity(new Intent(getActivity(), CreateAnimatedStickerActivity.class));
                break;
        }
    }

}