/*
 * class for gobbling up MPlayer process I/O streams so the process does
 * not deadlock the entire system
 */
package ipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Adrian
 * @see http://beradrian.wordpress.com/2008/01/30/jmplayer/
 */
public class LineRedirecter extends Thread {

    private InputStream in;
    private OutputStream out;

    LineRedirecter(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        PrintStream printStream = new PrintStream(out);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                printStream.println(line);
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
