package com.stickers.bank.ui.fragments;

import static com.stickers.bank.data.common.Constants.BANK_NEWARRIVAL;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.stickers.bank.data.model.NewArrivalResponse;
import com.stickers.bank.data.model.StickerModel;
import com.stickers.bank.data.webservices.APIRequest;
import com.stickers.bank.data.webservices.ResponseCallback;
import com.stickers.bank.databinding.FragmentNewArrivalBinding;
import com.stickers.bank.ui.activity.FeaturedSubActivity;
import com.stickers.bank.ui.adapters.FolderAdapter;
import com.stickers.bank.ui.adapters.NewArrivalAdapter;
import com.stickers.bank.ui.base.BaseFragment;
import com.stickers.bank.utils.AdvUtils;
import com.stickers.bank.utils.GridSpacingItemDecoration;
import com.stickers.bank.utils.PearlTextUtils;
import com.stickers.bank.utils.Preferences;
import com.stickers.bank.utils.StickerHandler;
import com.stickers.bank.utils.Utils;
import com.stickers.bank.utils.WhitelistCheck;

import java.util.ArrayList;
import java.util.List;

public class NewArrivalFragment extends BaseFragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private FragmentNewArrivalBinding binding;
    FeaturedModel data;
    ArrayList<NewArrivalResponse.NewArrivalModel> newArrivalModels = new ArrayList<>();
    NewArrivalAdapter newArrivalAdapter;
    List<StickerFolder> stkrFldr;

    public NewArrivalFragment() {
        // Required empty public constructor
    }

    public static NewArrivalFragment newInstance(FeaturedModel data) {
        NewArrivalFragment fragment = new NewArrivalFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, data);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNewArrivalBinding.inflate(inflater, container, false);
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

        newArrivalAdapter = new NewArrivalAdapter(newArrivalModels, getActivity(), new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (newArrivalAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                } else {
                    showStickerDialog(newArrivalModels.get(position).getStickerUrl());
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (newArrivalAdapter.isInSelectionMode()) {
                    //binding.clBtm.setVisibility(View.VISIBLE);
                }
            }
        });
        int spanCount = 3; // 3 columns
        int spacing = 22; // 22px
        binding.rvNewArrival.setLayoutManager(new GridLayoutManager(requireActivity(), spanCount));

        boolean includeEdge = true;
        binding.rvNewArrival.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        binding.rvNewArrival.setAdapter(newArrivalAdapter);

        if (Utils.isNetworkAvailable(getActivity(), false, false)) {
            getNewArrivalStickers();
        }
    }

    @Override
    public void setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            if (Utils.isNetworkAvailable(getActivity(), false, false)) {
                getNewArrivalStickers();
            }
        });
        binding.btnBank.setOnClickListener(this);
        binding.btnWa.setOnClickListener(this);
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_bank:
                AdvUtils.getInstance(getActivity()).showInterstitialAlternate(new InterstitialAdvListener() {
                    @Override
                    public void onInterstitialAdLoaded() {

                    }

                    @Override
                    public void onInterstitialAdClosed() {
                        if (newArrivalAdapter.isInSelectionMode()) {
                            if (checkStickerSelected()) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    if (newArrivalModels.get(i).isSelected()) {
                                        StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                        stkr.add(stickerModel);
                                    }
                                }
                                new GetAllFolders(stkr).execute();
                            }
                        } else {
                            if (newArrivalModels.size() > 0) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                    stkr.add(stickerModel);
                                }
                                new GetAllFolders(stkr).execute();
                            } else {
                                showToastMsg("No sticker exist");
                            }
                        }
                    }

                    @Override
                    public void onContinue() {
                        if (newArrivalAdapter.isInSelectionMode()) {
                            if (checkStickerSelected()) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    if (newArrivalModels.get(i).isSelected()) {
                                        StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                        stkr.add(stickerModel);
                                    }
                                }
                                new GetAllFolders(stkr).execute();
                            }
                        } else {
                            if (newArrivalModels.size() > 0) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                    stkr.add(stickerModel);
                                }
                                new GetAllFolders(stkr).execute();
                            } else {
                                showToastMsg("No sticker exist");
                            }
                        }
                    }
                });
                break;

            case R.id.btn_wa:
                AdvUtils.getInstance(getActivity()).showInterstitialAlternate(new InterstitialAdvListener() {
                    @Override
                    public void onInterstitialAdLoaded() {

                    }

                    @Override
                    public void onInterstitialAdClosed() {
                        if (newArrivalAdapter.isInSelectionMode()) {
                            if (checkStickerSelected()) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    if (newArrivalModels.get(i).isSelected()) {
                                        StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                        stkr.add(stickerModel);
                                    }
                                }
                                StickerHandler stickerHandler = new StickerHandler();
                                stickerHandler.buildAndDownloadAdd(stkr, getActivity(), NewArrivalFragment.this, BANK_NEWARRIVAL+System.currentTimeMillis() /*+ getResources().getString(R.string.app_name)*/ + "N", stkr.get(0).getIsAnimated() == Constants.STICKER_ANIMATED);
                                // new FolderUtils.AddFolder(getActivity(), "NewArrivalAnimated_" + FolderUtils.getRandomString(5), true, stkr).execute();

                            }
                        } else {
                            if (newArrivalModels.size() > 0) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                    stkr.add(stickerModel);
                                }
                                StickerHandler stickerHandler = new StickerHandler();
                                stickerHandler.buildAndDownloadAdd(stkr, getActivity(), NewArrivalFragment.this, BANK_NEWARRIVAL+System.currentTimeMillis() /*+ getResources().getString(R.string.app_name)*/, stkr.get(0).getIsAnimated() == Constants.STICKER_ANIMATED);
                                // new FolderUtils.AddFolder(getActivity(), "NewArrival_" + FolderUtils.getRandomString(5), false, stkr).execute();

                            } else {
                                showToastMsg("No sticker exist");
                            }
                        }
                    }

                    @Override
                    public void onContinue() {
                        if (newArrivalAdapter.isInSelectionMode()) {
                            if (checkStickerSelected()) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    if (newArrivalModels.get(i).isSelected()) {
                                        StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                        stkr.add(stickerModel);
                                    }
                                }
                                StickerHandler stickerHandler = new StickerHandler();
                                stickerHandler.buildAndDownloadAdd(stkr, getActivity(), NewArrivalFragment.this, BANK_NEWARRIVAL+System.currentTimeMillis() /*+ getResources().getString(R.string.app_name)*/ + "N", stkr.get(0).getIsAnimated() == Constants.STICKER_ANIMATED);
                                //new FolderUtils.AddFolder(getActivity(), "NewArrivalAnimated" + FolderUtils.getRandomString(5), true, stkr).execute();

                            }
                        } else {
                            if (newArrivalModels.size() > 0) {
                                ArrayList<StickerModel> stkr = new ArrayList<>();
                                for (int i = 0; i < newArrivalModels.size(); i++) {
                                    StickerModel stickerModel = new StickerModel(newArrivalModels.get(i).getId(), newArrivalModels.get(i).getMainCategoryId(), newArrivalModels.get(i).getSubCategoryId(), newArrivalModels.get(i).getSubSubCategoryId(), newArrivalModels.get(i).getStickerImage(), newArrivalModels.get(i).getIsActive(), newArrivalModels.get(i).getCreatedAt(), newArrivalModels.get(i).getStickerUrl(), false, newArrivalModels.get(i).getIsAnimated());
                                    stkr.add(stickerModel);
                                }
                                StickerHandler stickerHandler = new StickerHandler();
                                stickerHandler.buildAndDownloadAdd(stkr, getActivity(), NewArrivalFragment.this, BANK_NEWARRIVAL+System.currentTimeMillis() /*+ getResources().getString(R.string.app_name)*/, stkr.get(0).getIsAnimated() == Constants.STICKER_ANIMATED);
                                //new FolderUtils.AddFolder(getActivity(), "NewArrival" + FolderUtils.getRandomString(5), true, stkr).execute();

                            } else {
                                showToastMsg("No sticker exist");
                            }
                        }
                    }
                });

                break;
        }
    }

    public void getNewArrivalStickers() {
        showProgressDialog();

        APIRequest apiRequest = new APIRequest(null);
        apiRequest.getNewArrivalStickers(new ResponseCallback() {
            @Override
            public void ResponseSuccessCallBack(Object object) {
                try {
                    hideDialog();
                    if (object instanceof NewArrivalResponse) {
                        NewArrivalResponse newArrivalResponse = (NewArrivalResponse) object;
                        if (newArrivalResponse != null && newArrivalResponse.isSuccess()) {
                            if (newArrivalResponse.getData() != null && newArrivalResponse.getData().size() > 0) {
                                newArrivalModels.clear();
                                newArrivalModels.addAll(newArrivalResponse.getData());
                                newArrivalAdapter.notifyDataSetChanged();
                            }
                        } else {
                            new MaterialAlertDialogBuilder(getActivity(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                                    .setMessage(newArrivalResponse.getMessage())
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

    public boolean checkStickerSelected() {
        for (int i = 0; i < newArrivalModels.size(); i++) {
            if (newArrivalModels.get(i).isSelected()) {
                return true;
            }
        }
        showToastMsg("Please select sticker");
        return false;
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

            DatabaseClient.getInstance(getActivity()).getAppDatabase().getStickerDao().insert(stickerModelDBList);
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
                        newArrivalAdapter.setInSelectionMode(false);
                        newArrivalAdapter.removeSelection();
                        newArrivalAdapter.notifyDataSetChanged();
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
                    })
                    .show();


        }
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
                            FeaturedSubActivity.MessageDialogFragment.newInstance(R.string.title_validation_error, validationError).show(getChildFragmentManager(), "validation error");
                        }
                        Log.e("AddStickerPackActivity", "Validation failed:" + validationError);
                    }
                } else {
                    new FeaturedSubActivity.StickerPackNotAddedMessageFragment().show(getChildFragmentManager(), "sticker_pack_not_added");
                }
            }
            if (resultCode == Activity.RESULT_OK) {
                int version = Preferences.getInt(Preferences.KEY_VERSION);
                Preferences.setInt(Preferences.KEY_VERSION, ++version);
                showInfoMsgDlg("", "Sticker pack updated.");

                if (newArrivalAdapter.isInSelectionMode()) {
                    newArrivalAdapter.removeSelection();
                    newArrivalAdapter.setInSelectionMode(false);
                    newArrivalAdapter.notifyDataSetChanged();
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
        AlertDialog materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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

        FolderAdapter folderAdapter = new FolderAdapter(stickerFolderList, getActivity(), new OnFolderItemClickListener() {
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
                //new FolderUtils.AddFolder(getActivity(),edt_folder_name.getText().toString().trim(), cb_animated.isChecked(), stickerModels).execute();

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
            DatabaseClient.getInstance(getActivity()).getAppDatabase().getStickerDao().insertFolder(stickerFolder);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new GetAllFolders(stickerModels).execute();

            //List<StickerFolder> stickerFolderList = (List<StickerFolder>) new FolderUtils.GetAllFolders(getActivity(), stickerModels).execute();
        }
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
            List<StickerFolder> arrayList = DatabaseClient.getInstance(getActivity()).getAppDatabase().getStickerDao().getAllFolders();
            return arrayList;
        }

        @Override
        protected void onPostExecute(List<StickerFolder> stickerFolderList) {
            super.onPostExecute(stickerFolderList);
            showAddStickerDlg(stickerFolderList, stickerModels);
        }
    }
}