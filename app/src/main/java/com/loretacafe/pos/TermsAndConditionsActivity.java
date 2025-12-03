package com.loretacafe.pos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TermsAndConditionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_conditions);

        TextView termsText = findViewById(R.id.tvTermsContent);
        Button btnAgree = findViewById(R.id.btnAgree);

        if (termsText != null) {
            try {
                String termsContent = getString(R.string.terms_and_conditions_full);
                termsText.setText(termsContent);
            } catch (Exception e) {
                termsText.setText("Terms and Conditions content not available.");
            }
        }

        if (btnAgree != null) {
            btnAgree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        }
    }
}

