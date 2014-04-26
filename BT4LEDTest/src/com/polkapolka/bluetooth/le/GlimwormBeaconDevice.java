package com.polkapolka.bluetooth.le;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GlimwormBeaconDevice {

	private boolean isConnected = false;
	String name, pinCode;
	int major, minor, rssi, outputPower, battery, interval;
	boolean transmitting = false;

	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic characteristicTX;
	private BluetoothGattCharacteristic characteristicRX;
	public final static UUID HM_RX_TX = UUID
			.fromString(SampleGattAttributes.HM_RX_TX);

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	List<String> outputQueue = new ArrayList<String>();

	public void setName(String name) {
		this.name = name;
	}

	public void setPin(String pin) {
		this.pinCode = pin;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public void setMajor(int major) {

		this.major = major;
	}

	public void setBatteryLevel(int battery) {
		this.battery = battery;
	}

	public void setOutputPower(int outputPower) {
		this.outputPower = outputPower;
	}

	public void setAdvertisingInterval(int interval) {
		this.interval = interval;
	}

	public void connect() {

	}

	public void disconnect() {

	}

	public boolean isConnected() {
		return isConnected;
	}

	private void transmitQueue() {
		if (!isConnected)
			return;
		if (outputQueue.size() < 1)
			return;
		characteristicTX.setValue(outputQueue.get(0));
		if (mBluetoothLeService != null) {
			mBluetoothLeService.writeCharacteristic(characteristicTX);
			mBluetoothLeService.setCharacteristicNotification(characteristicRX,
					true);
		}
	}

	public void dataReceived(String data) {

	}

	public void writeSettings() {
		String minor_string = Integer.toHexString(minor);
		while (minor_string.length() < 4) {
			minor_string = "0" + minor_string;
		}
		String major_string = Integer.toHexString(major);
		while (major_string.length() < 4) {
			major_string = "0" + major_string;
		}
		outputQueue.add("AT+MARJ0x" + major_string);
		outputQueue.add("AT+MINO0x" + minor_string);
		outputQueue.add("AT+ADVI" + interval);
		outputQueue.add("AT+POWE" + outputPower);
		outputQueue.add("AT+NAME" + name);
		if (pinCode.length() >= 4 && pinCode.length() <= 6) {
			outputQueue.add("AT+TYPE2");
			outputQueue.add("AT+PASS" + pinCode);
		} else {
			outputQueue.add("AT+TYPE0");
			outputQueue.add("AT+PASS000000");
		}
		outputQueue.add("AT+RESET");
		transmitQueue();
	}

	public void setDefaultSettings() {
		outputQueue.add("AT+MARJ0x0001");
		outputQueue.add("AT+MINO0x0001");
		outputQueue.add("AT+ADVI9");
		outputQueue.add("AT+POWE2");
		outputQueue.add("AT+NAMEGLBeacon");
		outputQueue.add("AT+TYPE2");
		outputQueue.add("AT+PASS123456");
		outputQueue.add("AT+RESET");
		transmitQueue();
	}

	public void readSettings()
	{
		outputQueue.add("AT+MARJ?");
		outputQueue.add("AT+MINO?");
		outputQueue.add("AT+ADVI?");
		outputQueue.add("AT+POWE?");
		outputQueue.add("AT+NAME?");
		outputQueue.add("AT+TYPE?");
		outputQueue.add("AT+PASS?");
		transmitQueue();
	}
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				isConnected = false;
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				 searchSerialGATTService(mBluetoothLeService.getSupportedGattServices());
				isConnected = true;
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				dataReceived(intent
						.getStringExtra(mBluetoothLeService.EXTRA_DATA));
			}
		}
	};

	private void searchSerialGATTService(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = "Unknown Service";
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(LIST_NAME,
					SampleGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);
			characteristicTX = gattService
					.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
			characteristicRX = gattService
					.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
		}

	}

}

/*
final String actionPinRequested =
"android.bluetooth.device.action.PAIRING_REQUEST";
// android.bluetooth.device.action.
IntentFilter intentFilterPinRequested = new
IntentFilter(actionPinRequested);
registerReceiver(mReceiverRequiresPin, intentFilterPinRequested);

final String actionBonded = "android.bluetooth.device.action.BOND_BONDED";
IntentFilter intentFilterBonded = new IntentFilter(actionBonded);
registerReceiver(mReceiverBonded, intentFilterBonded);
*/
