package jp.co.ohq.blesampleomron.model.system;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public final class AppConfig {

    public static final String GUEST = "Guest";

    @Nullable
    private static AppConfig sInstance;

    @NonNull
    public final String mPackageName;

    @NonNull
    private String mCurrentUserName;

    private AppConfig(@NonNull Context context) {
        mPackageName = context.getPackageName();
        mCurrentUserName = "";
    }

    public static AppConfig init(@NonNull Context context) {
        if (null != sInstance) {
            throw new IllegalStateException("An instance has already been created.");
        }
        return sInstance = new AppConfig(context);
    }

    public static AppConfig sharedInstance() {
        if (null == sInstance) {
            throw new IllegalStateException("Instance has not been created.");
        }
        return sInstance;
    }

    @NonNull
    public String getNameOfCurrentUser() {
        return mCurrentUserName;
    }

    public void setNameOfCurrentUser(@NonNull String userName) {
        mCurrentUserName = userName;
    }

    @NonNull
    public String getApplicationVersionName(@NonNull Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);

            if (packageInfo != null) {
                return packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    @NonNull
    public String getExternalApplicationDirectoryPath(@NonNull Context context) {
        File dir = new File(getDocumentsDirectoryPath(context));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IllegalAccessError("Failed to make directory '" + dir + "'");
            }
        }
        return dir.getAbsolutePath();
    }

    private String getDocumentsDirectoryPath(@NonNull Context context) {
        return context.getFilesDir().getPath();
    }
}
