package com.hill.blemanager;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.hill.libblemanager.BLEDevice;
import com.hill.libblemanager.BLEManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BLEManager mBLEMng = null;
    private BLEDeviceManager mBLEDevMng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBLEMng = BLEManager.getInstance();
        mBLEDevMng = BLEDeviceManager.getInstance();

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private Button mBtnScan = null;
    private BLEDeviceAdapter mDeviceAdapter = null;
    private void initView() {
        mBtnScan = findViewById(R.id.btnScan);
        mBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScanStatus();
            }
        });

        final ListView listDevices = findViewById(R.id.listDevices);
        mDeviceAdapter = new BLEDeviceAdapter(getBaseContext());
        listDevices.setAdapter(mDeviceAdapter);
        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final List<BLEDevice> deviceList = mDeviceAdapter.getDeviceList();
                if (position < 0 || position > deviceList.size()-1) {
                    return;
                }
                connectDevice(deviceList.get(position));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (mBLEMng.onEnableBLEResult(getBaseContext(), requestCode)
                || mBLEMng.onRequestLocationResult(getBaseContext(), requestCode)) {
            toggleScanStatus();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Scan methods
    private BLEManager.ScanResultListener mScanResultListener = new BLEManager.ScanResultListener() {
        @Override
        public void onScanResult(int errorCode, BLEDevice device) {
            if (errorCode == BLEManager.SCAN_ERROR_CODE_SUCCESS && device != null) {
                mDeviceAdapter.addDevice(device.btDevice, device.deviceRssi);
            }
        }
    };

    private boolean mIsScanning = false;
    private void toggleScanStatus() {
        if (mIsScanning) {
            mIsScanning = false;
            stopScan();
        } else {
            mIsScanning = true;
            startScan();
        }
    }

    private void startScan() {
        if (!hasPermissions()) {
            return;
        }
        // clear previous result
        mDeviceAdapter.clearDevices();
        // Update UI
        mBtnScan.setText(R.string.ID_CAPTION_STOP_SCAN);
        // Do scan
        mBLEMng.startScan(getBaseContext(), mScanResultListener);
    }

    private void stopScan() {
        // Do stop
        mBLEMng.stopScan(getBaseContext());
        // Update UI
        mBtnScan.setText(R.string.ID_CAPTION_START_SCAN);
    }

    private boolean hasPermissions() {
        if (!mBLEMng.isBLEEnabled(getBaseContext())) {
            mBLEMng.enableBLE(this);
            return false;
        }
        if (!mBLEMng.hasLocationPermissions(getBaseContext())) {
            mBLEMng.requestLocationPermissions(this);
            return false;
        }
        return true;
    }

    private void connectDevice(final BLEDevice device) {
        stopScan();

        mBLEDevMng.setCurDevice(device);

        final Intent intent = new Intent(getBaseContext(), DeviceInfoActivity.class);
        startActivity(intent);
    }
}
