package com.example.glimwormbeacons.configuring;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ConfigureBeacon extends Activity implements OnCancelListener,BeaconConnectionListener {
	private final static String TAG = ConfigureBeacon.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	
	private String mDeviceName;
	private String mDeviceAddress;
	private TextView mmajor_field;
	private TextView mminor_field;
	private SeekBar moutput_power_slider;
	private TextView moutput_power;
	private TextView mpin_field_input;
	private Button mread_values;

	private SeekBar mtransmit_interval_slider;
	private TextView mtransmit_interval;
	private TextView mname_field_input;
	private LinearLayout mlayout_container;
	private TextView mversion;
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		final Intent intent = getIntent();
		String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		String mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beaconConnected() {
		// TODO Auto-generated method stub
		
	}
	
	

	@Override
	public void beaconDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataReceived() {
		// TODO Auto-generated method stub
		
	}
}
