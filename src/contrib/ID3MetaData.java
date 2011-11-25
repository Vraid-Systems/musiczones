/*
 * ID3 MetaData processor implementation using the jid3 library
 */
package contrib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v2_3;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class ID3MetaData {

    private ID3v2_3 imd_ID3 = null;
    public static final String imd_GenreSplitString = ";";

    public ID3MetaData(File theMediaFile) throws FileNotFoundException, IOException, TagException {
        RandomAccessFile aRandomAccessFile = new RandomAccessFile(theMediaFile, "r");
        imd_ID3 = new ID3v2_3(aRandomAccessFile);
    }

    public ID3MetaData(String theMediaFilePathStr) throws FileNotFoundException, IOException, TagException {
        if (ZoneServerUtility.getInstance().isWindows()) {
            theMediaFilePathStr = theMediaFilePathStr.replace("smb:", "\\");
        }
        RandomAccessFile aRandomAccessFile = new RandomAccessFile(new File(theMediaFilePathStr), "r");
        imd_ID3 = new ID3v2_3(aRandomAccessFile);
    }

    public String getAlbum() {
        return imd_ID3.getAlbumTitle();
    }

    /**
     * gets the genre ID3 string from file, parses into list
     * @return ArrayList<String>
     */
    public ArrayList<String> getGenresAsList() {
        String aSongGenre = imd_ID3.getSongGenre();
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
}
