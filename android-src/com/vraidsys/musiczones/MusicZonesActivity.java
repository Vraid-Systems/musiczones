/*
 * main Activity for the MusicZones application
 */
package com.vraidsys.musiczones;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author Jason Zerbe
 */
public class MusicZonesActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}
}
