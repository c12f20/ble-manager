package com.hill.libblemanager;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

public final class BLEData {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DATATYPE_UNKNOWN, DATATYPE_SERVICE, DATATYPE_CHARACTER, DATATYPE_DESCRIPTOR})
    public @interface DataType {}
    public static final int DATATYPE_UNKNOWN = 0;
    public static final int DATATYPE_SERVICE = 1;
    public static final int DATATYPE_CHARACTER = 2;
    public static final int DATATYPE_DESCRIPTOR = 3;

    public final UUID uuid;
    public final Object data;
    public final @DataType int dataType;

    /**
     * Construction method of BLEData
     * @param uuid UUID value of GATT Service/Characteristic/Descriptor
     * @param data object of GATT Service/Characteristic/Descriptor
     */
    public BLEData(final UUID uuid, final Object data) {
        this.uuid = uuid;
        this.data = data;
        this.dataType = checkDataType();
    }

    private @DataType int checkDataType() {
        if (data instanceof BluetoothGattService) {
            return DATATYPE_SERVICE;
        } else if (data instanceof BluetoothGattCharacteristic) {
            return DATATYPE_CHARACTER;
        } else if (data instanceof BluetoothGattDescriptor) {
            return DATATYPE_DESCRIPTOR;
        } else {
            return DATATYPE_UNKNOWN;
        }
    }

    /**
     * Get the stored value for this characteristic.
     *
     * <p>This function returns the stored value for this BLEData as
     * retrieved by calling {@link BLEDevice#queryCharacterData(BLEData, BLEDevice.QueryValueListener)}
     * or {@link BLEDevice#queryDescriptorData(BLEData, BLEDevice.QueryValueListener)} . The cached
     * value of the BLEData is updated as a result of a read BLEData
     * operation or if a BLEData update notification has been received
     * through {@link BLEDevice.ValueChangedListener} listener.
     *
     * @return Cached value of the BLEData
     */
    public byte[] getValue() {
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.getValue();
            case DATATYPE_DESCRIPTOR:
                final BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) data;
                descriptor.getValue();
            case DATATYPE_SERVICE:
            default:
                return null;
        }
    }

    /**
     * Return the stored value of this BLEData.
     * <p>See {@link #getValue} for details.
     *
     * @param offset Offset at which the string value can be found.
     * @return Cached value of the BLEData
     */
    public String getStringValue(final int offset) {
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.getStringValue(offset);
            case DATATYPE_DESCRIPTOR:
            case DATATYPE_SERVICE:
            default:
                return null;
        }
    }

    /**
     * Return the stored value of this BLEData.
     *
     * <p>The formatType parameter determines how the characteristic value
     * is to be interpreted. For example, setting formatType to
     * {@link BluetoothGattCharacteristic#FORMAT_UINT16} specifies that the first two bytes of the
     * characteristic value at the given offset are interpreted to generate the
     * return value.
     *
     * @param formatType The format type used to interpret the BLEData value.
     * @param offset Offset at which the integer value can be found.
     * @return Cached value of the BLEData or {@link Integer#MAX_VALUE} as invalid value.
     */
    public int getIntValue(final int formatType, final int offset) {
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.getIntValue(formatType, offset);
            case DATATYPE_DESCRIPTOR:
            case DATATYPE_SERVICE:
            default:
                return Integer.MAX_VALUE;
        }
    }
    /**
     * Return the stored value of this BLEData.
     * <p>See {@link #getValue} for details.
     *
     * @param formatType The format type used to interpret the BLEData value.
     * @param offset Offset at which the float value can be found.
     * @return Cached value of the BLEData at a given offset
     * or {@link Float#MAX_VALUE} as invalid value
     */
    public float getFloatValue(final int formatType, final int offset) {
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.getFloatValue(formatType, offset);
            case DATATYPE_DESCRIPTOR:
            case DATATYPE_SERVICE:
            default:
                return Float.MAX_VALUE;
        }
    }
    /**
     * Updates the locally stored value of this BLEData.
     *
     * <p>This function modifies the locally stored cached value of this
     * BLEData. To send the value to the remote device, call
     * {@link BLEDevice#writeCharacterData} or {@link BLEDevice#writeDescriptorData}
     * to send the value to the remote device.
     *
     * @param value New value for this BLEData
     * @return true if the locally stored value has been set, false if the requested value could not
     * be stored locally.
     */
    public boolean setValue(final byte[] value) {
        if (value == null) {
            return false;
        }
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.setValue(value);
            case DATATYPE_DESCRIPTOR:
                final BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) data;
                return descriptor.setValue(value);
            case DATATYPE_SERVICE:
            default:
                return false;
        }
    }
    /**
     * Set the locally stored value of this BLEData.
     * <p>See {@link #setValue(byte[])} for details.
     *
     * @param value New value for this BLEData
     * @return true if the locally stored value has been set
     */
    public boolean setValue(final String value) {
        if (value == null) {
            return false;
        }
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.setValue(value);
            case DATATYPE_DESCRIPTOR:
            case DATATYPE_SERVICE:
            default:
                return false;
        }
    }
    /**
     * Set the locally stored value of this BLEData.
     * <p>See {@link #setValue(byte[])} for details.
     *
     * @param value New value for this BLEData
     * @param formatType Integer format type used to transform the value parameter
     * @param offset Offset at which the value should be placed
     * @return true if the locally stored value has been set
     */
    public boolean setValue(final int value, final int formatType, final int offset) {
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.setValue(value, formatType, offset);
            case DATATYPE_DESCRIPTOR:
            case DATATYPE_SERVICE:
            default:
                return false;
        }
    }
    /**
     * Set the locally stored value of this BLEData.
     * <p>See {@link #setValue(byte[])} for details.
     *
     * @param mantissa Mantissa for this BLEData
     * @param exponent exponent value for this BLEData
     * @param formatType Float format type used to transform the value parameter
     * @param offset Offset at which the value should be placed
     * @return true if the locally stored value has been set
     */
    public boolean setValue(final int mantissa, final int exponent, final int formatType, final int offset) {
        switch (dataType) {
            case DATATYPE_CHARACTER:
                final BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) data;
                return character.setValue(mantissa, exponent, formatType, offset);
            case DATATYPE_DESCRIPTOR:
            case DATATYPE_SERVICE:
            default:
                return false;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof UUID) {
            return equalsToUUID((UUID) obj);
        } else if (obj instanceof BLEData) {
            return equalsToUUID(((BLEData) obj).uuid);
        } else {
            return false;
        }
    }

    private boolean equalsToUUID(final UUID uuid) {
        return this.uuid.equals(uuid);
    }
}
