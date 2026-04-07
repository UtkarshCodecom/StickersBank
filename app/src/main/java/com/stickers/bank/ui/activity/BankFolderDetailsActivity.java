package com.stickers.bank.ui.activity;

import static com.stickers.bank.data.common.Constants.BANK;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.BuildConfig;
import com.stickers.bank.R;
import com.stickers.bank.data.common.Constants;
import com.stickers.bank.data.database.DatabaseClient;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.database.entity.StickerModelDB;
import com.stickers.bank.data.listeners.InterstitialAdvListener;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.StickerModel;
import com.stickers.bank.databinding.ActivityBankFolderDetailsBinding;
import com.stickers.bank.ui.adapters.BankAdapter;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.utils.AdvUtils;
import com.stickers.bank.utils.GridSpacingItemDecoration;
import com.stickers.bank.utils.ParamArgus;
import com.stickers.bank.utils.Preferences;
import com.stickers.bank.utils.StickerHandler;
import com.stickers.bank.utils.WhitelistCheck;

import java.util.ArrayList;
import java.util.List;

public class BankFolderDetailsActivity extends BaseActivity<ActivityBankFolderDetailsBinding> implements View.OnClickListener {
    StickerFolder stickerFolder;

    ArrayList<StickerModel> stickerModels = new ArrayList<>();
    BankAdapter bankAdapter;

    @Override
    public int getLayoutId() {
        return R.layout.activity_bank_folder_details;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            stickerFolder = (StickerFolder) bundle.getSerializable(ParamArgus.MODEL);
            if (stickerFolder != null) {
                binding.header.tvTitle.setText(stickerFolder.getFolderName());
            }
        }
        binding.header.btnBack.setVisibility(View.VISIBLE);

        bankAdapter = new BankAdapter(stickerModels, this, new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (bankAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                } else {
                    showStickerDialog(stickerModels.get(position).getStickerUrl());
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (bankAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.rvBank.setLayoutManager(new GridLayoutManager(this, 4));
        int spanCount = 4; // 3 columns
        int spacing = 22; // 22px
        boolean includeEdge = true;
        binding.rvBank.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        binding.rvBank.setAdapter(bankAdapter);
        new GetAllStickers().execute();
    }

    @Override
    protected void setListeners() {
        binding.header.btnBack.setOnClickListener(this);
        binding.btnAddWa.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                onBackPressed();
                break;
            case R.id.btn_add_wa:
                if (stickerModels.size() > 0) {
                    StickerHandler stickerHandler = new StickerHandler();
                    stickerHandler.buildAndDownloadAdd(stickerModels, BankFolderDetailsActivity.this, null, BANK + stickerFolder.getFolderName(), stickerFolder.isIs_animated());
                } else {
                    showToastMsg("No sticker exist");
                }
                break;
        }
    }

    private class GetAllStickers extends AsyncTask<Void, Void, List<StickerModelDB>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<StickerModelDB> doInBackground(Void... voids) {
            //adding to database
            List<StickerModelDB> arrayList = DatabaseClient.getInstance(BankFolderDetailsActivity.this).getAppDatabase().getStickerDao().getStickerByID(stickerFolder.getId());
            return arrayList;
        }

        @Override
        protected void onPostExecute(List<StickerModelDB> stickerModelDBList) {
            super.onPostExecute(stickerModelDBList);
            if (stickerModelDBList != null && stickerModelDBList.size() > 0) {
                stickerModels.clear();
                for (int i = 0; i < stickerModelDBList.size(); i++) {
                    StickerModelDB db = stickerModelDBList.get(i);
                    StickerModel stickerModel = new StickerModel(db.getId(), db.getMainCategoryId(), db.getSubCategoryId(), db.getSubSubCategoryId(), db.getStickerImage(), db.getIsActive(), db.getCreatedAt(), db.getStickerUrl(), false, db.getIs_animated());
                    stickerModels.add(stickerModel);
                }
                bankAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ADD_PACK) {
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
            } else if (resultCode == Activity.RESULT_OK) {
                int version = Preferences.getInt(Preferences.KEY_VERSION);
                Preferences.setInt(Preferences.KEY_VERSION, ++version);

                new MaterialAlertDialogBuilder(BankFolderDetailsActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                        .setMessage("Sticker pack updated.")
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                            AdvUtils.getInstance(BankFolderDetailsActivity.this).showInterstitialAlternate(new InterstitialAdvListener() {
                                @Override
                                public void onInterstitialAdLoaded() {

                                }

                                @Override
                                public void onInterstitialAdClosed() {
                                    deleteFolderAfterAdd();
                                }

                                @Override
                                public void onContinue() {
                                    deleteFolderAfterAdd();
                                }
                            });
                        }).show();
            }
        }
    }

    private void deleteFolderAfterAdd() {
        new DeleteSticker().execute();
    }

    public static final class MessageDialogFragment extends DialogFragment {
        private static final String ARG_TITLE_ID = "title_id";
        private static final String ARG_MESSAGE = "message";

        public static DialogFragment newInstance(@StringRes int titleId, String message) {
            DialogFragment fragment = new MessageDialogFragment();
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

            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog).setMessage(message).setCancelable(true).setPositiveButton(android.R.string.ok, (dialog, which) -> {
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
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog).setMessage(R.string.add_pack_fail_prompt_update_whatsapp).setCancelable(true).setPositiveButton(android.R.string.ok, (dialog, which) -> dismiss()).setNeutralButton(R.string.add_pack_fail_prompt_update_play_link, (dialog, which) -> {
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
        AlertDialog materialAlertDialogBuilder = new MaterialAlertDialogBuilder(BankFolderDetailsActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog).create();
        materialAlertDialogBuilder.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        materialAlertDialogBuilder.getWindow().setLayout((getScreenWidth() * 75) / 100, ViewGroup.LayoutParams.WRAP_CONTENT);

        View view = LayoutInflater.from(BankFolderDetailsActivity.this).inflate(R.layout.dlg_show_sticker, null, false);
        SimpleDraweeView iv_sticker = view.findViewById(R.id.iv_sticker);
        AppCompatImageView iv_close = view.findViewById(R.id.iv_close);

        DraweeController controller = Fresco.newDraweeControllerBuilder().setUri(stickerUrl).setAutoPlayAnimations(true).build();
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

    private class DeleteSticker extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //adding to database
            DatabaseClient.getInstance(BankFolderDetailsActivity.this).getAppDatabase().getStickerDao().deleteStickerByFolderId(stickerFolder.getId());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new DeleteFolder().execute();
        }
    }

    private class DeleteFolder extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //adding to database
            DatabaseClient.getInstance(BankFolderDetailsActivity.this).getAppDatabase().getStickerDao().deleteFolderById(stickerFolder.getId());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            onBackPressed();
        }
    }
}