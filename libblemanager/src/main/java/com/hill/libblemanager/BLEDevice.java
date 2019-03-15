package com.hill.libblemanager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public final class BLEDevice {
    private static final String TAG = "BLEDevice";

    public final BluetoothDevice btDevice;
    public int deviceRssi;

    /**
     * <p>Construction method of BLEDevice
     * @param device {@link BluetoothDevice} object of Android SDK
     * @param rssi RSSI value of this device
     */
    public BLEDevice(final BluetoothDevice device, final int rssi) {
        btDevice = device;
        deviceRssi = rssi;
    }

    /**
     * <p>Get device name or address when its name is empty
     * @return the name to identify this device
     */
    public String getDisplayName() {
        final String deviceName = btDevice.getName();
        if (!TextUtils.isEmpty(deviceName)) {
            return deviceName;
        }
        return btDevice.getAddress();
    }

    public boolean equals(final BLEDevice other) {
        return btDevice.getAddress().equals(other.btDevice.getAddress());
    }

    public boolean equals(final BluetoothDevice otherBTDevice) {
        return btDevice.getAddress().equals(otherBTDevice.getAddress());
    }

    // GATT related methods
    private BluetoothGatt mGatt = null;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onConnectionStateChange callback");
                return;
            }
            notifyConnectionStateChanged(newState == BluetoothGatt.STATE_CONNECTED);
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_DISCONNECTED) {
                mGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onServicesDiscovered callback");
                return;
            }
            processOnServicesDiscovered(status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onCharacteristicRead callback");
                return;
            }
            if (characteristic == null) {
                Log.e(TAG, "Ignore onCharacteristicRead callback with invalid characteristic parameter");
                return;
            }

            processOnCharacterReadForReadingCharactersList(characteristic, status);

            processOnCharacterReadForReadingCharacterValue(characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onCharacteristicWrite callback");
                return;
            }
            if (characteristic == null) {
                Log.e(TAG, "Ignore onCharacteristicWrite callback with invalid characteristic parameter");
                return;
            }

            processOnCharacterWriteForWritingCharacterValue(characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onCharacteristicChanged callback");
                return;
            }
            if (characteristic == null) {
                Log.e(TAG, "Ignore onCharacteristicChanged callback with invalid characteristic parameter");
                return;
            }

            notifyCharacterChanged(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onDescriptorRead callback");
                return;
            }
            if (descriptor == null) {
                Log.e(TAG, "Ignore onDescriptorRead callback with invalid descriptor parameter");
                return;
            }

            processOnDescriptorReadForReadingDescriptorsList(descriptor, status);

            processOnDescriptorReadForReadingDescriptorValue(descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onDescriptorWrite callback");
                return;
            }
            if (descriptor == null) {
                Log.e(TAG, "Ignore onDescriptorWrite callback with invalid descriptor parameter");
                return;
            }

            processOnDescriptorWriteForWritingDescriptorValue(descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (mGatt != gatt) {
                Log.w(TAG, "Ignore unrelated Gatt onReadRemoteRssi callback");
                return;
            }
            Log.d(TAG, "onReadRemoteRssi, rssi: "+rssi+" status: "+status);
            notifyGotRssiValue(status == BluetoothGatt.GATT_SUCCESS, rssi);
        }
    };

    // connection methods
    public interface ConnectionListener {
        void onConnectionStateChanged(final boolean connected);
    }
    private ConnectionListener mConnectionListener = null;
    private void notifyConnectionStateChanged(final boolean connected) {
        if (mConnectionListener != null) {
            mConnectionListener.onConnectionStateChanged(connected);
        }
    }

    /**
     * <p>Method to connect to this device
     * @param context Application context
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     * automatically connect as soon as the remote device becomes available (true).
     * @param listener Listener to monitor the connection result/status
     */
    public void connect(final Context context, final boolean autoConnect, final ConnectionListener listener) {
        if (mGatt != null) {
            disconnect();
        }
        mConnectionListener = listener;
        mGatt = btDevice.connectGatt(context, autoConnect, mGattCallback);
    }

    /**
     * Method to disconnect from this device
     */
    public void disconnect() {
        if (mGatt != null) {
            mGatt.disconnect();
        }
    }

    // Query callback interfaces
    public interface QueryResultsListener {
        void onGotResults(final List<BLEData> dataList);
    }

    public interface QueryValueListener {
        void onGotValue(final BLEData data);
    }

    public interface QueryRssiValueListener {
        void onGotRssi(final boolean success, final int rssi);
    }

    public interface WriteValueListener {
        void onSetValue(final BLEData data);
    }

    // Query RSSI
    private QueryRssiValueListener mQueryRssiValueListener = null;
    private void notifyGotRssiValue(final boolean success, final int rssi) {
        if (mQueryRssiValueListener != null) {
            mQueryRssiValueListener.onGotRssi(success, rssi);
        }
    }

    /**
     * <p>Method to query RSSI value of this device
     * @param listener Listener to receive the RSSI value. Value 0 indicates invalid
     */
    public void queryRemoteRssi(final QueryRssiValueListener listener) {
        mQueryRssiValueListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifyGotRssiValue(false, 0);
            return;
        }
        if (!mGatt.readRemoteRssi()) {
            Log.e(TAG, "Failed to readRemoteRssi");
            notifyGotRssiValue(false, 0);
        }
    }

    // Query services
    private QueryResultsListener mQueryServicesListener = null;
    private void notifyGotServices(final List<BLEData> dataList) {
        if (mQueryServicesListener != null) {
            mQueryServicesListener.onGotResults(dataList);
        }
    }

    /**
     * <p>Method to query GATT Services of this device
     * @param listener Listener to receive the got services info
     */
    public void queryServices(final QueryResultsListener listener) {
        mQueryServicesListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifyGotServices(null);
            return;
        }
        final List<BluetoothGattService> servicesList = mGatt.getServices();
        if (!servicesList.isEmpty()) {
            notifyGotServices(buildBLEDataListFromServicesList(servicesList));
            return;
        }
        if (!mGatt.discoverServices()) {
            Log.e(TAG, "Failed to discover BLE services");
            notifyGotServices(null);
            return;
        }
    }

    private void processOnServicesDiscovered(final int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "processOnServicesDiscovered, failed to discover services");
            notifyGotServices(null);
            return;
        }
        final List<BluetoothGattService> services = mGatt.getServices();
        notifyGotServices(buildBLEDataListFromServicesList(services));
    }

    private List<BLEData> buildBLEDataListFromServicesList(final List<BluetoothGattService> services) {
        final ArrayList<BLEData> bleDataList = new ArrayList<BLEData>();
        for (final BluetoothGattService service : services) {
            final BLEData bleData = new BLEData(service.getUuid(), service);
            bleDataList.add(bleData);
        }
        return bleDataList;
    }

    /**
     * <p>Method to query included GATT Services in the GATT Service
     * @param serviceData The GATT Service BLEData object
     * @param listener Listener to receive the included services info
     */
    public void queryIncludedServices(final BLEData serviceData, final QueryResultsListener listener) {
        mQueryServicesListener = listener;
        final BluetoothGattService service = (BluetoothGattService) serviceData.data;
        final List<BluetoothGattService> includedServices = service.getIncludedServices();
        notifyGotServices(buildBLEDataListFromServicesList(includedServices));
    }

    // Query characters
    private QueryResultsListener mQueryCharactersListener = null;
    private void notifyGotCharacters(final List<BLEData> dataList) {
        if (mQueryCharactersListener != null) {
            mQueryCharactersListener.onGotResults(dataList);
        }
    }

    /**
     * <p>Method to query GATT Characteristic in the GATT Service
     * @param serviceData The GATT Service BLEData object to query
     * @param needValue Whether to get value of the got GATT Characteristics
     * @param listener Listener to receive the got GATT Characteristics
     */
    public void queryCharacters(final BLEData serviceData, final boolean needValue, final QueryResultsListener listener) {
        mQueryCharactersListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifyGotCharacters(null);
            return;
        }
        final BluetoothGattService service = (BluetoothGattService) serviceData.data;
        final List<BluetoothGattCharacteristic> characters = service.getCharacteristics();
        final List<BLEData> bleDataList = buildBLEDataListFromCharactersList(characters);
        if (!needValue) {
            notifyGotCharacters(bleDataList);
            return;
        }
        queryCharactersValues(bleDataList);
    }

    private int mQueryCharacterIndex = -1;
    private List<BLEData> mQueryCharactersData = null;
    private void queryCharactersValues(final List<BLEData> characters) {
        mQueryCharacterIndex = -1;
        mQueryCharactersData = characters;
        queryNextCharacter();
    }

    private void queryNextCharacter() {
        mQueryCharacterIndex++;
        if (mQueryCharacterIndex > mQueryCharactersData.size()-1) {
            notifyGotCharacters(mQueryCharactersData);
            // reset query characters list variables
            mQueryCharacterIndex = -1;
            mQueryCharactersData = null;
            return;
        }

        final BLEData curCharacterData = mQueryCharactersData.get(mQueryCharacterIndex);
        final BluetoothGattCharacteristic curCharacter = (BluetoothGattCharacteristic) curCharacterData.data;
        if (readCharacterValue(curCharacterData)) {
            queryNextCharacter();
            return;
        }
        if (!mGatt.readCharacteristic(curCharacter)) {
            Log.e(TAG, "queryNextCharacter, failed to readCharacteristic: "+curCharacter.getUuid());
            queryNextCharacter();
        }
    }

    private void processOnCharacterReadForReadingCharactersList(final BluetoothGattCharacteristic characteristic, final int status) {
        if (mQueryCharactersData == null
        || mQueryCharacterIndex < 0 || mQueryCharacterIndex > mQueryCharactersData.size()-1) { // We'are not process characters list reading, ignore it
            Log.d(TAG, "processOnCharacterReadForReadingCharactersList, not reading characters list, ignore it");
            return;
        }

        final BLEData curCharacterData = mQueryCharactersData.get(mQueryCharacterIndex);
        if (!curCharacterData.equals(characteristic.getUuid())) { // UUID doesn't match, ignore it
            Log.d(TAG, "processOnCharacterReadForReadingCharactersList, ignore unrelated character: "+characteristic.getUuid());
            return;
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "processOnCharacterReadForReadingCharactersList, failed to read character: "+characteristic.getUuid());
        } else {
            readCharacterValue(curCharacterData);
        }
        queryNextCharacter();
    }

    private boolean readCharacterValue(final BLEData characterData) {
        final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) characterData.data;
        final byte[] characterValue = character.getValue();
        if (characterValue != null && characterValue.length > 0) {
            return true;
        }
        return false;
    }

    private List<BLEData> buildBLEDataListFromCharactersList(final List<BluetoothGattCharacteristic> characters) {
        final ArrayList<BLEData> bleDataList = new ArrayList<BLEData>();
        for (final BluetoothGattCharacteristic character : characters) {
            final BLEData bleData = new BLEData(character.getUuid(), character);
            bleDataList.add(bleData);
        }
        return bleDataList;
    }

    // Query character item method
    private QueryValueListener mQueryCharacterValueListener = null;
    private void notifyGotCharacterValue(final BLEData data) {
        if (mQueryCharacterValueListener != null) {
            mQueryCharacterValueListener.onGotValue(data);
        }
    }

    private BLEData mQueryCharacterData = null;

    /**
     * <p>Method to query the value of the GATT
     * @param characterData The GATT Characteristic BLEData object to query
     * @param listener Listener to receive the value of the BLEData
     */
    public void queryCharacterData(final BLEData characterData, final QueryValueListener listener) {
        mQueryCharacterValueListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifyGotCharacterValue(null);
            return;
        }

        final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) characterData.data;
        if (readCharacterValue(characterData)) {
            notifyGotCharacterValue(characterData);
            return;
        }
        if (!mGatt.readCharacteristic(character)) {
            Log.e(TAG, "queryCharacterData, failed to readCharacteristic: "+character.getUuid());
            notifyGotCharacterValue(null);
            return;
        }
        mQueryCharacterData = characterData;
    }

    private void processOnCharacterReadForReadingCharacterValue(final BluetoothGattCharacteristic characteristic, final int status) {
        if (mQueryCharacterData == null) { // Not reading value, ignore it
            Log.d(TAG, "processOnCharacterReadForReadingCharacterValue, not reading character value, ignore it");
            return;
        }
        if (!mQueryCharacterData.equals(characteristic.getUuid())) { // UUID doesn't match
            Log.d(TAG, "processOnCharacterReadForReadingCharacterValue, ignore unrelated character: "+characteristic.getUuid());
            return;
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "processOnCharacterReadForReadingCharacterValue, failed to read character: "+characteristic.getUuid());
            notifyGotCharacterValue(null);
        } else {
            readCharacterValue(mQueryCharacterData);
            notifyGotCharacterValue(mQueryCharacterData);
        }
        // reset variable when got its related callback
        mQueryCharactersData = null;
    }

    // Write character item value
    private WriteValueListener mWriteCharacterValueListener = null;
    private void notifySetCharacterValue(final BLEData data) {
        if (mWriteCharacterValueListener != null) {
            mWriteCharacterValueListener.onSetValue(data);
        }
    }

    private BLEData mWriteCharacterData = null;
    /**
     * <p>Method to write value of the GATT Characteristic
     * @param characterData The Characteristic BLEData object to be written,
     * the written value is just the value returned by {@link BluetoothGattCharacteristic#getValue()}
     * @param writeType The write type to for this characteristic. Can be one of: {@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT},
     * {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE} or {@link BluetoothGattCharacteristic#WRITE_TYPE_SIGNED}.
     * @param listener The listener to get the result of this write action
     */
    public void writeCharacterData(final BLEData characterData, final int writeType, final WriteValueListener listener) {
        mWriteCharacterValueListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifySetCharacterValue(null);
            return;
        }
        final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) characterData.data;
        character.setWriteType(writeType);
        if (!mGatt.writeCharacteristic(character)) {
            notifySetCharacterValue(null);
            return;
        }
        if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            notifySetCharacterValue(characterData);
            return;
        }
        mWriteCharacterData = characterData;
    }

    /**
     * <p>Method to write value of the GATT Characteristic with default write type
     * {@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT}.
     * <p>See {@link #writeCharacterData} for details.
     * @param characterData The Characteristic BLEData object to be written
     * @param listener The listener to get the result of this write action
     */
    public void writeCharacterData(final BLEData characterData, final WriteValueListener listener) {
        writeCharacterData(characterData, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, listener);
    }

    private void processOnCharacterWriteForWritingCharacterValue(final BluetoothGattCharacteristic characteristic, final int status) {
        if (mWriteCharacterData == null) { // Not reading value, ignore it
            Log.d(TAG, "processOnCharacterWriteForWritingCharacterValue, not writing character value, ignore it");
            return;
        }
        if (!mWriteCharacterData.equals(characteristic.getUuid())) { // UUID doesn't match
            Log.d(TAG, "processOnCharacterWriteForWritingCharacterValue, ignore unrelated character: "+characteristic.getUuid());
            return;
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "processOnCharacterWriteForWritingCharacterValue, failed to write character: "+characteristic.getUuid());
            notifySetCharacterValue(null);
        } else {
            notifySetCharacterValue(mWriteCharacterData);
        }
        mWriteCharacterData = null;
    }

    // Query descriptors methods
    private QueryResultsListener mQueryDescriptorsListener = null;
    private void notifyGotDescriptors(final List<BLEData> dataList) {
        if (mQueryDescriptorsListener != null) {
            mQueryDescriptorsListener.onGotResults(dataList);
        }
    }

    /**
     * <p>Method to query GATT Descriptors info in the GATT Characteristic
     * @param characterData The GATT Characteristic BLEData object to query
     * @param needValue Whether need get values of the GATT Descriptors if they have
     * @param listener Listener to receive the got GATT Descriptors info
     */
    public void queryDescriptors(final BLEData characterData, final boolean needValue, final QueryResultsListener listener) {
        mQueryDescriptorsListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifyGotDescriptors(null);
            return;
        }
        final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) characterData.data;
        final List<BluetoothGattDescriptor> descriptors = character.getDescriptors();

        final List<BLEData> bleDataList = buildBLEDataListFromDescriptorsList(descriptors);
        if (!needValue) {
            notifyGotDescriptors(bleDataList);
            return;
        }
        queryDescriptorsValues(bleDataList);
    }

    private List<BLEData> buildBLEDataListFromDescriptorsList(final List<BluetoothGattDescriptor> characters) {
        final ArrayList<BLEData> bleDataList = new ArrayList<BLEData>();
        for (final BluetoothGattDescriptor descriptor : characters) {
            final BLEData bleData = new BLEData(descriptor.getUuid(), descriptor);
            bleDataList.add(bleData);
        }
        return bleDataList;
    }

    private int mQueryDescriptorIndex = -1;
    private List<BLEData> mQueryDescriptorsData = null;
    private void queryDescriptorsValues(final List<BLEData> descriptors) {
        mQueryDescriptorIndex = -1;
        mQueryDescriptorsData = descriptors;
        queryNextDescriptor();
    }

    private void queryNextDescriptor() {
        mQueryDescriptorIndex++;
        if (mQueryDescriptorIndex > mQueryDescriptorsData.size()-1) {
            notifyGotDescriptors(mQueryCharactersData);
            // reset query descriptors list variables
            mQueryDescriptorIndex = -1;
            mQueryDescriptorsData = null;
            return;
        }
        final BLEData curDescriptorData = mQueryDescriptorsData.get(mQueryDescriptorIndex);
        final BluetoothGattDescriptor curDescriptor = (BluetoothGattDescriptor) curDescriptorData.data;
        if (readDescriptorValue(curDescriptorData)) {
            queryNextDescriptor();
            return;
        }
        if (!mGatt.readDescriptor(curDescriptor)) {
            Log.e(TAG, "queryNextDescriptor, failed to readDescriptor: "+curDescriptor.getUuid());
            queryNextDescriptor();
        }
    }

    private boolean readDescriptorValue(final BLEData descriptorData) {
        final BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) descriptorData.data;
        final byte[] descriptorValue = descriptor.getValue();
        if (descriptorValue != null && descriptorValue.length > 0) {
            return true;
        }
        return false;
    }

    private void processOnDescriptorReadForReadingDescriptorsList(final BluetoothGattDescriptor descriptor, final int status) {
        if (mQueryDescriptorsData == null
        || mQueryDescriptorIndex < 0 || mQueryDescriptorIndex > mQueryDescriptorsData.size()-1) { // We'are not process descriptor list reading, ignore it
            Log.d(TAG, "processOnDescriptorForReadingDescriptorsList, not reading descriptors list, ignore it");
            return;
        }

        final BLEData curDescriptorData = mQueryDescriptorsData.get(mQueryDescriptorIndex);
        if (!curDescriptorData.equals(descriptor.getUuid())) { // UUID doesn't match, ignore it
            Log.d(TAG, "processOnDescriptorForReadingDescriptorsList, ignore unrelated character: "+descriptor.getUuid());
            return;
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "processOnDescriptorForReadingDescriptorsList, failed to read character: "+descriptor.getUuid());
        } else {
            readDescriptorValue(curDescriptorData);
        }
        queryNextDescriptor();
    }

    // Query descriptor item methods
    private QueryValueListener mQueryDescriptorValueListener = null;
    private void notifyGotDescriptorValue(final BLEData data) {
        if (mQueryDescriptorValueListener != null) {
            mQueryDescriptorValueListener.onGotValue(data);
        }
    }

    private BLEData mQueryDescriptorData = null;

    /**
     * <p>Method to query value of the GATT Descriptor
     * @param descriptorData The GATT Descriptor BLEData object
     * @param listener Listener to receive the got value of the GATT Descriptor
     */
    public void queryDescriptorData(final BLEData descriptorData, final QueryValueListener listener) {
        mQueryDescriptorValueListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifyGotDescriptorValue(null);
            return;
        }

        final BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) descriptorData.data;
        if (readDescriptorValue(descriptorData)) {
            notifyGotDescriptorValue(descriptorData);
            return;
        }
        if (!mGatt.readDescriptor(descriptor)) {
            Log.e(TAG, "queryDescriptorData, failed to readDescriptor: "+descriptor.getUuid());
            notifyGotDescriptorValue(null);
            return;
        }
        mQueryDescriptorData = descriptorData;
    }

    private void processOnDescriptorReadForReadingDescriptorValue(final BluetoothGattDescriptor descriptor, final int status) {
        if (mQueryDescriptorData == null) { // Not reading value, ignore it
            Log.d(TAG, "processOnDescriptorReadForReadingDescriptorValue, not reading descriptor value, ignore it");
            return;
        }
        if (!mQueryDescriptorData.equals(descriptor.getUuid())) { // UUID doesn't match
            Log.d(TAG, "processOnDescriptorReadForReadingDescriptorValue, ignore unrelated descriptor: "+descriptor.getUuid());
            return;
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "processOnDescriptorReadForReadingDescriptorValue, failed to read descriptor: "+descriptor.getUuid());
            notifyGotDescriptorValue(null);
        } else {
            readDescriptorValue(mQueryDescriptorData);
            notifyGotDescriptorValue(mQueryDescriptorData);
        }
        // reset variable when got its related callback
        mQueryDescriptorData = null;
    }

    // Write descriptor methods
    private WriteValueListener mWriteDescriptorValueListener = null;
    private void notifySetDescriptorValue(final BLEData data) {
        if (mWriteDescriptorValueListener != null) {
            mWriteDescriptorValueListener.onSetValue(data);
        }
    }

    private BLEData mWriteDescriptorData = null;

    /**
     * <p>Method to write the value of GATT Descriptor
     * @param descriptorData The GATT Descriptor BLEData object to write,
     * the value is just the value returned by {@link BluetoothGattDescriptor#getValue()} in the object.
     * @param listener Listener to receive the result of the write action
     */
    public void writeDescriptorData(final BLEData descriptorData, final WriteValueListener listener) {
        mWriteDescriptorValueListener = listener;
        if (mGatt == null) {
            Log.e(TAG, "The deivce hasn't been connected");
            notifySetDescriptorValue(null);
            return;
        }
        final BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) descriptorData.data;
        if (!mGatt.writeDescriptor(descriptor)) {
            notifySetDescriptorValue(null);
            return;
        }
        mWriteDescriptorData = descriptorData;
    }

    private void processOnDescriptorWriteForWritingDescriptorValue(final BluetoothGattDescriptor descriptor, final int status) {
        if (mWriteDescriptorData == null) { // Not reading value, ignore it
            Log.d(TAG, "processOnDescriptorWriteForWritingDescriptorValue, not writing descriptor value, ignore it");
            return;
        }
        if (!mWriteDescriptorData.equals(descriptor.getUuid())) { // UUID doesn't match
            Log.d(TAG, "processOnDescriptorWriteForWritingDescriptorValue, ignore unrelated descriptor: "+descriptor.getUuid());
            return;
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "processOnDescriptorWriteForWritingDescriptorValue, failed to write descriptor: "+descriptor.getUuid());
            notifySetDescriptorValue(null);
        } else {
            notifySetDescriptorValue(mWriteDescriptorData);
        }
        mWriteDescriptorData = null;
    }

    // Event methods
    public interface ValueChangedListener {
        /**
         * <p>Callback function of value change events of GATT Characteristics
         * @param data The changed GATT Characteristic BLEData object
         */
        void onValueChanged(final BLEData data);
    }
    // Character changed method
    private List<ValueChangedListener> mCharacterChangedListeners = new ArrayList<ValueChangedListener>();
    private void notifyCharacterChanged(final BluetoothGattCharacteristic characteristic) {
        final BLEData data = new BLEData(characteristic.getUuid(), characteristic);
        final List<ValueChangedListener> listeners = new ArrayList<ValueChangedListener>(mCharacterChangedListeners);
        for (final ValueChangedListener listener : listeners) {
            listener.onValueChanged(data);
        }
    }

    /**
     * <p>Method to add listeners to monitor the value change events of the GATT Characteristics of this device
     * See {@link ValueChangedListener#onValueChanged} for details about what it will receive
     * @param listener The listener to add
     */
    public void addCharacterChangedListener(final ValueChangedListener listener) {
        if (listener != null) {
            mCharacterChangedListeners.add(listener);
        }
    }

    /**
     * <p>Method to remove listeners to monitor the value change events of the GATT Characteristics
     * See {@link #addCharacterChangedListener} for details
     * @param listener The listener to remove
     */
    public void removeCharacterChangedListener(final ValueChangedListener listener) {
        if (listener != null) {
            mCharacterChangedListeners.remove(listener);
        }
    }
}
