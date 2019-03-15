package com.hill.blemanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hill.libblemanager.BLEData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BLEDataAdapter extends BaseAdapter {

    private final Context mContext;
    public BLEDataAdapter(final Context context) {
        mContext = context;
    }

    private List<BLEData> mBLEDataList = new ArrayList<BLEData>();

    public void updateBLEDataList(final List<BLEData> bleDataList) {
        mBLEDataList.clear();
        mBLEDataList.addAll(bleDataList);

        notifyDataSetChanged();
    }

    public List<BLEData> getBLEDataList() {
        return mBLEDataList;
    }

    @Override
    public int getCount() {
        return mBLEDataList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < 0 || position >= mBLEDataList.size()) {
            return null;
        }
        return mBLEDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ItemView extends LinearLayout {
        private final TextView mTextUUID;
        private final TextView mTextValue;
        private ItemView(final Context context) {
            super(context);
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View rootView = inflater.inflate(R.layout.view_data_item, this, true);

            mTextUUID = rootView.findViewById(R.id.textUUID);
            mTextValue = rootView.findViewById(R.id.textValue);
        }

        private String getDisplayUUID(final @BLEData.DataType int type, final UUID uuid) {
            switch (type) {
                case BLEData.DATATYPE_SERVICE:
                    return "["+uuid.toString()+"]";
                case BLEData.DATATYPE_CHARACTER:
                    return "<"+uuid.toString()+">";
                case BLEData.DATATYPE_DESCRIPTOR:
                    return "{"+uuid.toString()+"}";
                case BLEData.DATATYPE_UNKNOWN:
                default:
                    return uuid.toString();
            }
        }

        private String getDisplayValue(final BLEData data) {
            final byte[] bytesValue = data.getValue();
            if (bytesValue == null || bytesValue.length == 0) {
                return "";
            }
            return "["+bytesToString(bytesValue)+"] str: "+data.getStringValue(0);
        }

        private void updateInfo(final BLEData data) {
            mTextUUID.setText(getDisplayUUID(data.dataType, data.uuid));
            mTextValue.setText(getDisplayValue(data));
        }

        private String bytesToString(final byte[] bytes) {
            if (bytes == null) {
                return "";
            }
            final StringBuilder sb = new StringBuilder();
            for (final byte dataByte : bytes) {
                sb.append(String.format("%02X", dataByte));
            }
            return sb.toString();
        }

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position < 0 || position >= mBLEDataList.size()) {
            return null;
        }
        final ItemView itemView;
        if (convertView instanceof ItemView) {
            itemView = (ItemView) convertView;
        } else {
            itemView = new ItemView(mContext);
        }
        final BLEData data = mBLEDataList.get(position);
        itemView.updateInfo(data);
        return itemView;
    }
}
