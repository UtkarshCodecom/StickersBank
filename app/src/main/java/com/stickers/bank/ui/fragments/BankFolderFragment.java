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

import com.stickers.bank.R;
import com.stickers.bank.data.database.DatabaseClient;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.listeners.OnFolderItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.databinding.FragmentBankFolderBinding;
import com.stickers.bank.ui.activity.BankFolderDetailsActivity;
import com.stickers.bank.ui.adapters.BankFolderAdapter;
import com.stickers.bank.ui.base.BaseFragment;
import com.stickers.bank.utils.ParamArgus;
import com.stickers.bank.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class BankFolderFragment extends BaseFragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private FragmentBankFolderBinding binding;
    FeaturedModel data;
    ArrayList<StickerFolder> stickerModels = new ArrayList<>();
    BankFolderAdapter folderAdapter;

    public BankFolderFragment() {
        // Required empty public constructor
    }

    public static BankFolderFragment newInstance(FeaturedModel data) {
        BankFolderFragment fragment = new BankFolderFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, data);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBankFolderBinding.inflate(inflater, container, false);
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

        folderAdapter = new BankFolderAdapter(stickerModels, getActivity(), new OnFolderItemClickListener() {
            @Override
            public void onItemClick(View view, int position, StickerFolder stickerFolder) {
                startActivity(new Intent(getActivity(), BankFolderDetailsActivity.class)
                        .putExtra(ParamArgus.MODEL, stickerModels.get(position)));
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        binding.rvFolder.setAdapter(folderAdapter);
    }

    @Override
    public void setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            if (Utils.isNetworkAvailable(getActivity(), false, false)) {
                new GetAllFolders().execute();
            }
        });
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
            case R.id.btn_back:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new GetAllFolders().execute();
    }

    private class GetAllFolders extends AsyncTask<Void, Void, List<StickerFolder>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<StickerFolder> doInBackground(Void... voids) {
            //adding to database
            List<StickerFolder> arrayList = DatabaseClient.getInstance(getActivity()).getAppDatabase().getStickerDao().getAllFolders();
            return arrayList;
        }

        @Override
        protected void onPostExecute(List<StickerFolder> stickerFolderList) {
            super.onPostExecute(stickerFolderList);
            stickerModels.clear();
            if (stickerFolderList != null && stickerFolderList.size() > 0) {
                stickerModels.addAll(stickerFolderList);
            }
            folderAdapter.notifyDataSetChanged();
        }
    }
}