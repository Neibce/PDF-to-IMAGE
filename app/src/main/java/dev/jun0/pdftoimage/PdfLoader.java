package dev.jun0.pdftoimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;


public class PdfLoader {
    public static final int ERROR_FILE_NOT_FOUND = 0;
    public static final int ERROR_INCORRECT_PASSWORD = 1;
    public static final int ERROR_IO = 2;

    private final ExecutorService mExecutor;
    private final Handler mResultHandler;

    public PdfLoader(ExecutorService executor, Handler resultHandler) {
        mExecutor = executor;
        mResultHandler = resultHandler;
    }

    public void load(Context context, Uri fileUri, String password, LoadCallback loadCallback) {
        mExecutor.execute(() -> {
            try {
                PDFBoxResourceLoader.init(context);
                PDDocument pdDocument = PDDocument.load(context.getContentResolver().openInputStream(fileUri), password);

                File file = new File(context.getFilesDir(), "target_pdf.pdf");

                pdDocument.setAllSecurityToBeRemoved(true);
                pdDocument.save(file);
                pdDocument.close();

                ParcelFileDescriptor parcelFileDescriptor =
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

                PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                notifyLoaded(pdfRenderer, loadCallback);
            } catch (FileNotFoundException e) {
                notifyError(ERROR_FILE_NOT_FOUND, loadCallback);
            }catch(InvalidPasswordException e){
                notifyError(ERROR_INCORRECT_PASSWORD, loadCallback);
            }catch (IOException e) {
                notifyError(ERROR_IO, loadCallback);
            }
        });
    }

    public static PdfRenderer load(Context context, Uri fileUri) throws IOException {
        File file = new File(context.getFilesDir(), "target_pdf.pdf");
        ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        file.delete();
        return new PdfRenderer(parcelFileDescriptor);
    }

    public static Bitmap generateBitmap(PdfRenderer pdfRenderer, int index, int longSideLength) {
        return generateBitmap(pdfRenderer, index, longSideLength, -1);
    }

    public static Bitmap generateBitmap(PdfRenderer pdfRenderer, int index, int size1, int size2) {
        PdfRenderer.Page page = pdfRenderer.openPage(index);
        double ratio = (double) page.getWidth() / page.getHeight();

        int bitmapWidth = size1;
        int bitmapHeight = size2;
        if (size2 == -1) {
            if (ratio >= 1.0)
                bitmapHeight = (int) (size1 / ratio);
            else {
                bitmapWidth = (int) (size1 * ratio);
                bitmapHeight = size1;
            }

        }

        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();

        return bitmap;
    }

    public void generateThumbnails(@NonNull PdfRenderer pdfRenderer, int minItemWidth, GenerateThumbnailCallback generateThumbnailCallback) {
        mExecutor.execute(() -> {
            for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                if (Thread.interrupted())
                    return;

                Bitmap bitmap = generateBitmap(pdfRenderer, i, minItemWidth);
                Log.i("EEE", Thread.currentThread().getName() + " GENERATE: " + i);

                notifyGenerated(i, bitmap, generateThumbnailCallback);
            }

            notifyComplete(generateThumbnailCallback);
        });
    }

    private void notifyGenerated(int page, Bitmap thumbnail, GenerateThumbnailCallback callback) {
        if(!Thread.interrupted())
            mResultHandler.post(() -> callback.onGenerated(page, thumbnail));
    }

    private void notifyComplete(GenerateThumbnailCallback callback) {
        if(!Thread.interrupted())
            mResultHandler.post(callback::onComplete);
    }

    private void notifyLoaded(PdfRenderer pdfRenderer, LoadCallback callback) {
        if(!Thread.interrupted())
            mResultHandler.post(() -> callback.onLoaded(pdfRenderer));
    }

    private void notifyError(int errorCode, LoadCallback callback) {
        if(!Thread.interrupted())
            mResultHandler.post(() -> callback.onError(errorCode));
    }

    interface LoadCallback {
        void onLoaded(PdfRenderer pdfRenderer);
        void onError(int errorCode);
    }

    interface GenerateThumbnailCallback {
        void onGenerated(int page, Bitmap thumbnail);
        void onComplete();
    }
}
