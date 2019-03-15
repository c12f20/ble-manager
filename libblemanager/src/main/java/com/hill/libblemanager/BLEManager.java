package com.hill.libblemanager;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BLEManager {
    private static final String TAG = "BLEManager";

    private static BLEManager sInstance = null;

    public static BLEManager getInstance() {
        if (sInstance == null) {
            sInstance = new BLEManager();
        }
        return sInstance;
    }

    private BluetoothAdapter getAdapter(final Context context) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return null;
        }
        return bluetoothManager.getAdapter();
    }
    // BLE enable methods

    /**
     * <p>Method to check whether Bluetooth LE feature is enabled on the Android device
     * @param context Application context
     * @return Whether the BLE feature is enabled
     */
    public boolean isBLEEnabled(final Context context) {
        final BluetoothAdapter adapter = getAdapter(context);
        return adapter != null && adapter.isEnabled();
    }

    private int REQUEST_CODE_ENABLE_BLE = 0xFFFF;
    private int REQUEST_CODE_REQUEST_LOACTION_PERMISSION = 0xFFFE;

    /**
     * <p>Method to open Request Bluetooth LE permission dialog to ask user to allow to enable the feature.
     * It's usually called when {@link #isBLEEnabled} return false
     * @param activity The Activity to open the dialog
     */
    public void enableBLE(final Activity activity) {
        final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, REQUEST_CODE_ENABLE_BLE);
    }

    /**
     * <p>Method to check whether the request called by {@link #enableBLE} is allowed by user.
     * It shall be called in {@link Activity#onActivityResult} callback method
     * @param context Application context
     * @param requestCode The requestCode parameter of {@link Activity#onActivityResult} method
     * @return Whether Bluetooth LE feature is enable or not.
     */
    public boolean onEnableBLEResult(final Context context, final int requestCode) {
        if (requestCode != REQUEST_CODE_ENABLE_BLE) {
            return false;
        }
        return isBLEEnabled(context);
    }

    /**
     * <p>Method to check whether AccessFineLocation permission is allowed by user
     * @param context Application context
     * @return Whether is granted the permission
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean hasLocationPermissions(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * <p>Method to open Request AccessFineLocation permission dialog to ask user to allow it.
     * @param activity The Activity to open the dialog
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestLocationPermissions(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_REQUEST_LOACTION_PERMISSION);
    }

    /**
     * <p>Method to check whether the request called by {@link #requestLocationPermissions} is allowed by user.
     * It shall be called in {@link Activity#onActivityResult} callback method.
     * @param context Application context
     * @param requestCode The requestCode parameter of {@link Activity#onActivityResult} method
     * @return Whether AccessFineLocation permission is allowed or not.
     */
    public boolean onRequestLocationResult(final Context context, final int requestCode) {
        if (requestCode != REQUEST_CODE_REQUEST_LOACTION_PERMISSION) {
            return false;
        }
        return hasLocationPermissions(context);
    }

    // BLE scan methods
    public static final int SCAN_ERROR_CODE_SUCCESS = 0;
    public static final int SCAN_ERROR_CODE_GENERAL_FAILURE = -1;
    public static final int SCAN_ERROR_CODE_ALREADY_START = -2;
    public static final int SCAN_ERROR_CODE_REGISTRATION_FAILURE = -3;
    public static final int SCAN_ERROR_CODE_UNSUPPORTED = -4;

    public interface ScanResultListener {
        /**
         * Callback method when we get something after calling {@link #startScan}
         * @param errorCode The error code, can be {@link #SCAN_ERROR_CODE_SUCCESS}, {@link #SCAN_ERROR_CODE_GENERAL_FAILURE}
         * {@link #SCAN_ERROR_CODE_ALREADY_START}, {@link #SCAN_ERROR_CODE_REGISTRATION_FAILURE}, {@link #SCAN_ERROR_CODE_UNSUPPORTED}
         * @param device The found Bluetooth LE device {@link BLEDevice} object
         */
        void onScanResult(final int errorCode, final BLEDevice device);
    }
    private ScanResultListener mScanResultListener = null;
    private void notifyGotScanResult(final BLEDevice device) {
        if (mScanResultListener != null) {
            mScanResultListener.onScanResult(SCAN_ERROR_CODE_SUCCESS, device);
        }
    }

    private void notifyScanResultFailure(final int errorCode) {
        if (mScanResultListener != null) {
            mScanResultListener.onScanResult(errorCode, null);
        }
    }

    /**
     * <p>Method to scan BLE devices
     * @param context Application context
     * @param listener Listener to receive the scan result. See {@link ScanResultListener} for details
     */
    public void startScan(final Context context, final ScanResultListener listener) {
        startScan(context, null, listener);
    }

    /**
     * <p>Method to scan BLE devices with GATT Services UUIDs filter
     * @param context Application context
     * @param uuids UUID List to filter out the Bluetooth LE devices with these UUIDs
     * @param listener Listener to receive the scan result. See {@link ScanResultListener} for details
     */
    public void startScan(final Context context, final UUID[] uuids, final ScanResultListener listener) {
        mScanResultListener = listener;
        final BluetoothAdapter adapter = getAdapter(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            startScanOldVersion(adapter, uuids);
        } else {
            startScanNewVersion(adapter, uuids);
        }
    }

    /**
     * <p>Method to stop scanning BLE devices, the scan will start forever until you call this method.
     * Please note it will consume a lot of device power if you keep doing the scan.
     * @param context Application context
     */
    public void stopScan(final Context context) {
        final BluetoothAdapter adapter = getAdapter(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            stopScanOldVersion(adapter);
        } else {
            stopScanNewVersion(adapter);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;
    private void startScanOldVersion(final BluetoothAdapter adapter, final UUID[] uuids) {
        if (mLeScanCallback == null) {
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final BLEDevice bleDevice = new BLEDevice(device, rssi);
                    notifyGotScanResult(bleDevice);
                }
            };
        }
        if (uuids != null) {
            adapter.startLeScan(uuids, mLeScanCallback);
        } else {
            adapter.startLeScan(mLeScanCallback);
        }
    }
    private void stopScanOldVersion(final BluetoothAdapter adapter) {
        if (mLeScanCallback != null) {
            adapter.stopLeScan(mLeScanCallback);
            mLeScanCallback = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private List<ScanFilter> buildScanFilters(final UUID[] uuids) {
        if (uuids == null || uuids.length == 0) {
            return null;
        }
        UUID serviceUUID = uuids[0];
        UUID maskUUID = new UUID(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
        for (int i=1; i < uuids.length; i++) {
            final UUID uuid = uuids[i];
            final UUID diffUUID = new UUID(~(serviceUUID.getMostSignificantBits() ^ uuid.getMostSignificantBits()),
                    ~(serviceUUID.getLeastSignificantBits() ^ uuid.getLeastSignificantBits()));
            serviceUUID = new UUID(serviceUUID.getMostSignificantBits() | uuid.getMostSignificantBits(),
                    serviceUUID.getLeastSignificantBits() | uuid.getLeastSignificantBits());
            maskUUID = new UUID(maskUUID.getMostSignificantBits() & diffUUID.getMostSignificantBits(),
                    maskUUID.getLeastSignificantBits() & diffUUID.getLeastSignificantBits());
        }
        final List<ScanFilter> filters = new ArrayList<ScanFilter>();
        final ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(serviceUUID), new ParcelUuid(maskUUID))
                .build();
        filters.add(filter);
        return filters;
    }

    private ScanCallback mScanCallback = null;
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScanNewVersion(final BluetoothAdapter adapter, final UUID[] uuids) {
        if (mScanCallback == null) {
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    Log.d(TAG, "onScanResult, device: "+result.getDevice().getAddress()+" rssi: "+result.getRssi()+" name: "+result.getScanRecord().getDeviceName());
                    final BLEDevice bleDevice = new BLEDevice(result.getDevice(), result.getRssi());
                    notifyGotScanResult(bleDevice);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    Log.d(TAG, "onBatchScanResults");
                    for (final ScanResult result : results) {
                        final BLEDevice bleDevice = new BLEDevice(result.getDevice(), result.getRssi());
                        notifyGotScanResult(bleDevice);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.d(TAG, "onScanFailed: "+errorCode);
                    int bleErrorCode = SCAN_ERROR_CODE_GENERAL_FAILURE;
                    switch(errorCode) {
                        case SCAN_FAILED_ALREADY_STARTED:
                            bleErrorCode = SCAN_ERROR_CODE_ALREADY_START;
                            break;
                        case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                            bleErrorCode = SCAN_ERROR_CODE_REGISTRATION_FAILURE;
                            break;
                        case SCAN_FAILED_FEATURE_UNSUPPORTED:
                            bleErrorCode = SCAN_ERROR_CODE_UNSUPPORTED;
                            break;
                        case SCAN_FAILED_INTERNAL_ERROR:
                            bleErrorCode = SCAN_ERROR_CODE_GENERAL_FAILURE;
                            break;
                        default:
                            Log.e(TAG, "Unknow ScanFailed error code: "+errorCode);
                            break;
                    }
                    notifyScanResultFailure(bleErrorCode);
                }
            };
        }

        final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

        final List<ScanFilter> filters = buildScanFilters(uuids);
        final ScanSettings settings = new ScanSettings.Builder().build();
        scanner.startScan(filters, settings, mScanCallback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void stopScanNewVersion(final BluetoothAdapter adapter) {
        final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (mScanCallback != null) {
            scanner.stopScan(mScanCallback);
            mScanCallback = null;
        }
    }
}
