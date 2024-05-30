package dev.jun0.pdftoimage;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class ConvertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        adView.loadAd(adRequest);

        int selectedCount = extras.getInt("selectedCount");

        Button btnCancel = findViewById(R.id.btn_cancel);
        TextView tvProgress = findViewById(R.id.tv_progress);
        ProgressBar pbProgress = findViewById(R.id.pb_progress);

        btnCancel.setEnabled(false);
        tvProgress.setText(String.format(getString(R.string.progress), 0, selectedCount));
        pbProgress.setMax(selectedCount);
        pbProgress.setProgress(0);

        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putInt("selectedCount", selectedCount);
        dataBuilder.putInt("size1", extras.getInt("size1"));
        dataBuilder.putInt("size2", extras.getInt("size2"));
        dataBuilder.putBoolean("isPngChecked", extras.getBoolean("isPngChecked"));
        dataBuilder.putBoolean("isKeepRatioChecked", extras.getBoolean("isKeepRatioChecked"));
        dataBuilder.putString("fileUriString", extras.getString("fileUriString"));


        WorkManager workManager = WorkManager.getInstance(this);
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(PdfConversionWorker.class).setInputData(dataBuilder.build()).build();

        btnCancel.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ConvertActivity.this);
            builder.setMessage(R.string.are_you_sure_cancel)
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                        workManager.cancelWorkById(oneTimeWorkRequest.getId());
                        finish();
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        });
        workManager.beginWith(oneTimeWorkRequest).enqueue();

        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        Data progress = workInfo.getProgress();
                        int value = progress.getInt("PROGRESS", -1);
                        if(value != -1) {
                            tvProgress.setText(String.format(getString(R.string.progress), value, selectedCount));
                            pbProgress.setProgress(value);
                        }else if(workInfo.getState() == WorkInfo.State.RUNNING)
                            btnCancel.setEnabled(true);
                        else if(workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            btnCancel.setEnabled(false);
                            tvProgress.setText(String.format(getString(R.string.progress), selectedCount, selectedCount));
                            pbProgress.setProgress(selectedCount);
                            new AlertDialog.Builder(ConvertActivity.this)
                                    .setMessage(R.string.converted_successfully)
                                    .setPositiveButton(android.R.string.ok, (dialog, id) -> finish())
                                    .setCancelable(false)
                                    .create().show();
                        }
                    }
                });
    }
}