package com.stickers.bank.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.R;
import com.stickers.bank.data.listeners.InterstitialAdvListener;
import com.stickers.bank.data.listeners.OnItemClickListener;
import com.stickers.bank.data.model.DataResponse;
import com.stickers.bank.data.model.FeaturedModel;
import com.stickers.bank.data.model.FeaturedResponse;
import com.stickers.bank.data.webservices.APIRequest;
import com.stickers.bank.data.webservices.ResponseCallback;
import com.stickers.bank.databinding.FragmentFeaturedBinding;
import com.stickers.bank.ui.activity.FeaturedSubActivity;
import com.stickers.bank.ui.adapters.FeaturedAdapter;
import com.stickers.bank.ui.base.BaseFragment;
import com.stickers.bank.utils.AdvUtils;
import com.stickers.bank.utils.ParamArgus;
import com.stickers.bank.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FeaturedFragment extends BaseFragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private FragmentFeaturedBinding binding;
    FeaturedModel data;
    ArrayList<FeaturedModel> featuredModels = new ArrayList<>();
    FeaturedAdapter featuredAdapter;
    DataResponse dataResponse;

    public FeaturedFragment() {
        // Required empty public constructor
    }

    public static FeaturedFragment newInstance(FeaturedModel data) {
        FeaturedFragment fragment = new FeaturedFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, data);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFeaturedBinding.inflate(inflater, container, false);
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

        /*String jsonFileString = getJsonFromAssets(getActivity(), "featured.json");
        Gson gson = new Gson();
        Type listUserType = new TypeToken<FeaturedResponse>() {
        }.getType();
        FeaturedResponse featured = gson.fromJson(jsonFileString, listUserType);
        featuredModels.addAll(featured.getData());*/

        //binding.rvFeatured.addItemDecoration(new EqualSpacingItemDecoration(22, EqualSpacingItemDecoration.VERTICAL));
        featuredAdapter = new FeaturedAdapter(featuredModels, getActivity(), new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                /*if (dataResponse != null) {
                    DataResponse.Data data = new DataResponse().new Data();
                    data.setId(featuredModels.get(position).getId());

                    int index = dataResponse.getData().indexOf(data);
                    if (index != -1) {
                        DataResponse.Data dataNew = dataResponse.getData().get(index);
                        if (dataNew.getS_list().size() > 1) {

                        } else {
                            startActivity(new Intent(getActivity(), FeaturedDetailsActivity.class)
                                    .putExtra(ParamArgus.MODEL, dataNew)
                                    .putExtra(ParamArgus.MULTI_LANG, false));
                        }
                    }
                }*/
                getSubCategory(featuredModels.get(position).getId(), featuredModels.get(position).getName());
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        binding.rvFeatured.setAdapter(featuredAdapter);

        /*String dataString = getJsonFromAssets(getActivity(), "data.json");
        Type dataType = new TypeToken<DataResponse>() {
        }.getType();
        dataResponse = gson.fromJson(dataString, dataType);*/

        if (Utils.isNetworkAvailable(getActivity(), false, false)) {
            getFeatured();
        }
    }

    @Override
    public void setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            if (Utils.isNetworkAvailable(getActivity(), false, false)) {
                getFeatured();
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

    public void getFeatured() {
        showProgressDialog();

        APIRequest apiRequest = new APIRequest(null);
        apiRequest.getFeatured(new ResponseCallback() {
            @Override
            public void ResponseSuccessCallBack(Object object) {
                try {
                    hideDialog();
                    if (object instanceof FeaturedResponse) {
                        FeaturedResponse featuredResponse = (FeaturedResponse) object;
                        if (featuredResponse != null && featuredResponse.isSuccess()) {
                            if (featuredResponse.getData() != null && featuredResponse.getData().size() > 0) {
                                featuredModels.clear();
                                featuredModels.addAll(featuredResponse.getData());
                                featuredAdapter.notifyDataSetChanged();
                            }
                        } else {
                            new MaterialAlertDialogBuilder(getActivity(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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

    public void getSubCategory(int id, String name) {
        showProgressDialog();

        Map<String, Object> map = new HashMap<>();
        map.put("category_id", id);

        APIRequest apiRequest = new APIRequest(null);
        apiRequest.getSubCategory(map, new ResponseCallback() {
            @Override
            public void ResponseSuccessCallBack(Object object) {
                try {
                    hideDialog();
                    if (object instanceof FeaturedResponse) {
                        FeaturedResponse featuredResponse = (FeaturedResponse) object;
                        if (featuredResponse != null && featuredResponse.isSuccess()) {
                            if (featuredResponse.getData() != null && featuredResponse.getData().size() > 0) {
                                AdvUtils.getInstance(getActivity()).showInterstitialAlternate(new InterstitialAdvListener() {
                                    @Override
                                    public void onInterstitialAdLoaded() {

                                    }

                                    @Override
                                    public void onInterstitialAdClosed() {
                                        startActivity(new Intent(getActivity(), FeaturedSubActivity.class)
                                                .putExtra(ParamArgus.MODEL, featuredResponse.getData())
                                                .putExtra(ParamArgus.TITLE, name));
                                    }

                                    @Override
                                    public void onContinue() {
                                        startActivity(new Intent(getActivity(), FeaturedSubActivity.class)
                                                .putExtra(ParamArgus.MODEL, featuredResponse.getData())
                                                .putExtra(ParamArgus.TITLE, name));
                                    }
                                });


                            }
                        } else {
                            new MaterialAlertDialogBuilder(getActivity(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
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