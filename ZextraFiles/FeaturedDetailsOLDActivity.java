package com.stickers.bank.ui.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayoutMediator;
import com.stickers.bank.R;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.databinding.ActivityFeaturedDetailsOldBinding;
import com.stickers.bank.ui.adapters.ViewPagerAdapter;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.ui.fragments.StickersFragment;
import com.stickers.bank.utils.ParamArgus;

import java.util.ArrayList;

public class FeaturedDetailsOLDActivity extends BaseActivity<ActivityFeaturedDetailsOldBinding> {

    ArrayList<FeaturedModel> data;
    ViewPagerAdapter viewPagerAdapter;
    String name = "";

    @Override
    public int getLayoutId() {
        return R.layout.activity_featured_details_old;
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
            ArrayList<Fragment> fragments = new ArrayList<>();
            /*for (int i = 0; i < data.size(); i++) {
                fragments.add(StickersFragment.newInstance(data.get(i)));
            }*/

            for (int i = 0; i < 1; i++) {
                fragments.add(StickersFragment.newInstance(data.get(i)));
            }

            viewPagerAdapter = new ViewPagerAdapter(this, fragments);
            binding.pagerDetails.setAdapter(viewPagerAdapter);

            new TabLayoutMediator(binding.tabLayout, binding.pagerDetails,
                    (tab, position) -> {
                        tab.setText(data.get(position).getName());
                    }
            ).attach();

        }
    }

    @Override
    protected void setListeners() {

    }
}