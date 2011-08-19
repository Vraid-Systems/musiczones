/*
 * singleton class wrapper for controller a VLC media player instance
 * through the vlcj bindings
 */
package contrib;

import java.util.LinkedList;
import java.util.List;
import uk.co.caprica.vlcj.player.MediaMeta;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

/**
 * @author Jason Zerbe
 */
public class VLCMediaPlayer {

    private static VLCMediaPlayer vmp_SingleInstance = null;
    private MediaPlayer vmp_MediaPlayer = null;
    private List<String> vmp_MediaUrlStringArray = null;

    public VLCMediaPlayer() {
        vmp_MediaPlayer = new MediaPlayerFactory().newHeadlessMediaPlayer();
        vmp_MediaUrlStringArray = new LinkedList();
    }

    public static VLCMediaPlayer getInstance() {
        if (vmp_SingleInstance == null) {
            vmp_SingleInstance = new VLCMediaPlayer();
        }
        return vmp_SingleInstance;
    }

    public void addMediaUrl(String theMediaUrl) {
        vmp_MediaUrlStringArray.add(theMediaUrl);
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
        vmp_MediaPlayer.nextChapter();
    }

    public void previous() {
        vmp_MediaPlayer.previousChapter();
    }
}
