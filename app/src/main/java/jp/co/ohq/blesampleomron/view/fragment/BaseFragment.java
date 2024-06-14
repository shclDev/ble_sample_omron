package jp.co.ohq.blesampleomron.view.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.ohq.ble.OHQDeviceManager;
import jp.co.ohq.blesampleomron.R;
import jp.co.ohq.blesampleomron.controller.ScanController;
import jp.co.ohq.blesampleomron.controller.util.AppLog;

abstract class BaseFragment extends Fragment {
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PREFERENCE_KEY_NEVER_ASK_AGAIN_LOCALE_NAME = "KEY_NEVER_ASK_AGAIN_LOCALE";
    private static final String PREFERENCE_KEY_NEVER_ASK_AGAIN_STORAGE_NAME = "KEY_NEVER_ASK_AGAIN_STORAGE";
    private static final String PREFERENCE_XML_NAME = "permission";
    public AlertDialog mPermissionDialog;
    public TerminalSettingsStage mTerminalSettingsStage;
    public boolean mBluetoothPowerState;

    /*
    "TerminalSettingsStage" is to know which check stage you are in currently.
    Even if you skipped some checks, the scan will start when you reach "CHECK_COMPLETE" stage.
    */
    public enum TerminalSettingsStage {
        BLUETOOTH_CHECK,
        LOCATION_PERMISSION_CHECK,
        LOCATION_PERMISSION_REQUEST,
        LOCATION_CHECK,
        CHECK_COMPLETE;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(onGetTitle().toUpperCase());
    }

    @NonNull
    protected String onGetTitle() {
        return getString(R.string.app_name);
    }

    protected void replaceFragment(@IdRes int containerViewId, @NonNull Fragment fragment) {
        getFragmentManager()
                .beginTransaction()
                .replace(containerViewId, fragment)
                .commit();
    }

