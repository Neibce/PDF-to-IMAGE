package dev.jun0.pdftoimage;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class PdfConversionWorker extends Worker {
    private enum Status {SUCCESS, FAILED, CANCELED}
    private final static String CHANNEL_ID_CONVERTED= "converted";
    private final static String CHANNEL_ID_CONVERTING = "converting";

    private final NotificationManager mNotificationManager;
    private final Context mContext;

    private UriFileInfo mUriFileInfo;

    public PdfConversionWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mContext = context;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public Result doWork() {
        setProgressAsync(new Data.Builder().putInt("PROGRESS", 0).build());

        try {
            FileInputStream fileInputStream = mContext.openFileInput("convert_page_info");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            ArrayList<Boolean> isPageCheckedList = (ArrayList<Boolean>) objectInputStream.readObject();

            Data inputData = getInputData();
            int size1 = inputData.getInt("size1", 1600);
            int size2 = inputData.getInt("size2", 1600);
            boolean isPngChecked = inputData.getBoolean("isPngChecked", true);
            boolean isKeepRatioChecked = inputData.getBoolean("isKeepRatioChecked", true);

            Uri fileUri = Uri.parse(inputData.getString("fileUriString"));
            mUriFileInfo = new UriFileInfo(mContext, fileUri);

            setForegroundAsync(createForegroundInfo(0, 100));
            Status convertStatus = convert(fileUri, isPngChecked ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, isKeepRatioChecked, size1, size2, isPageCheckedList);
            notifyWorkResult(convertStatus);

            if(convertStatus == Status.SUCCESS)
                return Result.success();
            else
                return Result.failure();
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            notifyWorkResult(Status.FAILED);
            return Result.failure();
        }
    }

    private void saveImage(Bitmap bitmap, @NonNull String imageName, Bitmap.CompressFormat compressFormat) throws IOException {
        String strCompressFormat = compressFormat == Bitmap.CompressFormat.JPEG ? "jpg" : "png";

        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = mContext.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName + "." + strCompressFormat);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + strCompressFormat);
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + mContext.getText(R.string.app_name) + File.separator + mUriFileInfo.getBaseName());
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + mContext.getText(R.string.app_name) + File.separator + mUriFileInfo.getBaseName();
            File directory = new File(imagesDir);
            File image = new File(imagesDir, imageName + "." + strCompressFormat);
            directory.mkdirs();
            fos = new FileOutputStream(image);
        }
        bitmap.compress(compressFormat, 100, fos);
        fos.close();
    }

    private Status convert(Uri fileUri, Bitmap.CompressFormat compressFormat, boolean isKeepRatioChecked,
                         int size1, int size2, ArrayList<Boolean> checkedPageArray) throws IOException {
        PdfRenderer pdfRenderer = PdfLoader.load(mContext, fileUri);

        int pageCount = pdfRenderer.getPageCount();
        int progress = 0;
        for (int i = 0; i < pageCount; i++) {
            if(isStopped())
                return Status.CANCELED;

            if(checkedPageArray.get(i)) {
                Bitmap bitmap;

                if (isKeepRatioChecked)
                    bitmap = PdfLoader.generateBitmap(pdfRenderer, i, size1);
                else
                    bitmap = PdfLoader.generateBitmap(pdfRenderer, i, size1, size2);

                saveImage(bitmap, String.valueOf(i + 1), compressFormat);

                progress++;
                setProgressAsync(new Data.Builder().putInt("PROGRESS", progress).build());
                setForegroundAsync(createForegroundInfo(progress, pageCount));
            }
        }

        return Status.SUCCESS;
    }

    private void notifyWorkResult(Status status) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(CHANNEL_ID_CONVERTED, mContext.getString(R.string.converted), mContext.getString(R.string.noti_when_converted), NotificationManager.IMPORTANCE_DEFAULT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID_CONVERTED)
                //.setContentText(mUriFileInfo.getFileName())
                .setSmallIcon(R.drawable.ic_twotone_insert_drive_file_96)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false);

        switch (status){
            case SUCCESS:
                builder.setContentTitle(mContext.getString(R.string.converted_successfully));
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.location) + " : " + File.separator + Environment.DIRECTORY_DCIM + File.separator + mContext.getText(R.string.app_name) + File.separator + mUriFileInfo.getBaseName()));
                break;
            case FAILED:
                builder.setContentTitle(mContext.getString(R.string.conversion_failed));
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.unknown_error_occurred) + " : " + mUriFileInfo.getFileName()));
                break;
            case CANCELED:
                builder.setContentTitle(mContext.getString(R.string.conversion_canceled));
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(mUriFileInfo.getFileName()));
                break;

        }

        mNotificationManager.notify((int)SystemClock.uptimeMillis(), builder.build());
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(int currentProgress, int maxProgress) {
        PendingIntent intent = WorkManager.getInstance(mContext)
                .createCancelPendingIntent(getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(CHANNEL_ID_CONVERTING, mContext.getString(R.string.converting), mContext.getString(R.string.noti_converting_status), NotificationManager.IMPORTANCE_LOW);

        Notification notification = new NotificationCompat.Builder(mContext, CHANNEL_ID_CONVERTING)
                .setContentTitle(mContext.getString(R.string.converting_) + " " + String.format(mContext.getString(R.string.progress), currentProgress, maxProgress))
                //.setContentText(mUriFileInfo.getFileName())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mUriFileInfo.getFileName()))
                .setTicker("progress")
                .setProgress(maxProgress, currentProgress, false)
                .setSmallIcon(R.drawable.ic_twotone_insert_drive_file_96)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_delete, mContext.getString(android.R.string.cancel), intent)
                .build();

        return new ForegroundInfo(1023, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel(String id, String name, String description, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(description);
            mNotificationManager.createNotificationChannel(channel);
        }
    }
}
