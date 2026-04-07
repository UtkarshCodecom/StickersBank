package com.stickers.bank;

import static com.stickers.bank.data.common.Constants.token;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.stickers.bank.data.listeners.InterstitialAdvListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.StickerResponse;
import com.stickers.bank.data.webservices.APIRequest;
import com.stickers.bank.data.webservices.ResponseCallback;
import com.stickers.bank.databinding.ActivityMainBinding;
import com.stickers.bank.ui.activity.ContactUsActivity;
import com.stickers.bank.ui.activity.CreateAnimatedStickerActivity;
import com.stickers.bank.ui.activity.CreateStickerActivity;
import com.stickers.bank.ui.activity.FaqsActivity;
import com.stickers.bank.ui.activity.FeaturedSubActivity;
import com.stickers.bank.ui.activity.PrivacyPolicyActivity;
import com.stickers.bank.ui.adapters.ViewPagerAdapter;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.ui.fragments.BankFolderFragment;
import com.stickers.bank.ui.fragments.CreateFragment;
import com.stickers.bank.ui.fragments.FeaturedFragment;
import com.stickers.bank.ui.fragments.NewArrivalFragment;
import com.stickers.bank.utils.AdvUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity<ActivityMainBinding> implements View.OnClickListener {
    ViewPagerAdapter viewPagerAdapter;

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(FeaturedFragment.newInstance(new FeaturedModel()));
        fragments.add(NewArrivalFragment.newInstance(new FeaturedModel()));
        fragments.add(CreateFragment.newInstance(new FeaturedModel()));
        fragments.add(BankFolderFragment.newInstance(new FeaturedModel()));

        viewPagerAdapter = new ViewPagerAdapter(this, fragments);
        binding.pager.setAdapter(viewPagerAdapter);

        binding.tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                AdvUtils.getInstance(MainActivity.this).showInterstitial(false, 0, new InterstitialAdvListener() {
                    @Override
                    public void onInterstitialAdLoaded() {

                    }

                    @Override
                    public void onInterstitialAdClosed() {
                    }

                    @Override
                    public void onContinue() {
                    }
                });
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        new TabLayoutMediator(binding.tabLayout, binding.pager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(getResources().getString(R.string.featured));
                            break;
                        case 1:
                            tab.setText(getResources().getString(R.string.new_arrival));
                            break;
                        case 2:
                            tab.setText(getResources().getString(R.string.Create));
                            break;
                        case 3:
                            tab.setText(getResources().getString(R.string.bank));
                            break;
                    }

                }
        ).attach();

        sendFCMToken();
        AdvUtils.getInstance(this).loadShowBMed(binding.flBanner, AdvUtils.getAdSize(MainActivity.this));
    }

    @Override
    protected void setListeners() {
        binding.btnSearch.setOnClickListener(this);
        binding.btnMore.setOnClickListener(this);
    }

    private void showMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnMore);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true);
        }
        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.option_contact_us:
                    startActivity(new Intent(MainActivity.this, ContactUsActivity.class));
                    break;
                case R.id.option_faqs:
                    startActivity(new Intent(MainActivity.this, FaqsActivity.class));
                    break;
                case R.id.option_about_us:
                    startActivity(new Intent(MainActivity.this, PrivacyPolicyActivity.class));
                    break;
            }
            return true;
        });
        // Show the popup menu.
        popup.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_search:
                break;
            case R.id.btn_more:
                showMenu();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialAlertDialogBuilder(getContext(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.exit), (dialogInterface, i) -> {
                    super.onBackPressed();
                }).setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {

                })
                .show();
    }


    public void sendFCMToken() {
        showProgressDialog();

        Map<String, Object> map = new HashMap<>();
        map.put("fcm_token", token);
        map.put("device_type", "Android");
        //map.put("sub_sub_category_id", sub_sub_category_id);// do not pass if not sub sub

        APIRequest apiRequest = new APIRequest(null);
        apiRequest.sendToken(map, new ResponseCallback() {
            @Override
            public void ResponseSuccessCallBack(Object object) {
                try {
                    hideDialog();
                    if (object instanceof StickerResponse) {
                        StickerResponse stickerResponse = (StickerResponse) object;
                        if (stickerResponse != null && stickerResponse.isSuccess()) {
                            if (stickerResponse.getData() != null && stickerResponse.getData().size() > 0) {
                            }
                        } else {
                            new MaterialAlertDialogBuilder(MainActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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

   /* private void askNotificationPermission() {
        // This is only necessary for API Level > 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this, "FCM can't post notifications without POST_NOTIFICATIONS permission",
                            Toast.LENGTH_LONG).show();
                }
            });*/

}