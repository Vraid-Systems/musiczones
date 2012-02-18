/*
 * interface for keeping consistent MediaPlayer on JAR and Android
 */
package audio;

import java.util.List;

/**
 * @author Jason Zerbe
 */
public interface MediaPlayerIFace {

    void addMediaUrl(String theMediaUrlStr);

    void playIndex(int theIndex);

    void playIndex();

    void removeIndex(int theIndex);

    void shufflePlayList();

    void togglePlayPause();

    void next();

    void previous();

    void stop(int theIndex);

    void stop();

    boolean isStopped();

    void clearPlaylist();

    int getCurrentIndex();

    List<String> getPlayList();
}
