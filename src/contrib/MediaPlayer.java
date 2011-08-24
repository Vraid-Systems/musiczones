/*
 * singleton class wrapper for controlling mplayer. for slave commands see:
 * http://www.mplayerhq.hu/DOCS/tech/slave.txt
 */
package contrib;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import multicastmusiccontroller.ProgramConstants;
import zoneserver.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class MediaPlayer implements ProgramConstants {

    private static MediaPlayer vmp_SingleInstance = null;
    private List<String> vmp_MediaUrlStringArray = null;
    private int vmp_PlayBackIndexInt = -1;
    private Process vmp_MPlayerProcess = null;
    private String vmp_MPlayerBinPath = null;

    protected MediaPlayer() {
        vmp_MediaUrlStringArray = new LinkedList();
        vmp_MPlayerBinPath = ZoneServerUtility.getInstance().loadStringPref(prefMediaPlayerPathKeyStr,
                defaultMPlayerBinPath);
    }

    @Override
    public void finalize() throws Throwable {
        if (vmp_MPlayerProcess != null) {
            stop();
            vmp_MPlayerProcess.destroy();
        }
        super.finalize();
    }

    public static MediaPlayer getInstance() {
        if (vmp_SingleInstance == null) {
            vmp_SingleInstance = new MediaPlayer();
        }
        return vmp_SingleInstance;
    }

    public void addMediaUrl(String theMediaUrlStr) {
        //format windowsy backslashes if this is on a windows host and samba-ing
        if (ZoneServerUtility.getInstance().isWindows()
                && theMediaUrlStr.contains(ServerType.smb.toString().concat(prefixUriStr))) {
            theMediaUrlStr = theMediaUrlStr.replace(ServerType.smb.toString().concat(prefixUriStr), "\\\\");
            theMediaUrlStr = theMediaUrlStr.replace("/", "\\");
        }

        vmp_MediaUrlStringArray.add(theMediaUrlStr);
        System.out.println("added media url: " + theMediaUrlStr);
    }

    protected void initPlayer() {
        try {
            String startCmd = vmp_MPlayerBinPath + " -slave -quiet -idle "
                    + '"' + vmp_MediaUrlStringArray.get(vmp_PlayBackIndexInt) + '"';
            vmp_MPlayerProcess = Runtime.getRuntime().exec(startCmd);
            System.out.println("mplayer process: " + startCmd);
        } catch (IOException ex) {
            Logger.getLogger(MediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void writeSlaveCommand(String theCommand) throws IOException {
        if (vmp_MPlayerProcess != null) {
            OutputStream processInputStream = vmp_MPlayerProcess.getOutputStream();
            processInputStream.write(theCommand.getBytes());
            processInputStream.write("\n".getBytes());
            processInputStream.flush();
            System.out.println("mplayer process passed command: " + theCommand);
        }
    }

    public void playIndex(int theIndex) throws IOException {
        vmp_PlayBackIndexInt = theIndex;
        if (vmp_MPlayerProcess == null) {
            initPlayer();
        } else {
            String playFileCmdStr = "loadfile " + '"' + vmp_MediaUrlStringArray.get(vmp_PlayBackIndexInt) + '"' + " 0";
            writeSlaveCommand(playFileCmdStr);
        }
    }

    public void playIndex() throws IOException {
        playIndex(vmp_PlayBackIndexInt);
    }

    public void removeIndex(int theIndex) {
        if ((vmp_PlayBackIndexInt > 0) && (theIndex <= vmp_PlayBackIndexInt)) {
            vmp_PlayBackIndexInt--;
        }
        vmp_MediaUrlStringArray.remove(theIndex);
    }

    public void shufflePlayList() {
        if ((vmp_MediaUrlStringArray != null)
                && (vmp_MediaUrlStringArray.size() > 0)
                && (vmp_PlayBackIndexInt < vmp_MediaUrlStringArray.size())) {
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
    }

    public void togglePlayPause() throws IOException {
        if (vmp_MPlayerProcess == null) {
            initPlayer();
        } else {
            writeSlaveCommand("pause");
        }
    }

    public void setVolume(int theVolume) throws IOException {
        if (vmp_MPlayerProcess != null) {
            writeSlaveCommand("volume " + String.valueOf(theVolume) + " 0");
        }
    }

    public void stop() throws IOException {
        if (vmp_MPlayerProcess != null) {
            writeSlaveCommand("stop");
        }
    }

    public void next() throws IOException {
        if ((vmp_PlayBackIndexInt + 1) < vmp_MediaUrlStringArray.size()) {
            vmp_PlayBackIndexInt++;
            playIndex();
        } else { //no more items in playlist, stop process
            stop();
        }
    }

    public void previous() throws IOException {
        if ((vmp_PlayBackIndexInt - 1) >= 0) {
            vmp_PlayBackIndexInt--;
            playIndex();
        } else { //at beginning of playlist, loop
            stop();
        }
    }

    public int getCurrentIndex() {
        return vmp_PlayBackIndexInt;
    }

    public List<String> getPlayList() {
        return vmp_MediaUrlStringArray;
    }
}
