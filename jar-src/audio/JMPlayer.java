/*
 * class for managing direct slave interface to MPlayer executable
 * slight modifications from original
 * for slave commands: http://www.mplayerhq.hu/DOCS/tech/slave.txt
 */
package audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import musiczones.MusicZones;
import zonecontrol.ZoneLibraryIndex;

/**
 * @author Adrian BER
 * @see http://beradrian.users.sourceforge.net/articles/JMPlayer.java
 */
public class JMPlayer {

    private static final Logger logger = Logger.getLogger(JMPlayer.class.getName());
    public static final int kPipeSizeBytes = 3145728; //3MB

    /** A thread that reads from an input stream and outputs to another line by line. */
    private static class LineRedirecter extends Thread {

        /** The input stream to read from. */
        private InputStream in;
        /** The output stream to write to. */
        private OutputStream out;
        /** The prefix used to prefix the lines when outputting to the logger. */
        private String prefix;

        /**
         * @param in the input stream to read from.
         * @param out the output stream to write to.
         * @param prefix the prefix used to prefix the lines when outputting to the logger.
         */
        LineRedirecter(InputStream in, OutputStream out, String prefix) {
            this.in = in;
            this.out = out;
            this.prefix = prefix;
        }

        @Override
        public void run() {
            // creates the decorating reader and writer
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            PrintStream printStream = new PrintStream(out);
            String line;
            try {
                // read line by line
                while ((line = reader.readLine()) != null) {
                    if (MusicZones.getIsDebugOn()) {
                        logger.log(Level.INFO, "{0}{1}", new Object[]{prefix != null ? prefix : "", line});
                    }
                    printStream.println(line);
                }
            } catch (IOException ex) {
                Logger.getLogger(JMPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /** The path to the MPlayer executable. */
    private String mplayerPath = "mplayer";
    /** Options passed to MPlayer. */
    private String mplayerOptions = "-slave";//"-slave -idle";
    /** The process corresponding to MPlayer. */
    private Process mplayerProcess;
    /** The standard input for MPlayer where you can send commands. */
    private PrintStream mplayerIn;
    /** A combined reader for the the standard output and error of MPlayer. Used to read MPlayer responses. */
    private BufferedReader mplayerOutErr;

    public JMPlayer() {
    }

    /** @return the path to the MPlayer executable. */
    public String getMPlayerPath() {
        return mplayerPath;
    }

    /** Sets the path to the MPlayer executable.
     * @param mplayerPath the new MPlayer path; this will be in effect
     * after {@link #close() closing} the currently running player.
     */
    public void setMPlayerPath(String mplayerPath) {
        this.mplayerPath = mplayerPath;
    }

    public Process open(String theMediaPath) throws IOException {
        if (mplayerProcess != null) {
            close(); //close up the old player if one is already open
        }

        // start MPlayer as an external process
        if (ZoneLibraryIndex.getInstance().theContainerIsPlayList(theMediaPath)) {
            mplayerProcess = new ProcessBuilder(mplayerPath, mplayerOptions, "-playlist", theMediaPath).start();
        } else {
            mplayerProcess = new ProcessBuilder(mplayerPath, mplayerOptions, theMediaPath).start();
        }

        // create the piped streams where to redirect the standard output and error of MPlayer
        // specify a bigger pipesize
        PipedInputStream readFrom = new PipedInputStream(kPipeSizeBytes);
        PipedOutputStream writeTo = new PipedOutputStream(readFrom);
        mplayerOutErr = new BufferedReader(new InputStreamReader(readFrom));

        // create the threads to redirect the standard output and error of MPlayer
        new LineRedirecter(mplayerProcess.getInputStream(), writeTo, "MPlayer says: ").start();
        new LineRedirecter(mplayerProcess.getErrorStream(), writeTo, "MPlayer encountered an error: ").start();

        // the standard input of MPlayer
        mplayerIn = new PrintStream(mplayerProcess.getOutputStream());

        // wait to start playing
        waitForAnswer("Starting playback...");
        if (MusicZones.getIsDebugOn()) {
            logger.log(Level.INFO, "Started playing file {0}", theMediaPath);
        }

        //for use in attaching an exit handler in the calling class
        return mplayerProcess;
    }

    public void close() {
        if (mplayerProcess != null) {
            execute("quit");
            try {
                mplayerProcess.waitFor();
            } catch (InterruptedException e) {
            }
            mplayerProcess = null;
        }
    }

    public String getPlayingFileStr() {
        String path = getProperty("path");
        return path;
    }

    public void togglePlay() {
        execute("pause");
    }

    public long getTimePosition() {
        return getPropertyAsLong("time_pos");
    }

    public void setTimePosition(long seconds) {
        setProperty("time_pos", seconds);
    }

    public long getTotalTime() {
        return getPropertyAsLong("length");
    }

    public float getVolume() {
        return getPropertyAsFloat("volume");
    }

    public void setVolume(float volume) {
        setProperty("volume", volume);
    }

    protected String getProperty(String name) {
        if (name == null || mplayerProcess == null) {
            return null;
        }
        String s = "ANS_" + name + "=";
        String x = execute("get_property " + name, s);
        if (x == null) {
            return null;
        }
        if (!x.startsWith(s)) {
            return null;
        }
        return x.substring(s.length());
    }

    protected long getPropertyAsLong(String name) {
        try {
            return Long.parseLong(getProperty(name));
        } catch (NumberFormatException exc) {
        } catch (NullPointerException exc) {
        }
        return 0;
    }

    protected float getPropertyAsFloat(String name) {
        try {
            return Float.parseFloat(getProperty(name));
        } catch (NumberFormatException exc) {
        } catch (NullPointerException exc) {
        }
        return 0f;
    }

    protected void setProperty(String name, String value) {
        execute("set_property " + name + " " + value);
    }

    protected void setProperty(String name, long value) {
        execute("set_property " + name + " " + value);
    }

    protected void setProperty(String name, float value) {
        execute("set_property " + name + " " + value);
    }

    /** Sends a command to MPlayer..
     * @param command the command to be sent
     */
    private void execute(String command) {
        execute(command, null);
    }

    /** Sends a command to MPlayer and waits for an answer.
     * @param command the command to be sent
     * @param expected the string with which has to start the line; if null don't wait for an answer
     * @return the MPlayer answer
     */
    private String execute(String command, String expected) {
        if (mplayerProcess != null) {
            if (MusicZones.getIsDebugOn()) {
                logger.log(Level.INFO, "Send to MPlayer the command \"{0}\" and expecting {1}", new Object[]{command, expected != null ? "\"" + expected + "\"" : "no answer"});
            }
            mplayerIn.print(command);
            mplayerIn.print("\n");
            mplayerIn.flush();
            if (MusicZones.getIsDebugOn()) {
                logger.info("Command sent");
            }
            if (expected != null) {
                String response = waitForAnswer(expected);
                if (MusicZones.getIsDebugOn()) {
                    logger.log(Level.INFO, "MPlayer command response: {0}", response);
                }
                return response;
            }
        }
        return null;
    }

    /** Read from the MPlayer standard output and error a line that starts with the given parameter and return it.
     * @param expected the expected starting string for the line
     * @return the entire line from the standard output or error of MPlayer
     */
    private String waitForAnswer(String expected) {
        // todo add the possibility to specify more options to be specified
        // todo use regexp matching instead of the beginning of a string
        String line = null;
        if (expected != null) {
            try {
                while ((line = mplayerOutErr.readLine()) != null) {
                    if (MusicZones.getIsDebugOn()) {
                        logger.log(Level.INFO, "Reading line: {0}", line);
                    }
                    if (line.startsWith(expected)) {
                        return line;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(JMPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return line;
    }
}
