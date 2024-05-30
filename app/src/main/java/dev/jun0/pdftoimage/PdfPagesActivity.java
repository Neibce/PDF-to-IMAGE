package dev.jun0.pdftoimage;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfPagesActivity extends AppCompatActivity {
    private static final int MIN_ITEM_WIDTH = 250;
    private static final int MAX_CONVERT_IMAGE_SIZE = 9000;
    private static final int MIN_CONVERT_IMAGE_SIZE = 100;
    private static final int RECOMMEND_CONVERT_IMAGE_SIZE = 1600;
    private static final int SEEKBAR_INTERVAL = 400;

    private Uri mFileUri;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private ExecutorService mExecutorService;

    private Button mBtnStartConverting;
    private CheckBox mCbSelectAll;
    private LinearLayout mLLInProgress;
    private LinearLayout mLLTopInfo;

    private InterstitialAd mInterstitialAd;

    private final ActivityResultLauncher<String> mRequestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted)
                    showConversionDialog(mFileUri);
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_pages);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,"ca-app-pub-8090012097853976/5260637755", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });

        mFileUri = extras.getParcelable("file_uri");

        mCbSelectAll = findViewById(R.id.cb_select_all);
        mBtnStartConverting = findViewById(R.id.btn_start_converting);

        mLLTopInfo = findViewById(R.id.ll_top_info);
        mLLInProgress = findViewById(R.id.ll_in_progress);

        mBtnStartConverting.setEnabled(false);
        mLLTopInfo.setVisibility(View.INVISIBLE);
        mLLInProgress.setVisibility(View.VISIBLE);

        RecyclerView rvPdfPages = findViewById(R.id.rv_pdf_pages);
        VarColumnGridLayoutManager varColumnGridLayoutManager = new VarColumnGridLayoutManager(this, MIN_ITEM_WIDTH);
        rvPdfPages.setLayoutManager(varColumnGridLayoutManager);

        rvPdfPages.addItemDecoration(new GridSpacingItemDecoration(MIN_ITEM_WIDTH, 10, true));

        mRecyclerViewAdapter = new RecyclerViewAdapter(items->{
            mBtnStartConverting.setEnabled(items != 0);
            mCbSelectAll.setText(String.format(Locale.getDefault(), "%d", items));
            mCbSelectAll.setChecked(items >= mRecyclerViewAdapter.getItemCount());
        });
        rvPdfPages.setAdapter(mRecyclerViewAdapter);

        RecyclerView.ItemAnimator animator = rvPdfPages.getItemAnimator();
        if (animator instanceof SimpleItemAnimator)
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);

        mCbSelectAll.setOnClickListener((view) -> mRecyclerViewAdapter.selectAll(((CheckBox) view).isChecked()));

        mBtnStartConverting.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(PdfPagesActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                showConversionDialog(mFileUri);
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(PdfPagesActivity.this)
                        .setMessage(R.string.please_allow_fw_permission)
                        .setPositiveButton(R.string.open_setting, (dialog, id) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))
                                    .addCategory(Intent.CATEGORY_DEFAULT)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();
            } else {
                mRequestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

        });

        mExecutorService = Executors.newSingleThreadExecutor();
        Handler mMainThreadHandler;
        mMainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        PdfLoader pdfLoader = new PdfLoader(mExecutorService, mMainThreadHandler);
        loadPdf(pdfLoader, "");

    }

    private void loadPdf(PdfLoader pdfLoader, String password){
        pdfLoader.load(this, mFileUri, password, new PdfLoader.LoadCallback() {
            @Override
            public void onLoaded(PdfRenderer pdfRenderer) {
                int pageCount = pdfRenderer.getPageCount();
                mRecyclerViewAdapter.initializeItems(pageCount);
                mCbSelectAll.setChecked(true);
                mCbSelectAll.setText(String.format(Locale.getDefault(), "%d", pageCount));
                mBtnStartConverting.setEnabled(true);
                mLLInProgress.setVisibility(View.GONE);
                mLLTopInfo.setVisibility(View.VISIBLE);
                pdfLoader.generateThumbnails(pdfRenderer, MIN_ITEM_WIDTH, new PdfLoader.GenerateThumbnailCallback() {
                    @Override
                    public void onGenerated(int page, Bitmap thumbnail) {
                        mRecyclerViewAdapter.setItemBitmap(page, thumbnail);
                    }

                    @Override
                    public void onComplete() {
                        pdfRenderer.close();
                    }
                });
            }

            @Override
            public void onError(int errorCode) {
                if(errorCode == PdfLoader.ERROR_INCORRECT_PASSWORD) {
                    EditText editText = new EditText(PdfPagesActivity.this);
                    editText.setSingleLine();
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                    FrameLayout container = new FrameLayout(PdfPagesActivity.this);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = 48;
                    params.rightMargin = 48;
                    editText.setLayoutParams(params);
                    editText.requestFocus();
                    container.addView(editText);

                    if(!password.equals(""))
                        editText.setError(getString(R.string.incorrect_password));

                    new AlertDialog.Builder(PdfPagesActivity.this)
                            .setTitle(R.string.docuemt_password)
                            .setMessage(R.string.document_protected)
                            .setView(container)
                            .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                String etText = editText.getText().toString();
                                if(etText.equals(""))
                                    editText.setError(getString(R.string.required));
                                loadPdf(pdfLoader, etText);
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> finish())
                            .setOnCancelListener((dialog) -> finish())
                            .show();
                }
            }
        });

    }

    private void showConversionDialog(Uri fileUri) {
        LayoutInflater layoutinflater = LayoutInflater.from(PdfPagesActivity.this);
        View dialogView = layoutinflater.inflate(R.layout.convert_setting_dialog, null);

        RadioButton rbPng = dialogView.findViewById(R.id.rb_png);

        LinearLayout llHeight = dialogView.findViewById(R.id.ll_height);
        TextView tvSize1 = dialogView.findViewById(R.id.tv_size_1);

        SeekBar sb1 = dialogView.findViewById(R.id.sb1);
        SeekBar sb2 = dialogView.findViewById(R.id.sb2);

        EditText etSize1 = dialogView.findViewById(R.id.et_size_1);
        EditText etSize2 = dialogView.findViewById(R.id.et_size_2);

        CheckBox cbKeepRatio = dialogView.findViewById(R.id.cb_keep_ratio);

        sb1.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener(etSize1));
        sb2.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener(etSize2));

        etSize1.addTextChangedListener(new MyTextWatcher(sb1));
        etSize1.setOnFocusChangeListener(new MyOnFocusChangeListener());
        etSize2.addTextChangedListener(new MyTextWatcher(sb2));
        etSize2.setOnFocusChangeListener(new MyOnFocusChangeListener());

        cbKeepRatio.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                tvSize1.setText(R.string.long_side);
                llHeight.setVisibility(View.GONE);
                sb2.setVisibility(View.GONE);
            } else {
                tvSize1.setText(R.string.width);
                llHeight.setVisibility(View.VISIBLE);
                sb2.setVisibility(View.VISIBLE);
            }
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PdfPagesActivity.this);
        alertDialogBuilder.setView(dialogView);
        alertDialogBuilder.setTitle(R.string.conversion_setting);

        alertDialogBuilder.setPositiveButton(R.string.start, null);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean isKeepRatioChecked = cbKeepRatio.isChecked();

            boolean sizeInRange = isSizeInRange(etSize1);
            if (!isKeepRatioChecked && sizeInRange)
                sizeInRange = isSizeInRange(etSize2);

            if (sizeInRange) {
                alertDialog.dismiss();

                ArrayList<Boolean> isSelectedList = mRecyclerViewAdapter.getIsSelectedList();
                try {
                    FileOutputStream fileOutputStream = openFileOutput("convert_page_info", Context.MODE_PRIVATE);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(isSelectedList);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(PdfPagesActivity.this, ConvertActivity.class);
                intent.putExtra("selectedCount", mRecyclerViewAdapter.getSelectedCount());
                intent.putExtra("size1", Integer.parseInt(etSize1.getText().toString()));
                intent.putExtra("size2", Integer.parseInt(etSize2.getText().toString()));
                intent.putExtra("isPngChecked", rbPng.isChecked());
                intent.putExtra("isKeepRatioChecked", isKeepRatioChecked);
                intent.putExtra("fileUriString", fileUri.toString());

                if (mInterstitialAd != null) {
                    mInterstitialAd.show(PdfPagesActivity.this);
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) { }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            mBtnStartConverting.setEnabled(false);
                            mInterstitialAd = null;
                        }
                    });
                } else {
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutorService.shutdownNow();
    }

    private boolean isSizeInRange(@NonNull EditText editText) {
        Editable editable = editText.getText();
        try {
            int size = Integer.parseInt(editable.toString());
            if (size > MAX_CONVERT_IMAGE_SIZE) {
                editable.replace(0, editable.length(), Integer.toString(MAX_CONVERT_IMAGE_SIZE));
                Toast.makeText(PdfPagesActivity.this, String.format(getString(R.string.max_size_info), MAX_CONVERT_IMAGE_SIZE), Toast.LENGTH_LONG).show();
                return false;
            } else if (size < MIN_CONVERT_IMAGE_SIZE) {
                editable.replace(0, editable.length(), Integer.toString(MIN_CONVERT_IMAGE_SIZE));
                Toast.makeText(PdfPagesActivity.this, String.format(getString(R.string.min_size_info), MIN_CONVERT_IMAGE_SIZE), Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            editable.replace(0, editable.length(), Integer.toString(RECOMMEND_CONVERT_IMAGE_SIZE));
            Toast.makeText(PdfPagesActivity.this, R.string.wrong_size_input, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private static class MyOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        final EditText mEditText;

        MyOnSeekBarChangeListener(EditText editText) {
            mEditText = editText;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser)
                mEditText.setText(Integer.toString((progress + 1) * SEEKBAR_INTERVAL));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) { }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) { }
    }

    private static class MyTextWatcher implements TextWatcher {
        final SeekBar mSeekBar;

        MyTextWatcher(SeekBar seekBar) {
            mSeekBar = seekBar;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            String ss = s.toString();

            if (!ss.equals("")) {
                int size = Integer.parseInt(ss);
                if (size > MAX_CONVERT_IMAGE_SIZE)
                    s.replace(0, s.length(), Integer.toString(MAX_CONVERT_IMAGE_SIZE));

                mSeekBar.setProgress(Integer.parseInt(s.toString()) / SEEKBAR_INTERVAL - 1);
            }
        }

    }

    public static class MyOnFocusChangeListener implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View view, boolean b) {
            if (!b) {
                Editable editable = ((EditText) view).getText();
                try {
                    int size = Integer.parseInt(editable.toString());
                    if (size > MAX_CONVERT_IMAGE_SIZE)
                        editable.replace(0, editable.length(), Integer.toString(MAX_CONVERT_IMAGE_SIZE));
                    else if (size < MIN_CONVERT_IMAGE_SIZE)
                        editable.replace(0, editable.length(), Integer.toString(MIN_CONVERT_IMAGE_SIZE));
                } catch (NumberFormatException e) {
                    editable.replace(0, editable.length(), Integer.toString(RECOMMEND_CONVERT_IMAGE_SIZE));
                }
            }
        }
    }
}