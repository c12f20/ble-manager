package com.hill.blemanager;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.hill.libblemanager.BLEData;
import com.hill.libblemanager.BLEDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public final class BLEDeviceManager {
    private static BLEDeviceManager sInstance = null;

    public static BLEDeviceManager getInstance() {
        if (sInstance == null) {
            sInstance = new BLEDeviceManager();
        }
        return sInstance;
    }

    private BLEDeviceManager() {
    }

    private BLEDevice mCurDevice = null;
    public void setCurDevice(final BLEDevice device) {
        mCurDevice = device;
    }

    public BLEDevice getCurDevice() {
        return mCurDevice;
    }

    private BluetoothGattService mCurService = null;
    public void setCurService(final BluetoothGattService service) {
        mCurService = service;
    }

    public BluetoothGattService getCurService() {
        return mCurService;
    }

    private BluetoothGattCharacteristic mCurCharacter = null;
    public void setCurCharacter(final BluetoothGattCharacteristic character) {
        mCurCharacter = character;
    }

    public BluetoothGattCharacteristic getCurCharacter() {
        return mCurCharacter;
    }

    private BluetoothGattDescriptor mCurDescriptor = null;
    public void setCurDescriptor(final BluetoothGattDescriptor descriptor) {
        mCurDescriptor = descriptor;
    }

    public BluetoothGattDescriptor getCurDescriptor() {
        return mCurDescriptor;
    }


    private Stack<List<BLEData>> mDataInfoStack = new Stack<List<BLEData>>();
    public void pushBLEDataList(final List<BLEData> bleDataList) {
        final List<BLEData> oldBLEDataList = new ArrayList<BLEData>();
        oldBLEDataList.addAll(bleDataList);
        mDataInfoStack.push(oldBLEDataList);
    }

    public List<BLEData> popBLEDataList() {
        if (!mDataInfoStack.isEmpty()) {
            return mDataInfoStack.pop();
        } else {
            return null;
        }
    }
}
