package com.hill.blemanager;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hill.libblemanager.BLEDevice;

import java.util.ArrayList;
import java.util.List;

public final class BLEDeviceAdapter extends BaseAdapter {

    private final Context mContext;
    public BLEDeviceAdapter(final Context context) {
        mContext = context;
    }

    private final List<BLEDevice> mDevicesList = new ArrayList<BLEDevice>();

    private BLEDevice findDevice(final BluetoothDevice btDevice) {
        for (final BLEDevice device : mDevicesList) {
            if (device.equals(btDevice)) {
                return device;
            }
        }
        return null;
    }

    public void addDevice(final BluetoothDevice btDevice, final int rssi) {
        BLEDevice device = findDevice(btDevice);
        if (device != null) {
            device.deviceRssi = rssi;
        } else {
            device = new BLEDevice(btDevice, rssi);
            mDevicesList.add(device);
        }
        notifyDataSetChanged();
    }

    public void clearDevices() {
        mDevicesList.clear();
        notifyDataSetChanged();
    }

    public List<BLEDevice> getDeviceList() {
        return mDevicesList;
    }

    @Override
    public int getCount() {
        return mDevicesList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < 0 || position >= mDevicesList.size()) {
            return null;
        }
        return mDevicesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ItemView extends LinearLayout {
        private TextView mTextName;
        private TextView mTextRssi;
        private ItemView(Context context) {
            super(context);
            final View rootView = LayoutInflater.from(context).inflate(R.layout.view_device_item, this, true);
            mTextName = rootView.findViewById(R.id.textDeviceName);
            mTextRssi = rootView.findViewById(R.id.textDeviceRSSI);
        }

        private void updateInfo(final BLEDevice device) {
            mTextName.setText(device.getDisplayName());
            mTextRssi.setText(String.valueOf(device.deviceRssi));
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position < 0 || position >= mDevicesList.size()) {
            return null;
        }
        final ItemView itemView;
        if (convertView instanceof ItemView) {
            itemView = (ItemView) convertView;
        } else {
            itemView = new ItemView(mContext);
        }
        final BLEDevice device = mDevicesList.get(position);
        itemView.updateInfo(device);
        return itemView;
    }
}
