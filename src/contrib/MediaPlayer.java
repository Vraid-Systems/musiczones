/*
 * singleton class wrapper for controlling mplayer. for slave commands see:
 * http://www.mplayerhq.hu/DOCS/tech/slave.txt
 */
package contrib;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import multicastmusiccontroller.MulticastMusicController;
import multicastmusiccontroller.ProgramConstants;
import zoneserver.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class MediaPlayer implements ProgramConstants {

    private static MediaPlayer vmp_SingleInstance = null;
    private List<String> vmp_MediaUrlStringArray = null;
    private int vmp_PlayBackIndexInt = -1;
    private JMPlayer vmp_JMPlayer = null;

    protected MediaPlayer() {
        vmp_MediaUrlStringArray = new LinkedList();
        String aMPlayerBinPath = ZoneServerUtility.getInstance().loadStringPref(prefMediaPlayerPathKeyStr, "");
        if (aMPlayerBinPath.isEmpty()) {
            MulticastMusicController.MPlayerNotFound();
        }
        vmp_JMPlayer = new JMPlayer();
        vmp_JMPlayer.setMPlayerPath(aMPlayerBinPath);
    }

    @Override
    public void finalize() throws Throwable {
        vmp_JMPlayer.close();
        super.finalize();
    }

    public static MediaPlayer getInstance() {
        if (vmp_SingleInstance == null) {
            vmp_SingleInstance = new MediaPlayer();
        }
        return vmp_SingleInstance;
    }

    public void addMediaUrl(String theMediaUrlStr) {
        vmp_MediaUrlStringArray.add(theMediaUrlStr);
        System.out.println("added media url: " + theMediaUrlStr);
    }

    protected String formatMediaUrl(String theMediaUrlStr) {
        //format windowsy backslashes if this is on a windows host and samba-ing
        if (ZoneServerUtility.getInstance().isWindows()
                && theMediaUrlStr.contains(FileSystemType.smb.toString().concat(prefixUriStr))) {
            theMediaUrlStr = theMediaUrlStr.replace(FileSystemType.smb.toString().concat(prefixUriStr), "\\\\");
            theMediaUrlStr = theMediaUrlStr.replace("/", "\\");
        }

        //replace spaces with %20 if not windows
        if (!ZoneServerUtility.getInstance().isWindows()) {
            theMediaUrlStr = theMediaUrlStr.replace(" ", "%20");
        }

        return theMediaUrlStr;
    }

    public void playIndex(int theIndex) {
        vmp_PlayBackIndexInt = theIndex;
        try {
            String theMediaStr = formatMediaUrl(vmp_MediaUrlStringArray.get(vmp_PlayBackIndexInt));
            System.out.println("will now play: " + theMediaStr);
            vmp_JMPlayer.open(theMediaStr);
        } catch (IOException ex) {
            Logger.getLogger(MediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void playIndex() {
        playIndex(vmp_PlayBackIndexInt);
    }

    public void removeIndex(int theIndex) {
        stop(-1);
        vmp_MediaUrlStringArray.remove(theIndex);
    }

    public void shufflePlayList() {
        if ((vmp_MediaUrlStringArray != null)
                && (vmp_MediaUrlStringArray.size() > 0)
                && (vmp_PlayBackIndexInt < vmp_MediaUrlStringArray.size())) {
            if (vmp_PlayBackIndexInt >= 0) {
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
            } else {
                Collections.shuffle(vmp_MediaUrlStringArray);
            }
        }
    }

    public void togglePlayPause() {
        vmp_JMPlayer.togglePlay();
    }

    public void next() {
        if ((vmp_PlayBackIndexInt + 1) < vmp_MediaUrlStringArray.size()) {
            vmp_PlayBackIndexInt++;
            playIndex();
        } else { //no more items in playlist, stop process
            stop();
        }
    }

    public void previous() {
        if ((vmp_PlayBackIndexInt - 1) >= 0) {
            vmp_PlayBackIndexInt--;
            playIndex();
        } else { //at beginning of playlist, loop
            stop();
        }
    }

    public void stop(int theIndex) {
        vmp_PlayBackIndexInt = theIndex;
        vmp_JMPlayer.close();
    }

    public void stop() {
        stop(-1);
    }

    public int getCurrentIndex() {
        return vmp_PlayBackIndexInt;
    }

    public List<String> getPlayList() {
        return vmp_MediaUrlStringArray;
    }
}
