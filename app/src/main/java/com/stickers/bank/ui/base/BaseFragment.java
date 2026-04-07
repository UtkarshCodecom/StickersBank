package com.stickers.bank.ui.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.R;

public abstract class BaseFragment extends Fragment {

    public final String TAG = getFragmentContext().getClass().getSimpleName();

    ProgressDialog mProgressDialog;

    public abstract void initViews();

    public abstract void setListeners();

    protected abstract Context getActContext();

    protected abstract Fragment getFragmentContext();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setEnterTransition(new MaterialFadeThrough());
    }

    protected void showProgressDialog() {
        try {
            if (mProgressDialog == null)
                mProgressDialog = ProgressDialog.show(getActContext(), "", "Please wait...", false, false);
            if (!mProgressDialog.isShowing())
                mProgressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void hideDialog() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showToastMsg(String msg) {
        Toast.makeText(getActContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public void showInfoMsgDlg(String title, String msg) {
        new MaterialAlertDialogBuilder(getActContext(),R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {

                })
                .show();
    }

    public static void hideKeyBoard(View view, Context mActivity) {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showKeyBoard(View v, Activity mActivity) {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, 0);
    }
}
