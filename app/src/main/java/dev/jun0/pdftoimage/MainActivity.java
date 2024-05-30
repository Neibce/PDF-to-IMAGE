package dev.jun0.pdftoimage;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<String> startActivityResult = registerForActivityResult(new ActivityResultContracts.GetContent(),
                result -> {
                    if (result == null)
                        return;
                    Intent intent = new Intent(MainActivity.this, PdfPagesActivity.class);
                    intent.putExtra("file_uri", result);
                    startActivity(intent);
                });

        Button btnSelectPdf = findViewById(R.id.btn_select_pdf);
        btnSelectPdf.setOnClickListener(view -> startActivityResult.launch("application/pdf"));

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }
}