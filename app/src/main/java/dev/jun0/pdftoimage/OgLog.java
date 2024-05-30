package dev.jun0.pdftoimage;

import android.app.Activity;
import android.util.Log;

public class OgLog {
    public static final boolean D = true;

    public static final String TAG = "organ";

    public static void d(Activity paramActivity, String paramString) {}

    public static void d(String paramString) {
        Log.d(TAG, paramString);
    }
}

