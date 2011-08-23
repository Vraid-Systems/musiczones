/*
 * singleton class wrapper for controller a VLC media player instance
 * through the vlcj bindings
 * 
 * should work out of the box for windows and linux for more information on
 * Mac support see: http://code.google.com/p/vlcj/wiki/MacSupport
 * 
 * for some boundary cases on windows see:
 * http://code.google.com/p/vlcj/wiki/QuickStart#Notes_for_Windows_Users
 */
package contrib;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import uk.co.caprica.vlcj.player.MediaMeta;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import zoneserver.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class VLCMediaPlayer {

    private static VLCMediaPlayer vmp_SingleInstance = null;
    private MediaPlayerFactory vmp_MediaPlayerFactory = null;
    private MediaPlayer vmp_MediaPlayer = null;
    private List<String> vmp_MediaUrlStringArray = null;
    private int vmp_PlayBackIndexInt = 0;

    protected VLCMediaPlayer() {
        if (ZoneServerUtility.getInstance().isMac()) {
            vmp_MediaPlayerFactory = new MediaPlayerFactory("--vout=macosx");
        } else {
            vmp_MediaPlayerFactory = new MediaPlayerFactory();
        }
        vmp_MediaPlayer = vmp_MediaPlayerFactory.newHeadlessMediaPlayer();
        vmp_MediaUrlStringArray = new LinkedList();

        vmp_MediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

            @Override
            public void finished(MediaPlayer theMediaPlayer) {
                VLCMediaPlayer.getInstance().next();
            }
        });
    }

    public static VLCMediaPlayer getInstance() {
        if (vmp_SingleInstance == null) {
            vmp_SingleInstance = new VLCMediaPlayer();
        }
        return vmp_SingleInstance;
    }

    public void addMediaUrl(String theMediaUrlStr) {
        vmp_MediaUrlStringArray.add(theMediaUrlStr);
    }

    public void playIndex(int theIndex) {
        vmp_PlayBackIndexInt = theIndex;
        vmp_MediaPlayer.prepareMedia(vmp_MediaUrlStringArray.get(vmp_PlayBackIndexInt));
        play();
    }

    public void playIndex() {
        playIndex(vmp_PlayBackIndexInt);
    }

    public void removeIndex(int theIndex) {
        if ((vmp_PlayBackIndexInt > 0) && (theIndex <= vmp_PlayBackIndexInt)) {
            vmp_PlayBackIndexInt--;
        }
        vmp_MediaUrlStringArray.remove(theIndex);
    }

    public void removeMediaUrl(String theMediaUrlStr) {
        int aMediaUrlStrIndex = vmp_MediaUrlStringArray.indexOf(theMediaUrlStr);
        removeIndex(aMediaUrlStrIndex);
    }

    public String getMetaField(String theFieldName) {
        MediaMeta aMM = vmp_MediaPlayer.getMediaMeta();
        if (aMM == null) {
            return null;
        }

        if (theFieldName.equals("album")) {
            return aMM.getAlbum();
        } else if (theFieldName.equals("artist")) {
            return aMM.getArtist();
        } else if (theFieldName.equals("artwork")) {
            return aMM.getArtworkUrl();
        } else if (theFieldName.equals("title")) {
            return aMM.getTitle();
        }

        return null;
    }

    public void shufflePlayList() {
        String aCurrentMediaUrlStr = vmp_MediaUrlStringArray.get(vmp_PlayBackIndexInt);
        Collections.shuffle(vmp_MediaUrlStringArray);
        int i = 0;
        for (String aMediaUrlStr : vmp_MediaUrlStringArray) {
            if (aMediaUrlStr.equals(aCurrentMediaUrlStr)) {
                vmp_PlayBackIndexInt = i;
                break;
            }
            i++;
        }
    }

    public int getCurrentIndex() {
        return vmp_PlayBackIndexInt;
    }

    public List<String> getPlayList() {
        return vmp_MediaUrlStringArray;
    }

    public boolean isPlaying() {
        return vmp_MediaPlayer.isPlaying();
    }

    public void play() {
        vmp_MediaPlayer.play();
    }

    public void pause() {
        vmp_MediaPlayer.pause();
    }

    public void stop() {
        vmp_MediaPlayer.stop();
    }

    public void next() {
        if ((vmp_PlayBackIndexInt + 1) < vmp_MediaUrlStringArray.size()) {
            vmp_PlayBackIndexInt++;
            playIndex();
        } else { //no more items in playlist, stop
            stop();
        }
    }

    public void previous() {
        if ((vmp_PlayBackIndexInt - 1) >= 0) {
            vmp_PlayBackIndexInt--;
            playIndex();
        } else { //at beginning of playlist, stop
            stop();
        }
    }
}
