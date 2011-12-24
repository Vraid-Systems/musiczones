/*
 * ID3 MetaData processor implementation using the jid3 library
 */
package contrib;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import jcifs.smb.SmbFile;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v2.ID3V2Tag;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class ID3MetaData {

    private MP3File imd_mp3File = null;
    private ID3V2Tag imd_id3Tag = null;
    public static final String imd_kSmbPrefix = "smb:";
    public static final String imd_GenreSplitString = ";";

    public ID3MetaData(String theMediaFilePathStr) throws MalformedURLException, ID3Exception {
        if (theMediaFilePathStr.contains(imd_kSmbPrefix)) {
            if (ZoneServerUtility.getInstance().isWindows()) { //use Window's built in SMB support
                theMediaFilePathStr = theMediaFilePathStr.replace(imd_kSmbPrefix, "\\");
                imd_mp3File = new MP3File(new File(theMediaFilePathStr));
            } else { //use Smb specialty file handle on POSIX
                imd_mp3File = new MP3File(new SmbFileSourceImpl(new SmbFile(theMediaFilePathStr)));
            }
        } else { //regular file
            imd_mp3File = new MP3File(new File(theMediaFilePathStr));
        }
        imd_id3Tag = imd_mp3File.getID3V2Tag();
    }

    public String getAlbum() {
        return imd_id3Tag.getAlbum();
    }

    public String getArtist() {
        return imd_id3Tag.getArtist();
    }

    /**
     * gets the genre ID3 string from file, parses into list
     * @return ArrayList<String>
     */
    public ArrayList<String> getGenresAsList() {
        String aSongGenre = imd_id3Tag.getGenre();
        if (aSongGenre == null) {
            return null;
        }

        ArrayList<String> aGenreReturnList = new ArrayList<String>();
        if (aSongGenre.contains(imd_GenreSplitString)) {
            String[] aSongGenreArray = aSongGenre.split(imd_GenreSplitString);
            for (String aGenre : aSongGenreArray) {
                aGenre = aGenre.trim();
                aGenreReturnList.add(aGenre);
            }
        } else {
            aGenreReturnList.add(aSongGenre);
        }
        return aGenreReturnList;
    }

    public String getTitle() {
        return imd_id3Tag.getTitle();
    }
}
