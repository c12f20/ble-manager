package com.hill.blemanager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hill.libblemanager.BLEData;
import com.hill.libblemanager.BLEDevice;

import java.util.List;

public class DeviceInfoActivity extends AppCompatActivity {
    private static final String TAG = "DeviceInfoActivity";

    private Handler mHandler = null;
    private BLEDeviceManager mBLEDevMng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        mHandler = new Handler();
        mBLEDevMng = BLEDeviceManager.getInstance();

        initView();

        connectDevice();

        startQueryRSSI();
    }

    @Override
    protected void onDestroy() {
        stopQueryRSSI();
        disconnectDevice();
        super.onDestroy();
    }

    private TextView mTextTitle = null;
    private TextView mTextRSSI = null;
    private BLEDataAdapter mDataAdapter = null;
    private ProgressBar mProgressBarLoading = null;
    private void initView() {
        mTextTitle = findViewById(R.id.textTitle);
        mTextRSSI = findViewById(R.id.textRSSI);

        final ListView listData = findViewById(R.id.listData);
        mDataAdapter = new BLEDataAdapter(getBaseContext());
        listData.setAdapter(mDataAdapter);
        listData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final List<BLEData> bleDataList = mDataAdapter.getBLEDataList();
                final BLEData bleData = bleDataList.get(position);
                processBLEDataClicked(bleData);
            }
        });

        mProgressBarLoading = findViewById(R.id.progressBarLoading);
    }

    private void updateTitle(final BluetoothDevice device) {
        final String name = device.getName();
        final String address = device.getAddress();
        if (TextUtils.isEmpty(name)) {
            mTextTitle.setText(address);
        } else {
            mTextTitle.setText(String.format("%s (%s)", name, address));
        }
    }

    private void updateRSSI(final int rssi) {
        mTextRSSI.setText(String.valueOf(rssi));
    }

    private List<BLEData> mCurDataList = null;
    private void updateDataList(final List<BLEData> dataList) {
        mCurDataList = dataList;
        mDataAdapter.updateBLEDataList(dataList);
    }

    private void appendDataList(final List<BLEData> dataList) {
        mCurDataList.addAll(dataList);
        mDataAdapter.updateBLEDataList(dataList);
    }

    private void pushDataList(final List<BLEData> dataList) {
        if (dataList != null) {
            mBLEDevMng.pushBLEDataList(dataList);
        }
    }

    private void showLoading() {
        mProgressBarLoading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        mProgressBarLoading.setVisibility(View.GONE);
    }

    private void processBLEDataClicked(final BLEData data) {
        if (data.data instanceof BluetoothGattService) {
            listService(data);
        } else if (data.data instanceof BluetoothGattCharacteristic) {
            listDescriptors(data);
        } else if (data.data instanceof BluetoothGattDescriptor) {
        }
    }

    @Override
    public void onBackPressed() {
        final List<BLEData> prevBLEDataList = mBLEDevMng.popBLEDataList();
        if (prevBLEDataList == null) {
            super.onBackPressed();
            return;
        }
        updateDataList(prevBLEDataList);
    }

    // Connect methods
    private BLEDevice mDevice = null;
    private void connectDevice() {
        mDevice = mBLEDevMng.getCurDevice();
        if (mDevice == null) {
            Log.e(TAG, "No device is set yet");
            finish();
            return;
        }
        showLoading();
        updateTitle(mDevice.btDevice);
        updateRSSI(mDevice.deviceRssi);
        mDevice.connect(getBaseContext(), false, new BLEDevice.ConnectionListener() {
            @Override
            public void onConnectionStateChanged(boolean connected) {
                if (connected) {
                    queryServices();
                }
            }
        });
    }

    private void disconnectDevice() {
        if (mDevice != null) {
            mDevice.disconnect();
        }
    }

    // Query RSSI methods
    private BLEDevice.QueryRssiValueListener mQueryRssiValueListener = new BLEDevice.QueryRssiValueListener() {
        @Override
        public void onGotRssi(final boolean success, final int rssi) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onGotRssi, success: "+success+" rssi: "+rssi);
                    if (success) {
                        mDevice.deviceRssi = rssi;
                        updateRSSI(rssi);
                    }
                }
            });
        }
    };

    private Runnable mQueryRSSIRunnable = null;
    private void startQueryRSSI() {
        if (mQueryRSSIRunnable == null) {
            mQueryRSSIRunnable = new Runnable() {
                @Override
                public void run() {
                    mDevice.queryRemoteRssi(mQueryRssiValueListener);
                    queryRSSIDelayed(DEFAULT_QUERY_RSSI_INTERVAL);
                }
            };
            queryRSSIDelayed(0);
        }
    }

    private static final long DEFAULT_QUERY_RSSI_INTERVAL = 1000;
    private void queryRSSIDelayed(final long timeout) {
        if (mQueryRSSIRunnable == null) {
            return;
        }
        if (timeout > 0) {
            mHandler.postDelayed(mQueryRSSIRunnable, timeout);
        } else {
            mHandler.post(mQueryRSSIRunnable);
        }
    }

    private void stopQueryRSSI() {
        if (mQueryRSSIRunnable != null) {
            mHandler.removeCallbacks(mQueryRSSIRunnable);
            mQueryRSSIRunnable = null;
        }
    }

    // Service related methods
    private void queryServices() {
        mDevice.queryServices(new BLEDevice.QueryResultsListener() {
            @Override
            public void onGotResults(final List<BLEData> dataList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                        if (dataList != null) {
                            updateDataList(dataList);
                        }
                    }
                });
            }
        });
    }

    private void listService(final BLEData serviceData) {
        showLoading();
        pushDataList(mCurDataList);
        listIncludedService(serviceData);
    }

    private void listIncludedService(final BLEData serviceData) {
        mDevice.queryIncludedServices(serviceData, new BLEDevice.QueryResultsListener() {
            @Override
            public void onGotResults(final List<BLEData> dataList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dataList != null) {
                            updateDataList(dataList);
                            listCharacteristics(serviceData);
                        } else {
                            hideLoading();
                        }
                    }
                });
            }
        });
    }

    private void listCharacteristics(final BLEData serviceData) {
        mDevice.queryCharacters(serviceData, true, new BLEDevice.QueryResultsListener() {
            @Override
            public void onGotResults(final List<BLEData> dataList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                        if (dataList != null) {
                            appendDataList(dataList);
                        }
                    }
                });
            }
        });
    }

    // Characters related methods
    private void listDescriptors(final BLEData characterData) {
        showLoading();
        pushDataList(mCurDataList);
        mDevice.queryDescriptors(characterData, true, new BLEDevice.QueryResultsListener() {
            @Override
            public void onGotResults(final List<BLEData> dataList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                        if (dataList != null) {
                            updateDataList(dataList);
                        }
                    }
                });
            }
        });
    }
}
