/*
 * main Activity for the MusicZones application
 */
package com.vraidsys.musiczones;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import audio.MediaPlayerImpl;
import contrib.JettyWebServer;
import musiczones.MusicZones;
import netutil.HttpCmdClient;
import zonecontrol.ZoneLibraryIndex;
import zonecontrol.ZoneMulticastServer;
import zonecontrol.ZoneServerLogic;

/**
 * @author Jason Zerbe
 */
public class MusicZonesActivity extends Activity {
	ZoneMulticastServer myZoneMulticastServer = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		MusicZones.setIndexLocalHost(false);
		MusicZones.setIsDebugOn(false);
		MusicZones.setIsLowMem(false);
		MusicZones.setIsOnline(isZoneOnline());

		// start UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	/** check if zone has a route to the master server **/
	protected boolean isZoneOnline() {
		boolean aReturnBooleanOnlineFlag = false;
		InetAddress aInetAddress = null;
		try {
			aInetAddress = InetAddress
					.getByName(HttpCmdClient.MASTER_SERVER_URL_STR);
		} catch (UnknownHostException ex) {
			Logger.getLogger(MusicZonesActivity.class.getName()).log(
					Level.WARNING, null, ex);
		}
		if (aInetAddress != null) {
			byte[] aInetAddressBytes = aInetAddress.getAddress();
			int aIpv4AddrInt = ((aInetAddressBytes[3] & 0xff) << 24)
					| ((aInetAddressBytes[2] & 0xff) << 16)
					| ((aInetAddressBytes[1] & 0xff) << 8)
					| (aInetAddressBytes[0] & 0xff);

			ConnectivityManager aConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			aReturnBooleanOnlineFlag = aConnectivityManager.requestRouteToHost(
					ConnectivityManager.TYPE_WIFI, aIpv4AddrInt);
		}
		return aReturnBooleanOnlineFlag;
	}

	/** start/stop the zone control and indexing components **/
	public void toggleRunState(View theClickedView) {
		Button theClickedButton = (Button) theClickedView;

		MusicZones.setIsOnline(isZoneOnline()); // recheck at toggle event

		if (theClickedButton.getText().equals(R.string.zoneStartStr)) {
			// first initialize jetty in-case using custom webserver port
			JettyWebServer theWebServer = JettyWebServer.getInstance(MusicZones
					.getWebInterfacePort());
			theWebServer.startServer();

			// create system-wide MediaPlayer instance
			MediaPlayerImpl.getInstance(MusicZones.getIsDebugOn());

			// then bring up the zone controller logic and set the zone name
			ZoneServerLogic mainServerLogic = ZoneServerLogic.getInstance();
			EditText aEditTextDeviceName = (EditText) findViewById(R.id.EditTextDeviceName);
			Editable aEditable = aEditTextDeviceName.getText();
			String aZoneName = aEditable.toString();
			if ((aZoneName != null) && (!"".equals(aZoneName))) {
				mainServerLogic.setZoneName(aZoneName);
			}

			// pause just enough (1/10 second) to prevent receiving own init
			// messages from group
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				Logger.getLogger(MusicZonesActivity.class.getName()).log(
						Level.WARNING, null, ex);
			}

			// after networking is in place, finally ready to start accepting
			// control packets
			if (MusicZones.getIsOnline()) {
				myZoneMulticastServer = new ZoneMulticastServer(
						MusicZones.getIsDebugOn());
				myZoneMulticastServer.startServer();
			}

			// start up the library indexing service
			ZoneLibraryIndex.getInstance(MusicZones.getIsDebugOn())
					.addIndexBuild();

			// start the master server notification point
			if (MusicZones.getIsOnline()) {
				HttpCmdClient.getInstance(MusicZones.getIsDebugOn());
			}

			// swap the UI button
			theClickedButton.setText(R.string.zoneStopStr);
			// zone is now started
		} else if (theClickedButton.getText().equals(R.string.zoneStopStr)) {
			// make sure zone is no longer up according to master server
			HttpCmdClient.getInstance().removePingTask();

			// stop zone from crawling network
			ZoneLibraryIndex.getInstance().removeIndexBuild();

			// leave multicast group
			myZoneMulticastServer.stopServer();
			ZoneServerLogic.getInstance().removePingSchedule();

			// stop the web server process (un-bind)
			JettyWebServer.getInstance().stopServer();

			// swap the UI button
			theClickedButton.setText(R.string.zoneStartStr);
			// zone has stopped
		}
	}
}
