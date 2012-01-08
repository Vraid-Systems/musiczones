/*
 * singleton class wrapper for controlling a mediaplayer implementation
 * an abstraction away from JMPlayer
 */
package contrib;

import contrib.ProcessExit.ProcessExitDetector;
import contrib.ProcessExit.ProcessListener;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import musiczones.FileSystemType;
import musiczones.MusicZones;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class MediaPlayer {

    private static MediaPlayer vmp_SingleInstance = null;
    private List<String> vmp_MediaUrlStringArray = null;
    private int vmp_PlayBackIndexInt = -1;
    private boolean debugMessagesOn = false;
    protected int lengthOfPauseBetweenMediaInSeconds = 2;
    private JMPlayer vmp_JMPlayer = null;
    public static final String prefMediaPlayerPathKeyStr = "media-player-bin-path";
    public static final String[] theSupportedContainers = {"mp3", "mp4", "ogg",
        "wav", "flac", "aac", "wma"}; //MPlayer supports video as well, only wanted audio
    public static final String[] thePlayListContainers = {"pls", "m3u"};

    protected MediaPlayer(boolean theDebugIsOn) {
        debugMessagesOn = theDebugIsOn;
        vmp_MediaUrlStringArray = new LinkedList<String>();
        String aMPlayerBinPath = ZoneServerUtility.getInstance().loadStringPref(prefMediaPlayerPathKeyStr, "");
        if (aMPlayerBinPath.isEmpty()) {
            MusicZones.MPlayerNotFound();
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
            vmp_SingleInstance = new MediaPlayer(false);
        }
        return vmp_SingleInstance;
    }

    public static MediaPlayer getInstance(boolean theDebugIsOn) {
        if (vmp_SingleInstance == null) {
            vmp_SingleInstance = new MediaPlayer(theDebugIsOn);
        }
        return vmp_SingleInstance;
    }

    public void addMediaUrl(String theMediaUrlStr) {
        vmp_MediaUrlStringArray.add(theMediaUrlStr);
        if (debugMessagesOn) {
            System.out.println("added media url: " + theMediaUrlStr);
        }
    }

    protected String formatMediaUrl(String theMediaUrlStr) {
        //format windowsy backslashes if this is on a windows host and samba-ing
        if (ZoneServerUtility.getInstance().isWindows()
                && theMediaUrlStr.contains(FileSystemType.smb.toString().concat(ZoneServerUtility.prefixUriStr))) {
            theMediaUrlStr = theMediaUrlStr.replace(FileSystemType.smb.toString().concat(ZoneServerUtility.prefixUriStr), "\\\\");
            theMediaUrlStr = theMediaUrlStr.replace("/", "\\");
        }

        //remove radio prefix if it exists in media string
        if (theMediaUrlStr.contains(FileSystemType.radio.toString().concat(ZoneServerUtility.prefixUriStr))) {
            theMediaUrlStr = theMediaUrlStr.replace(FileSystemType.radio.toString().concat(ZoneServerUtility.prefixUriStr), "");
        }

        //replace spaces with escape chars if not windows
        if ((!ZoneServerUtility.getInstance().isWindows())) {
            //%20 if this is an external URL
            if (theMediaUrlStr.contains(FileSystemType.smb.toString().concat(ZoneServerUtility.prefixUriStr))) {
                theMediaUrlStr = theMediaUrlStr.replace(" ", "%20");
            }
        }

        return theMediaUrlStr;
    }

    public void playIndex(int theIndex) {
        vmp_PlayBackIndexInt = theIndex;
        try {
            String theMediaStr = formatMediaUrl(vmp_MediaUrlStringArray.get(vmp_PlayBackIndexInt));
            if (debugMessagesOn) {
                System.out.println("will now play: " + theMediaStr);
            }
            Process aMPlayerProcess = vmp_JMPlayer.open(theMediaStr);

            if (aMPlayerProcess != null) { //attach a process exit handler, if process started correctly
                final int earlierPlayBackIndexInt = vmp_PlayBackIndexInt;

                ProcessExitDetector processExitDetector = new ProcessExitDetector(aMPlayerProcess);
                processExitDetector.addProcessListener(new ProcessListener() {

                    public void processFinished(Process process) {
                        if (vmp_PlayBackIndexInt > -1) { //if not manually stopped ...
                            final int laterPlayBackIndexInt = vmp_PlayBackIndexInt;

                            try {
                                Thread.sleep(lengthOfPauseBetweenMediaInSeconds * 1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(MusicZones.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            if (debugMessagesOn) {
                                System.out.println("earlier: " + earlierPlayBackIndexInt);
                            }
                            if (debugMessagesOn) {
                                System.out.println("later: " + laterPlayBackIndexInt);
                            }
                            if (earlierPlayBackIndexInt == laterPlayBackIndexInt) {
                                next(); //auto-advance to the next playlist item
                            }
                        }
                    }
                });
                processExitDetector.start();
            }
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

    public boolean isStopped() {
        return (vmp_PlayBackIndexInt < 0);
    }

    public void clearPlaylist() {
        stop();
        vmp_MediaUrlStringArray.clear();
    }

    public int getCurrentIndex() {
        return vmp_PlayBackIndexInt;
    }

    public List<String> getPlayList() {
        return vmp_MediaUrlStringArray;
    }
}
