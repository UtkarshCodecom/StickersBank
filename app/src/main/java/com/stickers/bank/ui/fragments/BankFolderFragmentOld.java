package com.stickers.bank.ui.fragments;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.BuildConfig;
import com.stickers.bank.R;
import com.stickers.bank.data.database.DatabaseClient;
import com.stickers.bank.data.database.entity.StickerModelDB;
import com.stickers.bank.data.listeners.InterstitialAdvListener;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.StickerModel;
import com.stickers.bank.data.model.StickerPackApi;
import com.stickers.bank.databinding.FragmentBankFolderOldBinding;
import com.stickers.bank.ui.adapters.BankAdapter;
import com.stickers.bank.ui.base.BaseFragment;
import com.stickers.bank.utils.AdvUtils;
import com.stickers.bank.utils.GridSpacingItemDecoration;
import com.stickers.bank.utils.Preferences;
import com.stickers.bank.utils.StickerHandler;
import com.stickers.bank.utils.Utils;
import com.stickers.bank.utils.WhitelistCheck;
import com.stickers.bank.utils.WhitelistCheckApi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class BankFolderFragmentOld extends BaseFragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private FragmentBankFolderOldBinding binding;
    FeaturedModel data;
    ArrayList<StickerModel> stickerModels = new ArrayList<>();
    BankAdapter bankAdapter;

    //private StickerPackApi stickerPack;
    StickerPackApi stickerPackApi;
    private WhiteListCheckAsyncTask whiteListCheckAsyncTask;

    public static final int ADD_PACK = 201;

    public BankFolderFragmentOld() {
        // Required empty public constructor
    }

    public static BankFolderFragmentOld newInstance(FeaturedModel data) {
        BankFolderFragmentOld fragment = new BankFolderFragmentOld();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, data);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBankFolderOldBinding.inflate(inflater, container, false);
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

        /*String jsonFileString = getJsonFromAssets(getActivity(), "bank.json");
        Gson gson = new Gson();
        Type listUserType = new TypeToken<BankResponse>() {
        }.getType();
        BankResponse featured = gson.fromJson(jsonFileString, listUserType);
        stickerModels.addAll(featured.getData());*/

        bankAdapter = new BankAdapter(stickerModels, getActivity(), new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (bankAdapter.isInSelectionMode()) {
                    binding.clBtm.setVisibility(View.VISIBLE);
                } else {
                    showStickerDialog(stickerModels.get(position).getStickerUrl());
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (bankAdapter.isInSelectionMode()) {
                    binding.clBtm.setVisibility(View.VISIBLE);
                }
            }
        });

        int spanCount = 4; // 3 columns
        int spacing = 22; // 22px
        binding.rvBank.setLayoutManager(new GridLayoutManager(requireActivity(), spanCount));
        boolean includeEdge = true;
        binding.rvBank.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        binding.rvBank.setAdapter(bankAdapter);
    }

    @Override
    public void setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            if (Utils.isNetworkAvailable(getActivity(), false, false)) {
                new GetAllStickers().execute();
            }
        });

        binding.btnClear.setOnClickListener(this);
        binding.btnAddWa.setOnClickListener(this);
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
            case R.id.btn_clear:
                /*for (int i = 0; i < stickerModels.size(); i++) {
                    stickerModels.get(i).setSelected(false);
                }*/
                bankAdapter.removeSelection();
                bankAdapter.setInSelectionMode(false);
                bankAdapter.notifyDataSetChanged();
                binding.clBtm.setVisibility(View.GONE);
                break;
            case R.id.btn_add_wa:
                if (bankAdapter.isInSelectionMode()) {
                    if (checkStickerSelected()) {
                        ArrayList<StickerModel> stkr = new ArrayList<>();
                        for (int i = 0; i < stickerModels.size(); i++) {
                            if (stickerModels.get(i).isSelected()) {
                                stkr.add(stickerModels.get(i));
                            }
                        }
                        StickerHandler stickerHandler = new StickerHandler();
                        stickerHandler.buildAndDownloadAdd(stkr, getActivity(), BankFolderFragmentOld.this, getResources().getString(R.string.app_name),false);
                    }
                } else {
                    if (stickerModels.size() > 0) {
                        StickerHandler stickerHandler = new StickerHandler();
                        stickerHandler.buildAndDownloadAdd(stickerModels, getActivity(), BankFolderFragmentOld.this, getResources().getString(R.string.app_name),false);
                    } else {
                        showToastMsg("No sticker exist");
                    }
                }
                break;
        }
    }

    static class WhiteListCheckAsyncTask extends AsyncTask<StickerPackApi, Void, Boolean> {

        private final WeakReference<BankFolderFragmentOld> stickerPackDetailsActivityWeakReference;

        WhiteListCheckAsyncTask(BankFolderFragmentOld fragment) {
            this.stickerPackDetailsActivityWeakReference = new WeakReference<>(fragment);
        }

        @Override
        protected final Boolean doInBackground(StickerPackApi... stickerPacks) {
            StickerPackApi stickerPack = stickerPacks[0];
            final Fragment fragment = stickerPackDetailsActivityWeakReference.get();
            //noinspection SimplifiableIfStatement
            if (fragment == null) {
                return false;
            }
            return WhitelistCheckApi.isWhitelisted(fragment.getActivity(), stickerPack.identifier);
        }

        @Override
        protected void onPostExecute(Boolean isWhitelisted) {
            final BankFolderFragmentOld fragment = stickerPackDetailsActivityWeakReference.get();
            if (fragment != null) {
                fragment.updateAddUI(isWhitelisted);
            }
        }
    }

    private void updateAddUI(Boolean isWhitelisted) {
        if (isWhitelisted) {
            // TODO: 09-09-2022 update this
            //addButton.setVisibility(View.GONE);
            //alreadyAddedText.setVisibility(View.VISIBLE);
        } else {
            //addButton.setVisibility(View.VISIBLE);
            //alreadyAddedText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_PACK) {
            if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                    final String validationError = data.getStringExtra("validation_error");
                    if (validationError != null) {
                        if (BuildConfig.DEBUG) {
                            //validation error should be shown to developer only, not users.
                            MessageDialogFragment.newInstance(R.string.title_validation_error, validationError).show(getChildFragmentManager(), "validation error");
                        }
                        Log.e("AddStickerPackActivity", "Validation failed:" + validationError);
                    }
                } else {
                    new StickerPackNotAddedMessageFragment().show(getChildFragmentManager(), "sticker_pack_not_added");
                }
            } else if (resultCode == Activity.RESULT_OK) {
                int version = Preferences.getInt(Preferences.KEY_VERSION);
                Preferences.setInt(Preferences.KEY_VERSION, ++version);

                new MaterialAlertDialogBuilder(getActContext(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                        .setMessage("Sticker pack updated.")
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                            AdvUtils.getInstance(getActivity()).showInterstitialAlternate(new InterstitialAdvListener() {
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

            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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

    public boolean checkStickerSelected() {
        for (int i = 0; i < stickerModels.size(); i++) {
            if (stickerModels.get(i).isSelected()) {
                return true;
            }
        }
        showToastMsg("Please select sticker");
        return false;
    }

    private class GetAllStickers extends AsyncTask<Void, Void, List<StickerModelDB>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<StickerModelDB> doInBackground(Void... voids) {
            //adding to database
            List<StickerModelDB> arrayList = DatabaseClient.getInstance(getActivity()).getAppDatabase().getStickerDao().getAllStickers();
            return arrayList;
        }

        @Override
        protected void onPostExecute(List<StickerModelDB> stickerModelDBList) {
            super.onPostExecute(stickerModelDBList);
            if (stickerModelDBList != null && stickerModelDBList.size() > 0) {
                stickerModels.clear();
                for (int i = 0; i < stickerModelDBList.size(); i++) {
                    StickerModelDB db = stickerModelDBList.get(i);
                    StickerModel stickerModel = new StickerModel(db.getId(), db.getMainCategoryId(), db.getSubCategoryId(), db.getSubSubCategoryId(), db.getStickerImage(), db.getIsActive(), db.getCreatedAt(), db.getStickerUrl(), false,db.getIs_animated());
                    stickerModels.add(stickerModel);
                }
                bankAdapter.notifyDataSetChanged();
            }
        }
    }

    String getJsonFromAssets(Context context, String fileName) {
        String jsonString;
        try {
            InputStream is = context.getAssets().open(fileName);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return jsonString;
    }

    @Override
    public void onResume() {
        super.onResume();
        new GetAllStickers().execute();
    }

    private void showStickerDialog(String stickerUrl) {
        AlertDialog materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .create();
        materialAlertDialogBuilder.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        materialAlertDialogBuilder.getWindow().setLayout((getScreenWidth() * 75) / 100, ViewGroup.LayoutParams.WRAP_CONTENT);

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dlg_show_sticker, null, false);
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
}