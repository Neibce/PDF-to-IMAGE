package dev.jun0.pdftoimage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class UriFileInfo {
    private final String baseName;
    private final String fileExtension;

    @SuppressLint("Range")
    UriFileInfo(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        int cut = result.lastIndexOf(".");
        this.baseName =  result.substring(0, cut);
        this.fileExtension = result.substring(cut + 1);
    }

    public String getBaseName(){
        return baseName;
    }

    public String getFileName(){
        return baseName + "." + fileExtension;
    }

    public String getFileExtension(){
        return fileExtension;
    }
}
