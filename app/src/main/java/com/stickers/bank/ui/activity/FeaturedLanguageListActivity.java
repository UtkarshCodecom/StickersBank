package com.stickers.bank.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.FeaturedResponse;
import com.stickers.bank.data.webservices.APIRequest;
import com.stickers.bank.data.webservices.ResponseCallback;
import com.stickers.bank.databinding.ActivityFeaturedLanListBinding;
import com.stickers.bank.ui.adapters.LangListAdapter;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.utils.AdvUtils;
import com.stickers.bank.utils.ParamArgus;
import com.stickers.bank.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FeaturedLanguageListActivity extends BaseActivity<ActivityFeaturedLanListBinding> {

    ArrayList<FeaturedModel> data = new ArrayList<>();
    String name = "";
    LangListAdapter langListAdapter;

    @Override
    public int getLayoutId() {
        return R.layout.activity_featured_lan_list;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        Bundle bundle = getIntent().getExtras();
        data = (ArrayList<FeaturedModel>) bundle.getSerializable(ParamArgus.MODEL);
        name = bundle.getString(ParamArgus.TITLE);

        if (data != null) {
            binding.header.tvTitle.setText(name);
        }

        langListAdapter = new LangListAdapter(data, this, new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (Utils.isNetworkAvailable(FeaturedLanguageListActivity.this, false, false)) {
                    getSubSubCategory(data.get(position).getCategoryId(), data.get(position).getId());
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        binding.rvLanList.setAdapter(langListAdapter);
        AdvUtils.getInstance(this).loadShowBMed(binding.flBanner, AdvUtils.getAdSize(FeaturedLanguageListActivity.this));
    }

    @Override
    protected void setListeners() {

    }

    public void getSubSubCategory(int categoryId, int id) {
        showProgressDialog();

        Map<String, Object> map = new HashMap<>();
        map.put("category_id", categoryId);
        map.put("sub_category_id", id);

        APIRequest apiRequest = new APIRequest(null);
        apiRequest.getSubSubCategory(map, new ResponseCallback() {
            @Override
            public void ResponseSuccessCallBack(Object object) {
                try {
                    hideDialog();
                    if (object instanceof FeaturedResponse) {
                        FeaturedResponse featuredResponse = (FeaturedResponse) object;
                        if (featuredResponse != null && featuredResponse.isSuccess()) {
                            if (featuredResponse.getData() != null && featuredResponse.getData().size() > 0) {
                                startActivity(new Intent(FeaturedLanguageListActivity.this, FeaturedSubActivity.class)
                                        .putExtra(ParamArgus.MODEL, featuredResponse.getData())
                                        .putExtra(ParamArgus.TITLE, name));
                            }
                        } else {
                            new MaterialAlertDialogBuilder(FeaturedLanguageListActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                                    .setMessage(featuredResponse.getMessage())
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