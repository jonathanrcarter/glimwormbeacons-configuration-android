package com.example.glimwormbeacons.configuring;

public interface BeaconConnectionListener {
	public void beaconConnected();
	public void beaconDisconnected();
	public void dataReceived();
}
