/*
 * class that makes use of the MpegAudioSPI1.9.5 library to play and load
 * information about MP3 files
 * 
 * TODO: allow for trick-modes during playback
 */
package contrib;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * @author javazoom.net
 * @see http://www.javazoom.net/mp3spi/documents.html
 */
public class Mp3Audio {

    public Mp3Audio() {
    }

    /**
     * play an MP3 file located at a certain URL
     * @param theAudioFileUrlStr String
     * @return boolean - were we able to play the audio file at said URL?
     */
    public boolean playURL(String theAudioFileUrlStr) {
        // create the audio file url
        URL theAudioFileUrl;
        try {
            theAudioFileUrl = new URL(theAudioFileUrlStr);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Mp3Audio.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        // get the audio input stream from the url
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(theAudioFileUrl);
        } catch (UnsupportedAudioFileException ex) {
            Logger.getLogger(Mp3Audio.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(Mp3Audio.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        // convert to PCM for playback
        AudioInputStream din = null;
        AudioFormat baseFormat = in.getFormat();
        AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false);
        din = AudioSystem.getAudioInputStream(decodedFormat, in);

        // play audio now
        try {
            playRAW(decodedFormat, din);
        } catch (IOException ex) {
            Logger.getLogger(Mp3Audio.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (LineUnavailableException ex) {
            Logger.getLogger(Mp3Audio.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        // close audio input stream when done
        try {
            in.close();
        } catch (IOException ex) {
            Logger.getLogger(Mp3Audio.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    /**
     * internal method to play the raw PCM data from decoded MP3 file
     * @param targetFormat AudioFormat
     * @param din AudioInputStream
     * @throws IOException
     * @throws LineUnavailableException 
     */
    private void playRAW(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            // Start
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) {
                    nBytesWritten = line.write(data, 0, nBytesRead);
                }
            }
            // Stop
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }
}
