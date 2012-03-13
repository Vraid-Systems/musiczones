/*
 * singleton class wrapper for controlling a mediaplayer implementation
 */
package audio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import zonecontrol.FileSystemType;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class MediaPlayerImpl implements MediaPlayerIFace {

	public static final String[] theSupportedContainers = { "mp3", "mp4",
			"ogg", "wav", "aac" };
	public static final String[] thePlayListContainers = { "pls", "m3u" };

	private static MediaPlayerImpl vmp_SingleInstance = null;
	private static MediaPlayer vmp_MediaPlayer = null;

	private List<String> vmp_MediaUrlStrList = null;
	private int vmp_PlayBackIndexInt = -1;
	private boolean debugMessagesOn = false;
	protected int lengthOfPauseBetweenMediaInSeconds = 2;

	protected MediaPlayerImpl(boolean theDebugIsOn) {
		debugMessagesOn = theDebugIsOn;
		vmp_MediaPlayer = new MediaPlayer();
		vmp_MediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}

	@Override
	public void finalize() throws Throwable {
		if (vmp_MediaPlayer != null) {
			vmp_MediaPlayer.stop();
			vmp_MediaPlayer.release();
		}
		super.finalize();
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

	protected void handlePlayIndexEx(Exception ex) {
		if (debugMessagesOn) {
			System.err.println(ex);
		}
		stop();
		return;
	}

	@Override
	public void playIndex(int theIndex) {
		vmp_PlayBackIndexInt = theIndex;
		String theMediaStr = formatMediaUrl(vmp_MediaUrlStrList
				.get(vmp_PlayBackIndexInt));
		if (debugMessagesOn) {
			System.out.println("will now play: " + theMediaStr);
		}

		// different ways to set the plackback file depending on source
		if (vmp_MediaUrlStrList.get(vmp_PlayBackIndexInt).contains(
				FileSystemType.radio.toString().concat(
						ZoneServerUtility.prefixUriStr))) {
			try {
				vmp_MediaPlayer.setDataSource(theMediaStr);
			} catch (IllegalArgumentException ex) {
				handlePlayIndexEx(ex);
			} catch (IllegalStateException ex) {
				handlePlayIndexEx(ex);
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
			}
		} else if (vmp_MediaUrlStrList.get(vmp_PlayBackIndexInt).contains(
				FileSystemType.smb.toString().concat(
						ZoneServerUtility.prefixUriStr))) {
			// TODO: copy file from samba share to local storage and then play
			// file
			try {
				vmp_MediaPlayer.setDataSource("");
			} catch (IllegalArgumentException ex) {
				handlePlayIndexEx(ex);
			} catch (IllegalStateException ex) {
				handlePlayIndexEx(ex);
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
			}
		} else {
			FileInputStream aFileInputStream = null;
			try {
				aFileInputStream = new FileInputStream(new File(theMediaStr));
			} catch (FileNotFoundException ex) {
				handlePlayIndexEx(ex);
			}
			try {
				vmp_MediaPlayer.setDataSource(aFileInputStream.getFD());
			} catch (IllegalArgumentException ex) {
				handlePlayIndexEx(ex);
			} catch (IllegalStateException ex) {
				handlePlayIndexEx(ex);
			} catch (IOException ex) {
				handlePlayIndexEx(ex);
			}
		}

		// load the media
		try {
			vmp_MediaPlayer.prepare();
		} catch (IllegalStateException ex) {
			if (debugMessagesOn) {
				System.err.println(ex);
			}
			stop();
		} catch (IOException ex) {
			if (debugMessagesOn) {
				System.err.println(ex);
			}
			stop();
		}
		// start playback
		vmp_MediaPlayer.start();
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
