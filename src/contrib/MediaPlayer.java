/*
 * singleton class wrapper for controlling mplayer. for slave commands see:
 * http://www.mplayerhq.hu/DOCS/tech/slave.txt
 */
package contrib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
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
    private Process vmp_MPlayerProcess = null;
    private PrintStream vmp_MPlayerProcessIn = null;
    private BufferedReader vmp_MPlayerProcessOutErr = null;
    private String vmp_MPlayerBinPath = null;

    protected MediaPlayer() {
        vmp_MediaUrlStringArray = new LinkedList();
        vmp_MPlayerBinPath = ZoneServerUtility.getInstance().loadStringPref(prefMediaPlayerPathKeyStr, "");
        if (vmp_MPlayerBinPath.isEmpty()) {
            MulticastMusicController.MPlayerNotFound();
        }
    }

    @Override
    public void finalize() throws Throwable {
        if (vmp_MPlayerProcess != null) {
            stop();
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

    protected void initPlayer() throws IOException {
        if (vmp_PlayBackIndexInt < 0) {
            vmp_PlayBackIndexInt = 0;
        }
        try {
            String startCmd = vmp_MPlayerBinPath + " -slave -quiet -idle "
                    + '"' + vmp_MediaUrlStringArray.get(vmp_PlayBackIndexInt) + '"';
            vmp_MPlayerProcess = Runtime.getRuntime().exec(startCmd);
            System.out.println("mplayer process: " + startCmd);
        } catch (IOException ex) {
            Logger.getLogger(MediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }

        // create the piped streams where to redirect the standard output and error of MPlayer
        // specify a bigger pipesize than the default of 1024
        PipedInputStream readFrom = new PipedInputStream(256 * 1024);
        PipedOutputStream writeTo = new PipedOutputStream(readFrom);
        vmp_MPlayerProcessOutErr = new BufferedReader(new InputStreamReader(readFrom));

        // create the threads to redirect the standard output and error of MPlayer
        new LineRedirecter(vmp_MPlayerProcess.getInputStream(), writeTo).start();
        new LineRedirecter(vmp_MPlayerProcess.getErrorStream(), writeTo).start();

        // the standard input of MPlayer
        vmp_MPlayerProcessIn = new PrintStream(vmp_MPlayerProcess.getOutputStream());
    }

    protected void writeSlaveCommand(String theCommand) throws IOException {
        if (vmp_MPlayerProcessIn != null) {
            vmp_MPlayerProcessIn.print(theCommand);
            vmp_MPlayerProcessIn.print("\n");
            vmp_MPlayerProcessIn.flush();
            System.out.println("mplayer process passed command: " + theCommand);
        }
    }

    public void playIndex(int theIndex) throws IOException {
        vmp_PlayBackIndexInt = theIndex;
        if (vmp_MPlayerProcess == null) {
            initPlayer();
        } else {
            stop(theIndex);
            try { //sleep 1/5 second. just enough for previous mplayer to close up
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(MediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
            initPlayer();
        }
    }

    public void playIndex() throws IOException {
        playIndex(vmp_PlayBackIndexInt);
    }

    public void removeIndex(int theIndex) {
        try {
            stop();
        } catch (IOException ex) {
            Logger.getLogger(MediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if ((vmp_PlayBackIndexInt >= 0) && (theIndex <= vmp_PlayBackIndexInt)) {
            vmp_PlayBackIndexInt--;
        }
        vmp_MediaUrlStringArray.remove(theIndex);
    }

    public void shufflePlayList() {
        try {
            stop(vmp_PlayBackIndexInt);
        } catch (IOException ex) {
            Logger.getLogger(MediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if ((vmp_MediaUrlStringArray != null) //TODO: something about this does not work
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

    public void stop(int theIndex) throws IOException {
        if (vmp_MPlayerProcess != null) {
            writeSlaveCommand("quit 0");
            vmp_MPlayerProcess.destroy();
            vmp_MPlayerProcess = null;
            vmp_PlayBackIndexInt = theIndex;
        }
    }

    public void stop() throws IOException {
        stop(-1);
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
