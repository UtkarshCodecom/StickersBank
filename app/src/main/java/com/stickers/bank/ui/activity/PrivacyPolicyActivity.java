package com.stickers.bank.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import com.stickers.bank.R;
import com.stickers.bank.databinding.ActivityFaqsBinding;
import com.stickers.bank.databinding.ActivityPrivacyPolicyBinding;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.utils.AdvUtils;

public class PrivacyPolicyActivity extends BaseActivity<ActivityPrivacyPolicyBinding> implements View.OnClickListener {

    @Override
    public int getLayoutId() {
        return R.layout.activity_privacy_policy;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        binding.header.btnBack.setVisibility(View.VISIBLE);
        binding.header.tvTitle.setText(getString(R.string.about_us));
        AdvUtils.getInstance(this).loadShowBMed(binding.flBanner, AdvUtils.getAdSize(PrivacyPolicyActivity.this));

        String sourceString = "<p><br></p>\n" +
                "<p><strong>Privacy Policy</strong></p>\n" +
                "<p><br></p>\n" +
                "<p>STICKERS BANK is a registered Trademark,</p>\n" +
                "<p>STICKERS BANK is a sticker app for whatsapp which is developed and controlled by the BOLDBOIZ, StickersBank app is an Ad Supported app which can be used for the free of cost</p>\n" +
                "<p>This page is used to inform visitors regarding our policies with the collection, use, and disclosure of Personal Information if anyone decided to use our Service.</p>\n" +
                "<p>If you choose to use our Service, then you agree to the collection and use of information in relation to this policy.&nbsp;</p>\n" +
                "<p>StickersBank will not collect or use or share your information with anyone.</p>\n" +
                "<p><br></p>\n" +
                "<p><strong>Google Play Services</strong></p>\n" +
                "<p>AdMob</p>\n" +
                "<p>#Google Analytics for Firebase</p>\n" +
                "<p><br></p>\n" +
                "<p>StickersBank may employ third-party companies and individuals due to the following reasons:</p>\n" +
                "<p>To facilitate our Service, To provide the Service on our behalf, To perform Service-related services, To assist us in analyzing how our Service is used.</p>\n" +
                "<p><br></p>\n" +
                "<p><strong>Security</strong></p>\n" +
                "<p>StickersBank never collects your Personal information or Data</p>\n" +
                "<p><br></p>\n" +
                "<p><strong>Links to Other Sites</strong></p>\n" +
                "<p><br></p>\n" +
                "<p>StickersBank Service may contain links to other sites. If you click on a third-party link, you will be directed to that site. Note that these external sites are not operated by us. Therefore, we strongly advise you to review the Privacy Policy of these websites. We have no control over and assume no responsibility for the content, privacy policies, or practices of any third-party sites or services.</p>\n" +
                "<p><br></p>\n" +
                "<p><strong>Children&rsquo;s Privacy</strong></p>\n" +
                "<p><br></p>\n" +
                "<p>StickersBank Services do not address anyone under the age of 18. We do not knowingly collect personally identifiable information from children under 18. If you are a parent or guardian and you are aware that your child has provided us with personal information, please contact us so that we will be able to do necessary actions.</p>\n" +
                "<p><br></p>\n" +
                "<p>Changes to This Privacy Policy</p>\n" +
                "<p><br></p>\n" +
                "<p>StickersBank may update our Privacy Policy from time to time. Thus, you are advised to review this page periodically for any changes.&nbsp;</p>\n" +
                "<p>We will notify you of any changes by posting the new Privacy Policy on this page.</p>\n" +
                "<p>#This policy is effective as of 2021-09-10</p>\n" +
                "<p><br></p>";
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