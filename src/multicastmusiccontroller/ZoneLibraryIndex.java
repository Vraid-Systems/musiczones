/*
 * a singleton class for manging and allowing acces to the zone library search index
 */
package multicastmusiccontroller;

import contrib.CIFSNetworkInterface;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class ZoneLibraryIndex implements ProgramConstants {

    private static ZoneLibraryIndex zli_SingleInstance = null;
    protected HashMap<String, String> zli_FileMap = null; //<full file path, filename>
    protected Timer zli_Timer = null;
    protected boolean debugTimedEventsOn = false;

    protected ZoneLibraryIndex() {
        zli_FileMap = new HashMap<String, String>();
        zli_Timer = new Timer();
        zli_Timer.schedule(new RefreshSearchIndexTask(), 0, searchIndexRefreshInterval * 1000);
        System.out.println("ZLI RefreshSearchIndexTask added");
    }

    public static ZoneLibraryIndex getInstance() {
        if (zli_SingleInstance == null) {
            zli_SingleInstance = new ZoneLibraryIndex();
        }
        return zli_SingleInstance;
    }

    /**
     * return a HashMap<String - full file path, String - filename> of files
     * that match the given search parameters in the library index
     * @param theKeywordStrArray String[]
     * @param matchAllKeywords boolean
     * @param theStartIndexInt Integer
     * @param theEndIndexInt Integer
     * @return HashMap<String, String>
     */
    public HashMap<String, String> getFiles(String[] theKeywordStrArray,
            boolean matchAllKeywords, int theStartIndexInt, int theEndIndexInt) {
        HashMap<String, String> returnFileMap = new HashMap<String, String>();
        if (zli_FileMap.size() > 0) {
            int i = 0;
            int aOutputCount = 0;
            List<String> aFullFilePathArray = Arrays.asList(zli_FileMap.keySet().toArray(new String[0]));
            for (String aTempFileName : zli_FileMap.values()) {
                if (stringMatchesKeywords(aTempFileName, theKeywordStrArray, matchAllKeywords)) {
                    if ((aOutputCount >= theStartIndexInt) && (aOutputCount <= theEndIndexInt)) {
                        returnFileMap.put(aFullFilePathArray.get(i), aTempFileName);
                    }
                    aOutputCount++;
                }
                i++;
            }
        }
        return returnFileMap;
    }

    /**
     * add the contents of a certain path to the library index
     * @param thePathStr String
     */
    public void indexPath(String thePathStr) {
        if (thePathStr.contains(FileSystemType.smb.toString().concat(prefixUriStr))) { //CIFS share
            ArrayList<SmbFile> aCIFSDirList = CIFSNetworkInterface.getInstance().getDirectoryList(thePathStr);
            LinkedList<SmbFile> aSmbFileLinkedList = new LinkedList<SmbFile>(aCIFSDirList);

            while (aSmbFileLinkedList.peek() != null) {
                SmbFile iSmbFile = aSmbFileLinkedList.pop();

                if (iSmbFile.getPath().endsWith("/")) { //recurse into directory during search
                    ArrayList<SmbFile> tempSmbFiles = CIFSNetworkInterface.getInstance().getDirectoryList(iSmbFile.getPath());
                    if (tempSmbFiles.size() > 0) {
                        for (SmbFile tempSmbFile : tempSmbFiles) {
                            aSmbFileLinkedList.push(tempSmbFile);
                        }
                    }
                } else { //have a file, add it to the file index map
                    String aFullFilePathStr = null;
                    try {
                        aFullFilePathStr = URLEncoder.encode(iSmbFile.getPath(), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (aFullFilePathStr != null) {
                        zli_FileMap.put(aFullFilePathStr, iSmbFile.getName());
                    }
                }
            }
        } else { //local filesytem
            File dir = new File(thePathStr);
            File[] files = dir.listFiles();
            LinkedList<File> aFileLinkedList = new LinkedList<File>();
            aFileLinkedList.addAll(Arrays.asList(files));

            while (aFileLinkedList.peek() != null) {
                File iFile = aFileLinkedList.pop();

                if (iFile.isDirectory()) { //recurse into directory during search
                    File[] tempFileArray = iFile.listFiles();
                    if (tempFileArray != null) {
                        for (File tempFile : tempFileArray) {
                            aFileLinkedList.push(tempFile);
                        }
                    }
                } else { //have a file, add it to the file index map
                    String tempFilePathStr = iFile.getAbsolutePath();
                    if (tempFilePathStr.contains("\\")) {
                        tempFilePathStr = tempFilePathStr.replaceAll("\\\\+", "/");
                        //see http://www.java-forums.org/advanced-java/16452-replacing-backslashes-string-object.html#post59396
                    }

                    String aFullFilePathStr = null;
                    try {
                        aFullFilePathStr = URLEncoder.encode(tempFilePathStr, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (aFullFilePathStr != null) {
                        zli_FileMap.put(aFullFilePathStr, iFile.getName());
                    }
                }
            }
        }
    }

    /**
     * remove all files from the library index that contain the path string
     * @param thePathStr String
     */
    public void removePath(String thePathStr) {
        for (String aTempFullFilePath : zli_FileMap.keySet()) {
            if (aTempFullFilePath.contains(thePathStr)) {
                zli_FileMap.remove(aTempFullFilePath);
            }
        }
    }

    /**
     * function to see if the given string matches (all or any) of the keyword array strings
     * returns true if no keywords are given
     * 
     * @param theString String
     * @param theKeywords String[]
     * @param matchAllKeywords boolean
     * @return boolean - did theString match the keywords appropriately?
     */
    public boolean stringMatchesKeywords(String theString, String[] theKeywords, boolean matchAllKeywords) {
        if ((theString == null) || (theKeywords == null)) {
            return true;
        } else {
            theString = theString.toLowerCase(Locale.ENGLISH);
            boolean matchOneKeywordBoolean = false;
            for (String aKeyword : theKeywords) {
                aKeyword = aKeyword.toLowerCase(Locale.ENGLISH);
                if ((matchAllKeywords) && (!theString.contains(aKeyword))) {
                    return false;
                } else if (theString.contains(aKeyword)) {
                    matchOneKeywordBoolean = true;
                }
            }
            if ((!matchAllKeywords) && (!matchOneKeywordBoolean)) {
                return false;
            }
            return true;
        }
    }

    /**
     * private time task that goes through root paths and updates library index
     */
    private class RefreshSearchIndexTask extends TimerTask {

        public void run() {
            List<String> rootPathStrList = ZoneServerUtility.getInstance().getMediaDirEntries();
            if (rootPathStrList.size() > 0) {
                for (String aRootPathStr : rootPathStrList) {
                    //pre-process strings from db
                    if (debugTimedEventsOn) {
                        System.out.println("from db: " + aRootPathStr);
                    }
                    String rootMediaNameStr = aRootPathStr;
                    if (aRootPathStr.contains(mediaNameSplitStr)) {
                        String[] rootPathStrArray = aRootPathStr.split(mediaNameSplitStr);
                        rootMediaNameStr = rootPathStrArray[0];
                        if (debugTimedEventsOn) {
                            System.out.println("rootMediaNameStr=" + rootMediaNameStr);
                        }
                        if (!aRootPathStr.contains(FileSystemType.radio.toString().concat(prefixUriStr))) {
                            aRootPathStr = rootPathStrArray[1];
                        }
                        if (debugTimedEventsOn) {
                            System.out.println("rootPathStr=" + aRootPathStr);
                        }
                    }

                    if (!aRootPathStr.contains(FileSystemType.radio.toString().concat(prefixUriStr))) { //should not be a radio link
                        if (debugTimedEventsOn) {
                            System.out.println("indexing: " + aRootPathStr);
                        }
                        if (aRootPathStr.contains(FileSystemType.smb.toString().concat(prefixUriStr))) { //CIFS share
                            SmbFile aSmbFile = null;
                            try {
                                aSmbFile = new SmbFile(aRootPathStr);
                            } catch (MalformedURLException ex) {
                                Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            if (aSmbFile == null) {
                                removePath(aRootPathStr);
                            } else {
                                SmbFile[] aSmbFileArray = null;
                                try {
                                    aSmbFileArray = aSmbFile.listFiles();
                                } catch (SmbException ex) {
                                    Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if (aSmbFileArray == null) {
                                    removePath(aRootPathStr);
                                } else {
                                    indexPath(aRootPathStr);
                                }
                            }
                        } else { //local filesytem
                            indexPath(aRootPathStr);
                        }
                    }
                }
            }
        }
    }
}
