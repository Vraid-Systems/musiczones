/*
 * main Activity for the MusicZones application
 */
package com.vraidsys.musiczones;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
	TextView myTextViewStatus;
	boolean myZoneIsRunning = false;
	ZoneMulticastServer myZoneMulticastServer = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		MusicZones.setIndexLocalHost(false);
		MusicZones.setIsDebugOn(false);
		MusicZones.setIsLowMem(false);
		MusicZones.setIsOnline(true);

		// start UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		MusicZones.setAssets(this.getAssets());
		MusicZones.setApplicationContext(this.getApplicationContext());
		myTextViewStatus = (TextView) findViewById(R.id.TextViewStatus);
	}

	/** check if zone is connected to network **/
	protected boolean isZoneOnline() {
		boolean aReturnBooleanOnlineFlag = false;

		ConnectivityManager aConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		aReturnBooleanOnlineFlag = aConnectivityManager.getActiveNetworkInfo()
				.isConnected();

		return aReturnBooleanOnlineFlag;
	}

	/** start/stop the zone control and indexing components **/
	public void toggleRunState(View theClickedView) {
		Button theClickedButton = (Button) theClickedView;

		// does the zone have a route to the WAN
		MusicZones.setIsOnline(isZoneOnline());

		// check if we can write data to external storage
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			String aRootPathStr = Environment.getExternalStorageDirectory()
					.getAbsolutePath();
			MusicZones.setAppExternalStorageRoot(aRootPathStr + "/musiczones");
		} else {
			MusicZones.setAppExternalStorageRoot(null);
		}

		if (!myZoneIsRunning) {
			myTextViewStatus.append("Starting Zone ... ");

			// first initialize jetty in-case using custom webserver port
			JettyWebServer theWebServer = JettyWebServer.getInstance(MusicZones
					.getWebInterfacePort());
			theWebServer.startServer();

			// create system-wide MediaPlayer instance
			MediaPlayerImpl.getInstance(MusicZones.getIsDebugOn());

			// then bring up the zone controller logic and set the zone name
			ZoneServerLogic aZoneServerLogic = ZoneServerLogic.getInstance();
			EditText aEditTextDeviceName = (EditText) findViewById(R.id.EditTextDeviceName);
			Editable aEditable = aEditTextDeviceName.getText();
			String aZoneName = aEditable.toString();
			if ((aZoneName != null) && (!"".equals(aZoneName))) {
				aZoneServerLogic.setZoneName(aZoneName);
			}

			// pause just enough (1/10 second) to prevent receiving own init
			// messages from group
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				System.err.println(ex);
			}

			// after networking is in place, finally ready to start accepting
			// control packets
			if (MusicZones.getIsOnline()) {
				myZoneMulticastServer = new ZoneMulticastServer(
						MusicZones.getIsDebugOn());
				myZoneMulticastServer.startServer();
			}

			// start up the library indexing service
			ZoneLibraryIndex aZoneLibraryIndex = ZoneLibraryIndex
					.getInstance(MusicZones.getIsDebugOn());
			aZoneLibraryIndex.addIndexBuild();

			// start the master server notification point
			if (MusicZones.getIsOnline()) {
				HttpCmdClient.getInstance(MusicZones.getIsDebugOn());
			}

			// swap the UI button
			theClickedButton.setText(R.string.zoneExitStr);

			// zone is now started
			myZoneIsRunning = true;
			myTextViewStatus.append("Zone Started\n");
		} else if (myZoneIsRunning) {
			myTextViewStatus.append("Zone Stopping ...\n");

			// clean up all locally stored audio files
			MediaPlayerImpl.getInstance().close();

			// immediately end the process
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}
}
