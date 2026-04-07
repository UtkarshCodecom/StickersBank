package com.stickers.bank.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.stickers.bank.R;
import com.stickers.bank.databinding.ActivityContactUsBinding;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.utils.AdvUtils;

import java.util.ArrayList;
import java.util.Locale;

public class ContactUsActivity extends BaseActivity<ActivityContactUsBinding> implements View.OnClickListener {

    @Override
    public int getLayoutId() {
        return R.layout.activity_contact_us;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        binding.header.btnBack.setVisibility(View.VISIBLE);
        binding.header.tvTitle.setText(getString(R.string.contact_us));
        AdvUtils.getInstance(this).loadShowBMed(binding.flBanner, AdvUtils.getAdSize(ContactUsActivity.this));

        String sourceString = "Let's Talk\n" +
                "\n" +
                "Share your queries and excitements with us stickersbank@gmail.com\n" +
                "\n" +
                "We are also available on instagram, facebook";
        // binding.tvContent.setText(Html.fromHtml(sourceString));


        ArrayList<ClickableSpan> clickableSpans = new ArrayList<>();
        clickableSpans.add(mailClick);
        clickableSpans.add(instaClick);

        makeSectionOfTextBold(binding.tvContent, sourceString, clickableSpans, new String[]{"stickersbank@gmail.com", "instagram"});
        /*createLink(binding.tvContent, sourceString, "stickersbank@gmail.com", new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {

            }
        });*/
    }

    ClickableSpan mailClick = new ClickableSpan() {
        @Override
        public void onClick(@NonNull View view) {
            composeEmail(new String[]{"stickersbank@gmail.com"}, "");
        }
    };

    ClickableSpan instaClick = new ClickableSpan() {
        @Override
        public void onClick(@NonNull View view) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/stickers_bank_?igshid=YmMyMTA2M2Y=")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

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

    public void makeSectionOfTextBold(TextView targetTextView, String text, ArrayList<ClickableSpan> clickableAction, String... textToBold) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int k = 0;
        for (String textItem : textToBold) {
            if (textItem.length() > 0 && !textItem.trim().equals("")) {
                //for counting start/end indexes
                String testText = text.toLowerCase(Locale.US);
                String testTextToBold = textItem.toLowerCase(Locale.US);
                int startingIndex = testText.indexOf(testTextToBold);
                int endingIndex = startingIndex + testTextToBold.length();

                if (startingIndex >= 0 && endingIndex >= 0) {
                    builder.setSpan(new StyleSpan(Typeface.BOLD), startingIndex, endingIndex, 0);
                    builder.setSpan(clickableAction.get(k), startingIndex, endingIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE); //make link
                    k++;
                }
            }
        }
        targetTextView.setText(builder);
        targetTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void composeEmail(String[] addresses, String subject) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, addresses);
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
        /*if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }*/
    }
}