    protected void replaceFragmentWithAddingToBackStack(@IdRes int containerViewId, @NonNull Fragment fragment) {
        getFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(containerViewId, fragment)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        AppLog.vMethodIn();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        boolean isGranted = true;
        if (requestCode == PERMISSIONS_REQUEST
                && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                }
            }
        }
        if (isGranted) {
            // Nop
            AppLog.d("request permission granted!");
        } else {
            String permission = Manifest.permission.ACCESS_FINE_LOCATION;
            if (!shouldShowRequestPermissionRationale(permission)) {
                // Never ask again
                setPermissionNeverAskAgain(getContext(), permission, true);
            }
            warningLocationPermissionShowDialog(R.string.permission_denied_warning_location);
        }
        AppLog.vMethodOut();
    }

    protected boolean getPermissionNeverAskAgain(final Context context, final String permission) {
        if (context == null) {
            return false;
        }
        final SharedPreferences preference = context.getSharedPreferences(PREFERENCE_XML_NAME, Context.MODE_PRIVATE);
        final String key = permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ? PREFERENCE_KEY_NEVER_ASK_AGAIN_LOCALE_NAME : PREFERENCE_KEY_NEVER_ASK_AGAIN_STORAGE_NAME;
        return preference.getBoolean(key, false);
    }

    protected void setPermissionNeverAskAgain(final Context context, final String permission, final boolean b) {
        if (context == null) {
            return;
        }
        final SharedPreferences preference = context.getSharedPreferences(PREFERENCE_XML_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preference.edit();
        final String key = permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ? PREFERENCE_KEY_NEVER_ASK_AGAIN_LOCALE_NAME : PREFERENCE_KEY_NEVER_ASK_AGAIN_STORAGE_NAME;
        editor.putBoolean(key, b);
        editor.apply();
    }

    protected void requireBluetoothPowerOnDialog(final int rId) {
        if (mPermissionDialog != null && mPermissionDialog.isShowing()) {
            AppLog.d("PermissionDeniedDialog already showing, do nothing.");
            return;
        }
        mTerminalSettingsStage = TerminalSettingsStage.BLUETOOTH_CHECK;
        mPermissionDialog = new AlertDialog.Builder(getContext())
                .setMessage(rId)
                .setCancelable(false)
                .setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPermissionDialog.dismiss();
                        checkLocationState();
                    }
                })
                .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intent);
                    }
                })
                .show();
    }

    protected List<String> requestPermissionsIfNeeded(String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Arrays.asList(permissions);
        }

        ArrayList<String> alreadyGranted = new ArrayList<>();
        ArrayList<String> deniedPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (getPermissionNeverAskAgain(getContext(), permission)) {
                // Never ask again
                break;
            }
            if (getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                alreadyGranted.add(permission);
            } else {
                deniedPermissions.add(permission);
            }
        }

        if (!deniedPermissions.isEmpty() && alreadyGranted.isEmpty()) {
            requestPermissionDialog(deniedPermissions);
        }
        return alreadyGranted;
    }

    protected void requestPermissionDialog(final ArrayList<String> deniedPermissions) {
        if (mPermissionDialog != null && mPermissionDialog.isShowing()) {
            AppLog.d("PermissionInAdvanceDialog already showing, do nothing.");
            return;
        }
        int rId = R.string.permission_warning_in_advance_location;
        mTerminalSettingsStage = TerminalSettingsStage.LOCATION_PERMISSION_CHECK;
        mPermissionDialog = new AlertDialog.Builder(getContext())
                .setMessage(rId)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(
                                    deniedPermissions.toArray(new String[deniedPermissions.size()]),
                                    PERMISSIONS_REQUEST);
                        }
                    }
                })
                .show();
    }

    protected void checkLocationState(){
        mTerminalSettingsStage = TerminalSettingsStage.LOCATION_PERMISSION_CHECK;
        //Check permission
        List<String> alreadyGranted = requestPermissionsIfNeeded(new String[] {Manifest.permission.ACCESS_FINE_LOCATION});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String targetPermission = Manifest.permission.ACCESS_FINE_LOCATION;
            if (getActivity().checkSelfPermission(targetPermission) == PackageManager.PERMISSION_DENIED) {
                warningLocationPermissionShowDialog(R.string.permission_denied_warning_location);
            } else {
                //Location permission granted. Clear settings.
                setPermissionNeverAskAgain(getContext(), targetPermission, false);

                //Location on off check
                LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                final boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (gpsEnabled == true){
                    refreshView(mBluetoothPowerState);
                }else{
                    requestLocationTurnOnDialog(R.string.location_invalid_message);
                }
            }
        }
    }

    protected void warningLocationPermissionShowDialog(final int rId) {
        if (mPermissionDialog != null && mPermissionDialog.isShowing()) {
            AppLog.d("PermissionDeniedDialog already showing, do nothing.");
            return;
        }
        mTerminalSettingsStage = TerminalSettingsStage.LOCATION_PERMISSION_REQUEST;
        mPermissionDialog = new AlertDialog.Builder(getContext())
                .setMessage(rId)
                .setCancelable(false)
                .setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppLog.i("dialogOnClick:" + getString(R.string.skip));
                        mPermissionDialog.dismiss();
                        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                        final boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        if (gpsEnabled == true){
                            mTerminalSettingsStage = TerminalSettingsStage.CHECK_COMPLETE;
                            refreshView(mBluetoothPowerState);
                        }else{
                            requestLocationTurnOnDialog(R.string.location_invalid_message);
                        }
                    }
                })
                .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppLog.i("dialogOnClick:" + getString(R.string.settings));
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                        startActivity(intent);
                    }
                })
                .show();
    }

    protected void requestLocationTurnOnDialog(final int rId) {
        if (mPermissionDialog != null && mPermissionDialog.isShowing()) {
            AppLog.d("PermissionDeniedDialog already showing, do nothing.");
            return;
        }
        mTerminalSettingsStage = TerminalSettingsStage.LOCATION_CHECK;
        mPermissionDialog = new AlertDialog.Builder(getContext())
                .setMessage(rId)
                .setCancelable(false)
                .setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPermissionDialog.dismiss();
                        mTerminalSettingsStage = TerminalSettingsStage.CHECK_COMPLETE;
                        refreshView(mBluetoothPowerState);
                    }
                })
                .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .show();
    }

    protected TerminalSettingsStage checkSettingsState(boolean bluetoothPowerState){
        mBluetoothPowerState = bluetoothPowerState;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED) {
                //Location permission granted. Clear settings.
                setPermissionNeverAskAgain(getContext(), Manifest.permission.ACCESS_FINE_LOCATION, false);
            }
        }

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!bluetoothPowerState && mTerminalSettingsStage.equals(TerminalSettingsStage.BLUETOOTH_CHECK)) {
            //Bluetooth Off
            requireBluetoothPowerOnDialog(R.string.bluetooth_invalid_message);
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED &&
                mTerminalSettingsStage.compareTo(TerminalSettingsStage.LOCATION_PERMISSION_CHECK) <= 0) {
            //Location permission denied
            checkLocationState();
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED &&
                mTerminalSettingsStage.compareTo(TerminalSettingsStage.LOCATION_PERMISSION_REQUEST) <= 0) {
            //Location permission denied
            warningLocationPermissionShowDialog(R.string.permission_denied_warning_location);
        }else if (gpsEnabled == false && mTerminalSettingsStage.compareTo(TerminalSettingsStage.LOCATION_CHECK) <= 0){
            //Location Off
            requestLocationTurnOnDialog(R.string.location_invalid_message);
        }else{
            mTerminalSettingsStage = TerminalSettingsStage.CHECK_COMPLETE;
        }
        return mTerminalSettingsStage;
    }

    protected void refreshView(boolean bluetoothPowerState) {
    }

    protected  void startScan(@NonNull ScanController scanController){
        outputLog();
        scanController.startScan();
    }

    protected void outputLog(){
        switch (OHQDeviceManager.sharedInstance().state()) {
            case PoweredOn:
                AppLog.d("BluetoothSettings:ON");
                break;
            case PoweredOff:
                AppLog.d("BluetoothSettings:OFF");
                break;
        }

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            AppLog.d("LocationSettings:ON");
        }else{
            AppLog.d("LocationSettings:OFF");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                AppLog.d("LocationPermission:Denied");
            } else {
                AppLog.d("LocationPermission:Granted");
            }
        }
    }
}
