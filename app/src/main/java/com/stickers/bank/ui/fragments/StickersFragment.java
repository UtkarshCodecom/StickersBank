package com.stickers.bank.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.StickerModel;
import com.stickers.bank.data.model.StickerResponse;
import com.stickers.bank.data.webservices.APIRequest;
import com.stickers.bank.data.webservices.ResponseCallback;
import com.stickers.bank.databinding.FragmentStickersBinding;
import com.stickers.bank.ui.adapters.StickerAppAdapter;
import com.stickers.bank.ui.base.BaseFragment;
import com.stickers.bank.utils.GridSpacingItemDecoration;
import com.stickers.bank.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StickersFragment extends BaseFragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private FragmentStickersBinding binding;
    FeaturedModel data;
    ArrayList<StickerModel> stickerModels = new ArrayList<>();
    StickerAppAdapter stickerAdapter;

    public StickersFragment() {
        // Required empty public constructor
    }

    public static StickersFragment newInstance(FeaturedModel data) {
        StickersFragment fragment = new StickersFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, data);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStickersBinding.inflate(inflater, container, false);
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
                if (Utils.isNetworkAvailable(getActivity(), false, false)) {
                    getStickers(data.getCategoryId(), data.getSubCategoryId(), data.getId());
                }
            }
        }

        stickerAdapter = new StickerAppAdapter(stickerModels, getActivity(), new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (stickerAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (stickerAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                }
            }
        });
        binding.rvNewArrival.setLayoutManager(new GridLayoutManager(requireActivity(), 3));
        int spanCount = 3; // 3 columns
        int spacing = 16; // 22px
        boolean includeEdge = true;
        binding.rvNewArrival.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        binding.rvNewArrival.setAdapter(stickerAdapter);
    }

    @Override
    public void setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            if (Utils.isNetworkAvailable(getActivity(), false, false)) {
                getStickers(data.getCategoryId(), data.getSubCategoryId(), data.getId());
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

    }

    public void getStickers(int categoryId, int sub_category_id, int id) {
        showProgressDialog();

        Map<String, Object> map = new HashMap<>();
        map.put("category_id", categoryId);
        map.put("sub_category_id", sub_category_id);
        map.put("sub_sub_category_id", id);

        APIRequest apiRequest = new APIRequest(null);
        apiRequest.getStickers(map, new ResponseCallback() {
            @Override
            public void ResponseSuccessCallBack(Object object) {
                try {
                    hideDialog();
                    if (object instanceof StickerResponse) {
                        StickerResponse stickerResponse = (StickerResponse) object;
                        if (stickerResponse != null && stickerResponse.isSuccess()) {
                            if (stickerResponse.getData() != null && stickerResponse.getData().size() > 0) {
                                stickerModels.clear();
                                stickerModels.addAll(stickerResponse.getData());
                                stickerAdapter.notifyDataSetChanged();
                            }
                        } else {
                            new MaterialAlertDialogBuilder(getActivity(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                                    .setMessage(stickerResponse.getMessage())
                                    .setCancelable(false)
                                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                                    }).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void ResponseFailCallBack(String message) {
                hideDialog();
                showToastMsg(message);
            }

            @Override
            public void onResponseFail(String msg) {
                hideDialog();
                showToastMsg(msg);
            }
        });
    }
}