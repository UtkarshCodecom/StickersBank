package com.stickers.bank.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import com.stickers.bank.R;
import com.stickers.bank.databinding.ActivityFaqsBinding;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.utils.AdvUtils;

public class FaqsActivity extends BaseActivity<ActivityFaqsBinding> implements View.OnClickListener {

    @Override
    public int getLayoutId() {
        return R.layout.activity_faqs;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        binding.header.btnBack.setVisibility(View.VISIBLE);
        binding.header.tvTitle.setText(getString(R.string.faq_s));
        AdvUtils.getInstance(this).loadShowBMed(binding.flBanner, AdvUtils.getAdSize(FaqsActivity.this));

        String sourceString = "<p><strong>&nbsp; !!! STICKERS &nbsp;WILL &nbsp;BE &nbsp;UPDATED &nbsp;DAILY !!!</strong></p>\n" +
                "<p><br></p>\n" +
                "<p><font color='#636726'>Do StickersBank collect any personal information?</font></p>\n" +
                "<p><font color='white'>NO</font></p>\n" +
                "<p><br></p>\n" +
                "<p><font color='#636726'>Do StickersBank charge any money from the users?</font></p>\n" +
                "<p><font color='white'>NO</font></p>\n" +
                "<p><br></p>\n" +
                "<p><font color='#636726'>Is StickersBank present in both android and ios?</font></p>\n" +
                "<p><font color='white'>YES</font></p>\n" +
                "<p><br></p>\n" +
                "<p><font color='#636726'>What is NEWARRIVAL?</font></p>\n" +
                "<p><font color='white'>Newly updated stickers time to time of all categories</font></p>\n" +
                "<p><br></p>\n" +
                "<p><font color='#636726'>What is BANK?</font></p>\n" +
                "<p><font color='white'>Your random collectios</font></p>\n" +
                "<p><br></p>\n" +
                "<p><font color='#636726'>Can stickers be directly dump to whatsapp without adding to bank?</font></p>\n" +
                "<p><font color='white'>YES</font></p>\n" +
                "<p><br></p>\n" +
                "<p><font color='#636726'>Will the collection remains same in the bank after dumping to whatsapp?</p>\n" +
                "<p><font color='white'>NO</font></p>\n" +
                "<p><br></p>\n" +
                /*"<p><font color='#636726'>How many Maximum stickers can be added in a bank randomly?</font></p>\n" +
                "<p><font color='white'>Yourwish</font></p>\n" +
                "<p><br></p>\n" +*/
                "<p><font color='#636726'>How many Minimum stickers can be added in a bank randomly?</font></p>\n" +
                "<p><font color='white'>Minimum 3</font></p>" +
                "<p><font color='#636726'>What if stickers bank give eror while dumping?</font></p>\n" +
                "<p><font color='white'>Clear cache & data in the app settings</font></p>"+
                "<p><font color='#636726'>Can an animated and normal stickers dump added directly to whattsapp?</font></p>\n" +
                "<p><font color='white'>No animated has to be dump Separately</font></p>"+
                "<p><font color='#636726'>how many stickers can be added at once?</font></p>\n" +
                "<p><font color='white'>3 to 30 stickers can be added at once</font></p>";
        binding.tvContent.setText(Html.fromHtml(sourceString));
    }

    @Override
    protected void setListeners() {
        binding.header.btnBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                onBackPressed();
                break;
        }
    }
}