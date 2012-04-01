/*
 * singleton class wrapper for controlling a mediaplayer implementation
 */
package audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFileInputStream;

import musiczones.MusicZones;
import netutil.CIFSNetworkInterface;
import zonecontrol.FileSystemType;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class MediaPlayerImpl implements MediaPlayerIFace, OnCompletionListener {

	public static final String[] theSupportedContainers = { "mp3", "mp4",
			"ogg", "wav", "aac" };
	public static final String[] thePlayListContainers = { "pls", "m3u" };

	private static MediaPlayerImpl vmp_SingleInstance = null;
	private static MediaPlayer vmp_MediaPlayer = null;
	private static StreamProxy vmp_StreamProxy = null;

	private List<String> vmp_MediaUrlStrList = null;
	private List<String> vmp_LocalFileNameList = null;
	private int vmp_PlayBackIndexInt = -1;
	private boolean debugMessagesOn = false;
	protected int lengthOfPauseBetweenMediaInSeconds = 2;

	protected MediaPlayerImpl(boolean theDebugIsOn) {
		debugMessagesOn = theDebugIsOn;
		vmp_MediaUrlStrList = new ArrayList<String>();
		vmp_LocalFileNameList = new ArrayList<String>();
		vmp_MediaPlayer = new MediaPlayer();
		vmp_MediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		vmp_MediaPlayer.setLooping(false);
		vmp_MediaPlayer.setOnCompletionListener(this);
	}

	@Override
	public void finalize() throws Throwable {
		close();
		super.finalize();
	}

	public void close() {
		stop();

		if (vmp_MediaPlayer != null) {
			vmp_MediaPlayer.release();
		}

		if (vmp_StreamProxy != null) {
			vmp_StreamProxy.stop();
		}

		for (String aFileName : vmp_LocalFileNameList) {
			MusicZones.getApplicationContext().deleteFile(aFileName);
		}
	}

	public static MediaPlayerImpl getInstance(boolean theDebugIsOn) {
		if (vmp_SingleInstance == null) {
			vmp_SingleInstance = new MediaPlayerImpl(theDebugIsOn);
		}
		return vmp_SingleInstance;
	}

	public static MediaPlayerImpl getInstance() {
		return getInstance(false);
	}

	@Override
	public void addMediaUrl(String theMediaUrlStr) {
		vmp_MediaUrlStrList.add(theMediaUrlStr);
		if (debugMessagesOn) {
			System.out.println("added media url: " + theMediaUrlStr);
		}
	}

	protected String formatMediaUrl(String theMediaUrlStr) {
		// remove media file name if prepended
		theMediaUrlStr = ZoneServerUtility.getInstance().getPlainUrlFromUrlStr(
				theMediaUrlStr);

		// format windowsy backslashes if this is on a windows host and
		// samba-ing
		if (ZoneServerUtility.getInstance().isWindows()
				&& theMediaUrlStr.contains(FileSystemType.smb.toString()
						.concat(ZoneServerUtility.prefixUriStr))) {
			theMediaUrlStr = theMediaUrlStr.replace(FileSystemType.smb
					.toString().concat(ZoneServerUtility.prefixUriStr), "\\\\");
			theMediaUrlStr = theMediaUrlStr.replace("/", "\\");
		}

		// remove radio prefix if it exists in media string
		if (theMediaUrlStr.contains(FileSystemType.radio.toString().concat(
				ZoneServerUtility.prefixUriStr))) {
			theMediaUrlStr = theMediaUrlStr.replace(FileSystemType.radio
					.toString().concat(ZoneServerUtility.prefixUriStr), "");
		}

		// replace spaces with escape chars if not windows
		if ((!ZoneServerUtility.getInstance().isWindows())) {
			// %20 if this is an external URL
			if (theMediaUrlStr.contains(FileSystemType.smb.toString().concat(
					ZoneServerUtility.prefixUriStr))) {
				theMediaUrlStr = theMediaUrlStr.replace(" ", "%20");
			}
		}

		return theMediaUrlStr;
	}

	protected String getPlayIndexFileName(int theIndex) {
		String[] aFilePathArray = vmp_MediaUrlStrList.get(theIndex).split("/");
		return aFilePathArray[(aFilePathArray.length - 1)];
	}

	protected void handlePlayIndexEx(Exception ex) {
		if (debugMessagesOn) {
			System.err.println(ex);
		}
		stop();
	}

	@Override
	public void playIndex(int theIndex) {
		vmp_PlayBackIndexInt = theIndex;
		String theMediaStr = formatMediaUrl(vmp_MediaUrlStrList
				.get(vmp_PlayBackIndexInt));
		if (debugMessagesOn) {
			System.out.println("will now play: " + theMediaStr);
		}

		vmp_MediaPlayer.reset();
		vmp_MediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		// different ways to set the plackback file depending on source
		if (vmp_MediaUrlStrList.get(vmp_PlayBackIndexInt).contains(
				FileSystemType.radio.toString().concat(
						ZoneServerUtility.prefixUriStr))) { // radio stream

			// http://code.google.com/p/npr-android-app/source/search?q=mediaPlayer.setDataSource&origq=mediaPlayer.setDataSource&btnG=Search+Trunk
			// From 2.2 on (SDK ver 8), the local mediaplayer can handle
			// Shoutcast streams natively. Let's detect that, and not proxy.
			int aSdkVersion = 0;
			try {
				aSdkVersion = Integer.parseInt(Build.VERSION.SDK);
			} catch (NumberFormatException ignored) {
			}
			if (aSdkVersion < 8) {
				if (vmp_StreamProxy == null) {
					vmp_StreamProxy = new StreamProxy();
					vmp_StreamProxy.init();
					vmp_StreamProxy.start();
				}
				theMediaStr = String.format("http://127.0.0.1:%d/%s",
						vmp_StreamProxy.getPort(), theMediaStr);
			}

			try {
				vmp_MediaPlayer.setDataSource(theMediaStr);
			} catch (IllegalArgumentException ex) {
				handlePlayIndexEx(ex);
				return;
			} catch (IllegalStateException ex) {
				handlePlayIndexEx(ex);
				return;
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
				return;
			}
		} else if (vmp_MediaUrlStrList.get(vmp_PlayBackIndexInt).contains(
				FileSystemType.smb.toString().concat(
						ZoneServerUtility.prefixUriStr))) { // SMB file
			// check if file is already stored on the internal storage
			FileInputStream aFileInputStream = null;
			String aPlayBackFileNameStr = getPlayIndexFileName(vmp_PlayBackIndexInt);
			try {
				aFileInputStream = MusicZones.getApplicationContext()
						.openFileInput(aPlayBackFileNameStr);
			} catch (FileNotFoundException ex) {
				aFileInputStream = null;
				if (debugMessagesOn) {
					System.out.println(ex);
				}
			}
			if (aFileInputStream == null) { // need to copy
				// SETUP to copy the SMB file to internal storage
				SmbFileInputStream aSmbFileInputStream = null;
				try {
					aSmbFileInputStream = new SmbFileInputStream(theMediaStr);
				} catch (SmbException ex) {

					// try unformatted before error-ing out
					theMediaStr = vmp_MediaUrlStrList.get(vmp_PlayBackIndexInt);
					try {
						aSmbFileInputStream = new SmbFileInputStream(
								theMediaStr);
					} catch (SmbException ex1) {
						handlePlayIndexEx(ex1);
						return;
					} catch (MalformedURLException ex1) {
						handlePlayIndexEx(ex1);
						return;
					} catch (UnknownHostException ex1) {
						handlePlayIndexEx(ex1);
						return;
					}
				} catch (MalformedURLException ex) {
					handlePlayIndexEx(ex);
					return;
				} catch (UnknownHostException ex) {
					handlePlayIndexEx(ex);
					return;
				}

				// copy SMB file to internal storage
				FileOutputStream aFileOutputStream = null;
				try {
					aFileOutputStream = MusicZones.getApplicationContext()
							.openFileOutput(aPlayBackFileNameStr,
									Context.MODE_PRIVATE);
				} catch (FileNotFoundException ex) {
					handlePlayIndexEx(ex);
					return;
				}
				if (aFileOutputStream == null) {
					return;
				}
				boolean aTransferWorked = CIFSNetworkInterface.getInstance()
						.transferSmbInputStreamToFileOutputStream(
								aSmbFileInputStream, aFileOutputStream);
				if (!aTransferWorked) {
					System.err.println("transfer of " + aPlayBackFileNameStr
							+ " failed");
					return;
				}
				// log that we need to delete
				vmp_LocalFileNameList.add(aPlayBackFileNameStr);
			}

			// play the file from internal storage
			try {
				aFileInputStream = MusicZones.getApplicationContext()
						.openFileInput(aPlayBackFileNameStr);
			} catch (FileNotFoundException ex) {
				handlePlayIndexEx(ex);
				return;
			}
			try {
				vmp_MediaPlayer.setDataSource(aFileInputStream.getFD());
			} catch (IllegalArgumentException ex) {
				handlePlayIndexEx(ex);
				return;
			} catch (IllegalStateException ex) {
				handlePlayIndexEx(ex);
				return;
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
				return;
			}
			try {
				aFileInputStream.close();
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
				return;
			}
		} else { // local file
			FileInputStream aFileInputStream = null;
			try {
				aFileInputStream = new FileInputStream(new File(theMediaStr));
			} catch (FileNotFoundException ex) {
				handlePlayIndexEx(ex);
				return;
			}
			try {
				vmp_MediaPlayer.setDataSource(aFileInputStream.getFD());
			} catch (IllegalArgumentException ex) {
				handlePlayIndexEx(ex);
				return;
			} catch (IllegalStateException ex) {
				handlePlayIndexEx(ex);
				return;
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
				return;
			}
			try {
				aFileInputStream.close();
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
				return;
			}
		}

		// load the media
		try {
			vmp_MediaPlayer.prepare();
		} catch (IllegalStateException ex) {
			handlePlayIndexEx(ex);
		} catch (IOException ex) {
			handlePlayIndexEx(ex);
		}
		// start playback
		vmp_MediaPlayer.start();
	}

	@Override
	public void onCompletion(MediaPlayer theMediaPlayer) {
		next();
	}

	@Override
	public void playIndex() {
		playIndex(vmp_PlayBackIndexInt);
	}

	@Override
	public void removeIndex(int theIndex) {
		stop(-1);
		vmp_MediaUrlStrList.remove(theIndex);
	}

	@Override
	public void shufflePlayList() {
		if ((vmp_MediaUrlStrList != null) && (vmp_MediaUrlStrList.size() > 0)
				&& (vmp_PlayBackIndexInt < vmp_MediaUrlStrList.size())) {
			if (vmp_PlayBackIndexInt >= 0) {
				String aCurrentMediaUrlStr = vmp_MediaUrlStrList
						.get(vmp_PlayBackIndexInt);
				Collections.shuffle(vmp_MediaUrlStrList);
				int i = 0;
				for (String aMediaUrlStr : vmp_MediaUrlStrList) {
					if (aMediaUrlStr.equals(aCurrentMediaUrlStr)) {
						vmp_PlayBackIndexInt = i;
						break;
					}
					i++;
				}
			} else {
				Collections.shuffle(vmp_MediaUrlStrList);
			}
		}
	}

	@Override
	public void togglePlayPause() {
		if (vmp_MediaPlayer.isPlaying()) {
			vmp_MediaPlayer.pause();
		} else {
			vmp_MediaPlayer.start();
		}
	}

	@Override
	public void next() {
		if ((vmp_PlayBackIndexInt + 1) < vmp_MediaUrlStrList.size()) {
			vmp_PlayBackIndexInt++;
			playIndex();
		} else { // no more items in playlist, stop process
			stop();
		}
	}

	@Override
	public void previous() {
		if ((vmp_PlayBackIndexInt - 1) >= 0) {
			vmp_PlayBackIndexInt--;
			playIndex();
		} else { // at beginning of playlist, loop
			stop();
		}
	}

	@Override
	public void stop(int theIndex) {
		vmp_PlayBackIndexInt = theIndex;
		vmp_MediaPlayer.stop();
	}

	@Override
	public void stop() {
		stop(-1);
	}

	@Override
	public boolean isStopped() {
		return (vmp_PlayBackIndexInt < 0);
	}

	@Override
	public void clearPlaylist() {
		stop();
		vmp_MediaUrlStrList.clear();
	}

	@Override
	public int getCurrentIndex() {
		return vmp_PlayBackIndexInt;
	}

	@Override
	public List<String> getPlayList() {
		return vmp_MediaUrlStrList;
	}

}
