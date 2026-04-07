package com.stickers.bank.ui.activity;

import static com.stickers.bank.data.common.Constants.BANK_FEATURE;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.stickers.bank.BuildConfig;
import com.stickers.bank.R;
import com.stickers.bank.data.common.Constants;
import com.stickers.bank.data.database.DatabaseClient;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.database.entity.StickerModelDB;
import com.stickers.bank.data.listeners.InterstitialAdvListener;
import com.stickers.bank.data.listeners.OnFolderItemClickListener;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.FeaturedResponse;
import com.stickers.bank.data.model.StickerModel;
import com.stickers.bank.data.model.StickerResponse;
import com.stickers.bank.data.webservices.APIRequest;
import com.stickers.bank.data.webservices.ResponseCallback;
import com.stickers.bank.databinding.ActivityFeaturedSubBinding;
import com.stickers.bank.ui.adapters.FolderAdapter;
import com.stickers.bank.ui.adapters.StickerAppAdapter;
import com.stickers.bank.ui.adapters.StickerSubAdapter;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.ui.fragments.BankFolderFragmentOld;
import com.stickers.bank.utils.AdvUtils;
import com.stickers.bank.utils.FolderUtils;
import com.stickers.bank.utils.GridSpacingItemDecoration;
import com.stickers.bank.utils.ParamArgus;
import com.stickers.bank.utils.PearlTextUtils;
import com.stickers.bank.utils.Preferences;
import com.stickers.bank.utils.StickerHandler;
import com.stickers.bank.utils.Utils;
import com.stickers.bank.utils.WhitelistCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeaturedSubActivity extends BaseActivity<ActivityFeaturedSubBinding> implements View.OnClickListener {

    ArrayList<FeaturedModel> data;
    String name = "";
    List<StickerFolder> stkrFldr;

    //ArrayList<ArrayList<StickerModel>> stickerModelsMainCatList = new ArrayList<>();
    ArrayList<ArrayList<StickerModel>> stickerModelsSubCatList = new ArrayList<>();
    ArrayList<StickerModel> stickerModels = new ArrayList<>();
    StickerAppAdapter stickerAdapter;

    ArrayList<FeaturedModel> dataSubSub = new ArrayList<>();
    StickerSubAdapter stickerSubAdapter;
    private int pos = 0;
    AlertDialog dialog = null;

    @Override
    public int getLayoutId() {
        return R.layout.activity_featured_sub;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        binding.header.btnBack.setVisibility(View.VISIBLE);
        Bundle bundle = getIntent().getExtras();
        data = (ArrayList<FeaturedModel>) bundle.getSerializable(ParamArgus.MODEL);
        name = bundle.getString(ParamArgus.TITLE);
        Log.e(TAG, "initViews: --------- " + FeaturedSubActivity.class.getSimpleName());

        if (data != null) {
            binding.header.tvTitle.setText(name);

            for (int i = 0; i < data.size(); i++) {
                binding.tabLayout.addTab(binding.tabLayout.newTab().setText(data.get(i).getName()));
                /*ArrayList<StickerModel> arrayList = new ArrayList<>();
                stickerModelsMainCatList.add(arrayList);*/
            }
        }
        stickerAdapter = new StickerAppAdapter(stickerModels, FeaturedSubActivity.this, new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (stickerAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                } else {
                    showStickerDialog(stickerModels.get(position).getStickerUrl());
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (stickerAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                }
            }
        });

        int spanCount = 3; // 3 columns
        binding.rvStickers.setLayoutManager(new GridLayoutManager(FeaturedSubActivity.this, spanCount));
        int spacing = 22; // 22px
        boolean includeEdge = true;
        binding.rvStickers.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        binding.rvStickers.setAdapter(stickerAdapter);

        stickerSubAdapter = new StickerSubAdapter(dataSubSub, FeaturedSubActivity.this, new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                startActivity(new Intent(FeaturedSubActivity.this, FeaturedSubDetailsActivity.class)
                        .putExtra(ParamArgus.MODEL, dataSubSub.get(position)));
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        binding.rvSubCat.setLayoutManager(new GridLayoutManager(FeaturedSubActivity.this, 3));
        binding.rvSubCat.addItemDecoration(new GridSpacingItemDecoration(3, spacing, includeEdge));
        binding.rvSubCat.setAdapter(stickerSubAdapter);

        if (Utils.isNetworkAvailable(FeaturedSubActivity.this, false, false)) {
            if (data.get(pos).getSubSubCategoriesCount().equals("0")) {
                getStickers(data.get(pos).getCategoryId(), data.get(pos).getSubCategoryId(), data.get(pos).getId());
            } else {
                getSubSubCategory(data.get(pos).getCategoryId(), data.get(pos).getId());
            }
        }

        AdvUtils.getInstance(this).loadShowBMed(binding.flBanner, AdvUtils.getAdSize(FeaturedSubActivity.this));
    }

    @Override
    protected void setListeners() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pos = tab.getPosition();
                /*if (stickerModelsMainCatList.get(pos).size() == 0) {
                    if (Utils.isNetworkAvailable(FeaturedDetailsActivity.this, false, false)) {
                        //getStickers(data.get(pos).getCategoryId(), data.get(pos).getSubCategoryId(), data.get(pos).getId());
                        getSubSubCategory(data.get(pos).getCategoryId(), data.get(pos).getId());
                    }
                } else {
                    stickerModelsCurrentTab.clear();
                    stickerModelsCurrentTab.addAll(stickerModelsMainCatList.get(pos));
                    stickerAdapter.notifyDataSetChanged();
                }*/

                stickerModels.clear();
                stickerAdapter.notifyDataSetChanged();
                dataSubSub.clear();
                stickerSubAdapter.notifyDataSetChanged();

                if (Utils.isNetworkAvailable(FeaturedSubActivity.this, true, false)) {
                    if (data.get(pos).getSubSubCategoriesCount().equals("0")) {
                        getStickers(data.get(pos).getCategoryId(), data.get(pos).getSubCategoryId(), data.get(pos).getId());
                    } else {
                        getSubSubCategory(data.get(pos).getCategoryId(), data.get(pos).getId());
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.btnBank.setOnClickListener(this);
        binding.btnWa.setOnClickListener(this);
        binding.header.btnBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                onBackPressed();
                break;
            case R.id.btn_bank:
                if (stickerAdapter.isInSelectionMode()) {
                    if (checkStickerSelected()) {
                        ArrayList<StickerModel> stkr = new ArrayList<>();
                        for (int i = 0; i < stickerModels.size(); i++) {
                            if (stickerModels.get(i).isSelected()) {
                                StickerModel stickerModel = new StickerModel(stickerModels.get(i).getId(), stickerModels.get(i).getMainCategoryId(), stickerModels.get(i).getSubCategoryId(), stickerModels.get(i).getSubSubCategoryId(), stickerModels.get(i).getStickerImage(), stickerModels.get(i).getIsActive(), stickerModels.get(i).getCreatedAt(), stickerModels.get(i).getStickerUrl(), false, stickerModels.get(i).getIsAnimated());
                                stkr.add(stickerModel);
                            }
                        }
                        new GetAllFolders(stkr).execute();
                    }
                } else {
                    if (stickerModels.size() > 0) {
                        ArrayList<StickerModel> stkr = new ArrayList<>();
                        for (int i = 0; i < stickerModels.size(); i++) {
                            StickerModel stickerModel = new StickerModel(stickerModels.get(i).getId(), stickerModels.get(i).getMainCategoryId(), stickerModels.get(i).getSubCategoryId(), stickerModels.get(i).getSubSubCategoryId(), stickerModels.get(i).getStickerImage(), stickerModels.get(i).getIsActive(), stickerModels.get(i).getCreatedAt(), stickerModels.get(i).getStickerUrl(), false, stickerModels.get(i).getIsAnimated());
                            stkr.add(stickerModel);
                        }
                        new GetAllFolders(stkr).execute();
                    } else {
                        showToastMsg("No sticker exist");
                    }
                }
                break;
            case R.id.btn_wa:
                if (stickerAdapter.isInSelectionMode()) {
                    if (checkStickerSelected()) {
                        ArrayList<StickerModel> stkr = new ArrayList<>();
                        for (int i = 0; i < stickerModels.size(); i++) {
                            if (stickerModels.get(i).isSelected()) {
                                stkr.add(stickerModels.get(i));
                            }
                        }
                        StickerHandler stickerHandler = new StickerHandler();
                        stickerHandler.buildAndDownloadAdd(stkr, FeaturedSubActivity.this, null, BANK_FEATURE +System.currentTimeMillis()/*+ getResources().getString(R.string.app_name)*/, stkr.get(0).getIsAnimated() == Constants.STICKER_ANIMATED);
                        //new FolderUtils.AddFolder(FeaturedSubActivity.this, "FeaturedSubAnimated_" + FolderUtils.getRandomString(5), true, stkr).execute();

                    }
                } else {
                    if (stickerModels.size() > 0) {
                        StickerHandler stickerHandler = new StickerHandler();
                        stickerHandler.buildAndDownloadAdd(stickerModels, FeaturedSubActivity.this, null, BANK_FEATURE+System.currentTimeMillis() /*+ getResources().getString(R.string.app_name)*/, stickerModels.get(0).getIsAnimated() == Constants.STICKER_ANIMATED);
                        //new FolderUtils.AddFolder(FeaturedSubActivity.this, "FeaturedSub_" + FolderUtils.getRandomString(5), false, stickerModels).execute();

                    } else {
                        showToastMsg("No sticker exist");
                    }
                }
                break;
        }
    }

    private class AddAllStickers extends AsyncTask<Void, Void, Void> {

        ArrayList<StickerModel> stkrModels;
        int folderId;
        String name;

        public AddAllStickers(ArrayList<StickerModel> stickerModels, int folderId, String name) {
            this.stkrModels = stickerModels;
            this.folderId = folderId;
            this.name = name;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //adding to database
            List<StickerModelDB> stickerModelDBList = new ArrayList<>();

            for (int i = 0; i < stkrModels.size(); i++) {
                StickerModelDB db = new StickerModelDB();
                db.setId(stkrModels.get(i).getId());
                db.setFolderId(folderId);
                db.setMainCategoryId(stkrModels.get(i).getMainCategoryId());
                db.setSubCategoryId(stkrModels.get(i).getSubCategoryId());
                db.setSubSubCategoryId(stkrModels.get(i).getSubSubCategoryId());
                db.setStickerImage(stkrModels.get(i).getStickerImage());
                db.setIsActive(stkrModels.get(i).getIsActive());
                db.setCreatedAt(stkrModels.get(i).getCreatedAt());
                db.setStickerUrl(stkrModels.get(i).getStickerUrl());
                stickerModelDBList.add(db);
            }

            DatabaseClient.getInstance(FeaturedSubActivity.this).getAppDatabase().getStickerDao().insert(stickerModelDBList);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new MaterialAlertDialogBuilder(getContext(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setCancelable(false)
                    .setMessage("Sticker added to " + name)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                        stickerAdapter.setInSelectionMode(false);
                        stickerAdapter.removeSelection();
                        stickerAdapter.notifyDataSetChanged();

                        AdvUtils.getInstance(FeaturedSubActivity.this).showInterstitial(false, 0, new InterstitialAdvListener() {
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
                    })
                    .show();


        }
    }

    public boolean checkStickerSelected() {
        for (int i = 0; i < stickerModels.size(); i++) {
            if (stickerModels.get(i).isSelected()) {
                return true;
            }
        }
        showToastMsg("Please select sticker");
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StickerHandler.ADD_PACK) {
            if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                    final String validationError = data.getStringExtra("validation_error");
                    if (validationError != null) {
                        if (BuildConfig.DEBUG) {
                            //validation error should be shown to developer only, not users.
                            MessageDialogFragment.newInstance(R.string.title_validation_error, validationError).show(getSupportFragmentManager(), "validation error");
                        }
                        Log.e("AddStickerPackActivity", "Validation failed:" + validationError);
                    }
                } else {
                    new StickerPackNotAddedMessageFragment().show(getSupportFragmentManager(), "sticker_pack_not_added");
                }
            }
            if (resultCode == Activity.RESULT_OK) {
                int version = Preferences.getInt(Preferences.KEY_VERSION);
                Preferences.setInt(Preferences.KEY_VERSION, ++version);

                showInfoMsgDlg("", "Sticker pack updated.");
                if (stickerAdapter.isInSelectionMode()) {
                    stickerAdapter.removeSelection();
                    stickerAdapter.setInSelectionMode(false);
                    stickerAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    public static final class MessageDialogFragment extends DialogFragment {
        private static final String ARG_TITLE_ID = "title_id";
        private static final String ARG_MESSAGE = "message";

        public static DialogFragment newInstance(@StringRes int titleId, String message) {
            DialogFragment fragment = new BankFolderFragmentOld.MessageDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt(ARG_TITLE_ID, titleId);
            arguments.putString(ARG_MESSAGE, message);
            fragment.setArguments(arguments);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            @StringRes final int title = getArguments().getInt(ARG_TITLE_ID);
            String message = getArguments().getString(ARG_MESSAGE);

            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dismiss();
                        AdvUtils.getInstance(getActivity()).showInterstitial(false, 0, new InterstitialAdvListener() {
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
                    });

            if (title != 0) {
                dialogBuilder.setTitle(title);
            }
            return dialogBuilder.create();
        }
    }

    public static final class StickerPackNotAddedMessageFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setMessage(R.string.add_pack_fail_prompt_update_whatsapp)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dismiss())
                    .setNeutralButton(R.string.add_pack_fail_prompt_update_play_link, (dialog, which) -> {
                        AdvUtils.getInstance(getActivity()).showInterstitial(false, 0, new InterstitialAdvListener() {
                            @Override
                            public void onInterstitialAdLoaded() {
                            }

                            @Override
                            public void onInterstitialAdClosed() {
                                launchWhatsAppPlayStorePage();
                            }

                            @Override
                            public void onContinue() {
                                launchWhatsAppPlayStorePage();
                            }
                        });
                    });

            return dialogBuilder.create();
        }

        private void launchWhatsAppPlayStorePage() {
            if (getActivity() != null) {
                final PackageManager packageManager = getActivity().getPackageManager();
                final boolean whatsAppInstalled = WhitelistCheck.isPackageInstalled(WhitelistCheck.CONSUMER_WHATSAPP_PACKAGE_NAME, packageManager);
                final boolean smbAppInstalled = WhitelistCheck.isPackageInstalled(WhitelistCheck.SMB_WHATSAPP_PACKAGE_NAME, packageManager);
                final String playPackageLinkPrefix = "http://play.google.com/store/apps/details?id=";
                if (whatsAppInstalled && smbAppInstalled) {
                    launchPlayStoreWithUri("https://play.google.com/store/apps/developer?id=WhatsApp+Inc.");
                } else if (whatsAppInstalled) {
                    launchPlayStoreWithUri(playPackageLinkPrefix + WhitelistCheck.CONSUMER_WHATSAPP_PACKAGE_NAME);
                } else if (smbAppInstalled) {
                    launchPlayStoreWithUri(playPackageLinkPrefix + WhitelistCheck.SMB_WHATSAPP_PACKAGE_NAME);
                }
            }
        }

        private void launchPlayStoreWithUri(String uriString) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uriString));
            intent.setPackage("com.android.vending");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.cannot_find_play_store, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showStickerDialog(String stickerUrl) {
        AlertDialog materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .create();
        materialAlertDialogBuilder.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        materialAlertDialogBuilder.getWindow().setLayout((getScreenWidth() * 75) / 100, ViewGroup.LayoutParams.WRAP_CONTENT);

        View view = LayoutInflater.from(this).inflate(R.layout.dlg_show_sticker, null, false);
        SimpleDraweeView iv_sticker = view.findViewById(R.id.iv_sticker);
        AppCompatImageView iv_close = view.findViewById(R.id.iv_close);

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setUri(stickerUrl)
                .setAutoPlayAnimations(true)
                .build();
        iv_sticker.setController(controller);

        iv_close.setOnClickListener(v -> {
            materialAlertDialogBuilder.dismiss();
        });
        materialAlertDialogBuilder.setView(view);
        materialAlertDialogBuilder.show();
    }

    private int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    @Override
    public void onBackPressed() {
        if (stickerAdapter.isInSelectionMode()) {
            stickerAdapter.removeSelection();
            stickerAdapter.setInSelectionMode(false);
            stickerAdapter.notifyDataSetChanged();
        } else {
            super.onBackPressed();
        }
    }

    public void getSubSubCategory(int categoryId, int id) {
        showProgressDialog();
        dataSubSub.clear();
        stickerSubAdapter.notifyDataSetChanged();

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
                                binding.rvSubCat.setVisibility(View.VISIBLE);
                                binding.rvStickers.setVisibility(View.GONE);
                                dataSubSub.addAll(featuredResponse.getData());
                                stickerSubAdapter.notifyDataSetChanged();
                            } else {
                                binding.rvSubCat.setVisibility(View.GONE);
                            }
                        } else {
                            new MaterialAlertDialogBuilder(FeaturedSubActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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

    public void getStickers(int categoryId, int sub_sub_category_id, int sub_category_id) {
        showProgressDialog();
        stickerModels.clear();
        stickerAdapter.setInSelectionMode(false);
        stickerAdapter.notifyDataSetChanged();

        Map<String, Object> map = new HashMap<>();
        map.put("category_id", categoryId);
        map.put("sub_category_id", sub_category_id);
        //map.put("sub_sub_category_id", sub_sub_category_id);// do not pass if not sub sub

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
                                binding.rvStickers.setVisibility(View.VISIBLE);
                                binding.rvSubCat.setVisibility(View.GONE);
                                stickerModels.addAll(stickerResponse.getData());

                                /*stickerModelsMainCatList.get(pos).clear();
                                stickerModelsMainCatList.get(pos).addAll(stickerResponse.getData());*/

                                stickerAdapter.notifyDataSetChanged();
                            }
                        } else {
                            new MaterialAlertDialogBuilder(FeaturedSubActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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

    private class GetAllFolders extends AsyncTask<Void, Void, List<StickerFolder>> {

        ArrayList<StickerModel> stickerModels;

        public GetAllFolders(ArrayList<StickerModel> newArrivalModels) {
            this.stickerModels = newArrivalModels;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<StickerFolder> doInBackground(Void... voids) {
            //adding to database
            List<StickerFolder> arrayList = DatabaseClient.getInstance(FeaturedSubActivity.this).getAppDatabase().getStickerDao().getAllFolders();
            return arrayList;
        }

        @Override
        protected void onPostExecute(List<StickerFolder> stickerFolderList) {
            super.onPostExecute(stickerFolderList);
            showAddStickerDlg(stickerFolderList, stickerModels);
        }
    }

    private void showAddStickerDlg(List<StickerFolder> stickerFolderList, ArrayList<StickerModel> stickerModels) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.AppBottomSheetDialogTheme);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_bank_folder);

        MaterialButton btn_close = bottomSheetDialog.findViewById(R.id.btn_close);
        MaterialButton btn_new_folder = bottomSheetDialog.findViewById(R.id.btn_new_folder);
        RecyclerView rv_folder = bottomSheetDialog.findViewById(R.id.rv_folder);
        EditText edt_folder_name = bottomSheetDialog.findViewById(R.id.edt_folder_name);
        CheckBox cb_animated = bottomSheetDialog.findViewById(R.id.cb_animated);

        if (stickerFolderList == null) {
            stickerFolderList = new ArrayList<>();
        }
        stkrFldr = stickerFolderList;

        FolderAdapter folderAdapter = new FolderAdapter(stickerFolderList, FeaturedSubActivity.this, new OnFolderItemClickListener() {
            @Override
            public void onItemClick(View view, int position, StickerFolder stickerFolder) {
                if (stickerFolder.isIs_animated() && stickerModels.get(0).getIsAnimated() != Constants.STICKER_ANIMATED) {
                    showToastMsg("You are adding simple sticker to animated sticker bank");
                    return;
                } else if (!stickerFolder.isIs_animated() && stickerModels.get(0).getIsAnimated() == Constants.STICKER_ANIMATED) {
                    showToastMsg("You are adding animated sticker to simple sticker bank");
                    return;
                }
                new AddAllStickers(stickerModels, stickerFolder.getId(), stickerFolder.getFolderName()).execute();
                bottomSheetDialog.dismiss();
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        rv_folder.setAdapter(folderAdapter);

        btn_close.setOnClickListener(view -> {
            bottomSheetDialog.dismiss();
        });

        btn_new_folder.setOnClickListener(view -> {
            if (PearlTextUtils.isBlank(edt_folder_name.getText().toString().trim())) {
                showToastMsg("Please enter folder name");
            } else if (edt_folder_name.getText().toString().trim().length() < 3) {
                showToastMsg("Folder name must be 3 character or grater");
            } else if (checkFolderExist(edt_folder_name.getText().toString().trim())) {
                showToastMsg("Folder exist.");
            } else {
                new AddFolder(edt_folder_name.getText().toString().trim(), cb_animated.isChecked(), stickerModels).execute();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    private boolean checkFolderExist(String name) {
        for (int i = 0; i < stkrFldr.size(); i++) {
            if (name.equalsIgnoreCase(stkrFldr.get(i).getFolderName())) {
                return true;
            }
        }
        return false;
    }

    private class AddFolder extends AsyncTask<Void, Void, Void> {

        String folder_name;
        ArrayList<StickerModel> stickerModels;
        boolean is_checked;

        public AddFolder(String folder_name, boolean is_checked, ArrayList<StickerModel> stickerModels) {
            this.folder_name = folder_name;
            this.stickerModels = stickerModels;
            this.is_checked = is_checked;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //adding to database
            StickerFolder stickerFolder = new StickerFolder();
            stickerFolder.setFolderName(folder_name);
            stickerFolder.setIs_animated(is_checked);
            DatabaseClient.getInstance(FeaturedSubActivity.this).getAppDatabase().getStickerDao().insertFolder(stickerFolder);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new GetAllFolders(stickerModels).execute();
        }
    }
}