package app.revanced.bilibili.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressWarnings("unused")
public class Utils {
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static Handler mainHandler = new Handler(Looper.getMainLooper());

    private static String mobiApp = "";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Boolean isPink = null;
    private static Boolean isBlue = null;
    private static Boolean isPlay = null;
    private static Boolean isHd = null;

    // value will changed by patcher
    public static boolean newPlayerEnabled = false;

    public static Context getContext() {
        if (context == null) {
            LogHelper.error(() -> "Context is null");
        }
        return context;
    }

    @SuppressLint("DiscouragedApi")
    public static int getResId(String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    public static String getString(String idName) {
        return context.getString(getResId(idName, "string"));
    }

    public static String getString(String idName, Object... formatArgs) {
        return context.getString(getResId(idName, "string"), formatArgs);
    }

    public static String[] getStringArray(String idName) {
        return context.getResources().getStringArray(getResId(idName, "array"));
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public static Drawable getDrawable(String idName) {
        return context.getDrawable(getResId(idName, "drawable"));
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public static Drawable getDrawable(Context context, String idName) {
        return context.getDrawable(getResId(idName, "drawable"));
    }

    public static int getColor(String idName) {
        return context.getColor(getResId(idName, "color"));
    }

    public static int getColor(Context context, String idName) {
        return context.getColor(getResId(idName, "color"));
    }

    public static ColorStateList getColorStateList(String idName) {
        return context.getColorStateList(getResId(idName, "color"));
    }

    public static ColorStateList getColorStateList(Context context, String idName) {
        return context.getColorStateList(getResId(idName, "color"));
    }

    public static void reboot() {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
        if (intent != null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityManager am = context.getSystemService(ActivityManager.class);
        if (am != null)
            for (ActivityManager.AppTask task : am.getAppTasks())
                task.finishAndRemoveTask();
        context.startActivity(intent);
        System.exit(0);
    }

    public static String getMobiApp() {
        if (TextUtils.isEmpty(mobiApp)) {
            String mobiApp = null;
            try {
                mobiApp = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                        .metaData.getString("MOBI_APP");
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (TextUtils.isEmpty(mobiApp)) {
                switch (context.getPackageName()) {
                    case Constants.BLUE_PACKAGE_NAME:
                        mobiApp = "android_b";
                        break;
                    case Constants.PLAY_PACKAGE_NAME:
                        mobiApp = "android_i";
                        break;
                    case Constants.HD_PACKAGE_NAME:
                        mobiApp = "android_hd";
                        break;
                    case Constants.PINK_PACKAGE_NAME:
                    default:
                        mobiApp = "android";
                        break;
                }
            }
            Utils.mobiApp = mobiApp;
        }
        return mobiApp;
    }

    public static boolean isPink() {
        Boolean pink = isPink;
        if (pink == null) {
            pink = "android".equals(getMobiApp());
            isPink = pink;
        }
        return pink;
    }

    public static boolean isBlue() {
        Boolean blue = isBlue;
        if (blue == null) {
            blue = "android_b".equals(getMobiApp());
            isBlue = blue;
        }
        return blue;
    }

    public static boolean isPlay() {
        Boolean play = isPlay;
        if (play == null) {
            play = "android_i".equals(getMobiApp());
            isPlay = play;
        }
        return play;
    }

    public static boolean isHd() {
        Boolean hd = isHd;
        if (hd == null) {
            hd = "android_hd".equals(getMobiApp());
            isHd = hd;
        }
        return hd;
    }

    public static boolean isCurrentlyOnMainThread() {
        return Looper.getMainLooper().isCurrentThread();
    }

    public static void runOnMainThread(Runnable runnable) {
        runOnMainThread(0L, runnable);
    }

    public static void runOnMainThread(long delayMills, Runnable runnable) {
        if (delayMills == 0L) {
            if (isCurrentlyOnMainThread()) {
                runnable.run();
            } else {
                mainHandler.post(runnable);
            }
        } else {
            mainHandler.postDelayed(runnable, delayMills);
        }
    }

    public static void async(Runnable runnable) {
        executor.execute(runnable);
    }

    public static <T> Future<T> submitTask(Callable<T> task) {
        return executor.submit(task);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        var bytesCopied = 0L;
        var buffer = new byte[8192];
        var bytes = in.read(buffer);
        while (bytes >= 0) {
            out.write(buffer, 0, bytes);
            bytesCopied += bytes;
            bytes = in.read(buffer);
        }
        return bytesCopied;
    }

    @SuppressWarnings("deprecation")
    public static boolean currentIsLandscape() {
        WindowManager windowManager = context.getSystemService(WindowManager.class);
        int orientation = windowManager.getDefaultDisplay().getOrientation();
        return orientation == Surface.ROTATION_90 || orientation == Surface.ROTATION_270;
    }

    // codes will filled by patcher
    public static String signQuery(Map<String, String> params) {
        return "";
    }

    // codes will filled by patcher
    public static String getAppKey() {
        return "";
    }

    // codes will filled by patcher
    public static String getAccessKey() {
        return "";
    }

    // codes will filled by patcher
    @NonNull
    public static SharedPreferences blkvPrefsByName(String name, boolean multiProcess) {
        throw new UnsupportedOperationException();
    }

    // codes will filled by patcher
    @NonNull
    public static SharedPreferences blkvPrefsByFile(File file, boolean multiProcess) {
        throw new UnsupportedOperationException();
    }

    // codes will filled by patcher
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isEffectiveVip() {
        return false;
    }

    // codes will filled by patcher
    public static boolean isLogin() {
        return false;
    }

    // codes will filled by patcher
    public static boolean isNightFollowSystem() {
        return false;
    }

    public static String getCurrentProcessName() {
        String name = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            name = Application.getProcessName();
        } else try {
            name = ActivityThread.currentProcessName();
        } catch (Throwable ignored) {
        }
        return name;
    }

    public static boolean isInMainProcess() {
        String name = getCurrentProcessName();
        return !name.isEmpty() && !name.contains(":");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void saveImage(String url) {
        try {
            try (var input = new URL(url).openStream()) {
                var relativePath = Environment.DIRECTORY_PICTURES + File.separator + "bilibili";
                var fullFilename = url.substring(url.lastIndexOf('/') + 1);
                var filename = fullFilename.substring(0, fullFilename.lastIndexOf('.'));

                var now = System.currentTimeMillis();
                var contentValues = new ContentValues();
                var mimeType = HttpURLConnection.guessContentTypeFromName(fullFilename);
                if (TextUtils.isEmpty(mimeType)) mimeType = "image/png";
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                contentValues.put(MediaStore.MediaColumns.DATE_ADDED, now / 1000);
                contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, now / 1000);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, now);
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
                } else {
                    var path = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                    ), "bilibili");
                    path.mkdirs();
                    contentValues.put(MediaStore.MediaColumns.DATA, new File(path, fullFilename).getAbsolutePath());
                }
                try {
                    var resolver = Utils.getContext().getContentResolver();
                    var uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    if (uri == null) return;
                    try (var output = resolver.openOutputStream(uri)) {
                        Utils.copyStream(input, output);
                    }
                    Toasts.showShortWithId("biliroaming_toast_image_save_success", relativePath + File.separator + fullFilename);
                } catch (Throwable th2) {
                    Toasts.showShortWithId("biliroaming_toast_image_save_failed");
                }
            }
        } catch (Throwable th) {
            Toasts.showShortWithId("biliroaming_toast_image_get_failed");
        }
    }
}
