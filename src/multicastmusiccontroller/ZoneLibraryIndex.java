/*
 * a singleton class for manging and allowing acces to the zone library search index
 * only indexes one copy of media with a certain filename
 *
 * NOTE: recursive symlinks does this puppy in
 */
package multicastmusiccontroller;

import contrib.CIFSNetworkInterface;
import contrib.MediaPlayer;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
public class ZoneLibraryIndex {

    private static ZoneLibraryIndex zli_SingleInstance = null;
    protected HashMap<String, String> zli_FileMap = null; //<full file path, filename>
    protected Timer zli_Timer = null;
    protected boolean debugEventsOn = false;
    protected int zli_RefreshIndexSeconds = 120;
    protected String[] zli_PathBlackListArray = {"$", "tftp", "install"};

    protected ZoneLibraryIndex(boolean theDebugIsOn) {
        debugEventsOn = theDebugIsOn;
        zli_FileMap = new HashMap<String, String>();
        zli_Timer = new Timer();
        zli_Timer.schedule(new RefreshSearchIndexTask(), 0,
                zli_RefreshIndexSeconds * 1000);
        System.out.println("ZLI RefreshSearchIndexTask added");
    }

    public static ZoneLibraryIndex getInstance() {
        if (zli_SingleInstance == null) {
            zli_SingleInstance = new ZoneLibraryIndex(false);
        }
        return zli_SingleInstance;
    }

    public static ZoneLibraryIndex getInstance(boolean theDebugIsOn) {
        if (zli_SingleInstance == null) {
            zli_SingleInstance = new ZoneLibraryIndex(theDebugIsOn);
        }
        return zli_SingleInstance;
    }

    /**
     * return all files within a certain range of those currently indexed
     * @param theStartIndexInt Integer
     * @param theEndIndexInt Integer
     * @return HashMap<String, String>
     */
    public HashMap<String, String> getFiles(int theStartIndexInt, int theEndIndexInt) {
        HashMap<String, String> returnFileMap = new HashMap<String, String>();
        if (zli_FileMap.size() > 0) {
            int i = 0;
            List<String> aFullFilePathArray = Arrays.asList(zli_FileMap.keySet().toArray(new String[0]));
            for (String aTempFileName : zli_FileMap.values()) {
                if ((i >= theStartIndexInt) && (i <= theEndIndexInt)) {
                    returnFileMap.put(aFullFilePathArray.get(i), aTempFileName);
                }
                i++;
            }

            if (debugEventsOn) {
                System.out.println("ZLI getFiles - output " + String.valueOf(i) + " files");
            }
        }
        return returnFileMap;
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

            if (debugEventsOn) {
                System.out.println("ZLI getFiles - output " + String.valueOf(i) + " files");
            }
        }
        return returnFileMap;
    }

    /**
     * add the contents of a certain path to the library index
     * @param thePathStr String
     */
    protected void indexPath(String thePathStr) {
        if (debugEventsOn) {
            System.out.println("ZLI indexPath - will now index " + thePathStr);
        }

        if (thePathStr.contains(FileSystemType.smb.toString().concat(ZoneServerUtility.prefixUriStr))) { //CIFS share
            ArrayList<SmbFile> aCIFSDirList = CIFSNetworkInterface.getInstance().getDirectoryList(thePathStr);
            LinkedList<SmbFile> aSmbFileLinkedList = new LinkedList<SmbFile>(aCIFSDirList);

            while (aSmbFileLinkedList.peek() != null) {
                SmbFile iSmbFile = aSmbFileLinkedList.pop();

                if (iSmbFile.getPath().endsWith("/")) { //recurse into directory during search
                    ArrayList<SmbFile> tempSmbFiles = CIFSNetworkInterface.getInstance().getDirectoryList(iSmbFile.getPath());
                    if ((tempSmbFiles != null) && (tempSmbFiles.size() > 0)) {
                        for (SmbFile tempSmbFile : tempSmbFiles) {
                            aSmbFileLinkedList.push(tempSmbFile);

                            if (debugEventsOn) {
                                System.out.println("ZLI indexPath - will follow " + tempSmbFile.toString());
                            }
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
                        if (theContainerIsSupported(aFullFilePathStr)) {
                            if (!zli_FileMap.containsValue(iSmbFile.getName())) {
                                zli_FileMap.put(aFullFilePathStr, iSmbFile.getName());

                                if (debugEventsOn) {
                                    System.out.println("ZLI indexPath - added " + aFullFilePathStr);
                                }
                            }
                        }
                    }
                }
            }

            if (debugEventsOn) {
                System.out.println("ZLI indexPath - done indexing " + thePathStr);
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
                        if (theContainerIsSupported(aFullFilePathStr)) {
                            if (!zli_FileMap.containsValue(iFile.getName())) {
                                zli_FileMap.put(aFullFilePathStr, iFile.getName());

                                if (debugEventsOn) {
                                    System.out.println("ZLI indexPath - added " + aFullFilePathStr);
                                }
                            }
                        }
                    }
                }
            }

            if (debugEventsOn) {
                System.out.println("ZLI indexPath - done indexing " + thePathStr);
            }
        }
    }

    /**
     * remove all files from the library index that contain the path string
     * @param thePathStr String
     */
    protected void removePath(String thePathStr) {
        thePathStr = thePathStr.toLowerCase(Locale.ENGLISH);
        try {
            thePathStr = URLEncoder.encode(thePathStr, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (String aTempFullFilePath : zli_FileMap.keySet()) {
            if (aTempFullFilePath.contains(thePathStr)) {
                zli_FileMap.remove(aTempFullFilePath);

                if (debugEventsOn) {
                    System.out.println("ZLI removePath - removed " + thePathStr);
                }
            }
        }
    }

    /**
     * remove all indexed media that does not contain a server in the input list
     * @param theHostList LinkedList<SmbFile>
     */
    protected void removeOffline(LinkedList<SmbFile> theHostList) {
        if ((theHostList == null) || (zli_FileMap == null)) {
            return;
        }

        for (String aTempFullFilePath : zli_FileMap.keySet()) {
            boolean aRemoveFile = true;

            Iterator<SmbFile> aServerSmbFileIter = theHostList.iterator();
            while (aServerSmbFileIter.hasNext()) {
                SmbFile aServerSmbFile = aServerSmbFileIter.next();
                String aServerStrEncoded;
                try {
                    aServerStrEncoded = URLEncoder.encode(aServerSmbFile.toString(), "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }
                if (aTempFullFilePath.contains(aServerStrEncoded)) {
                    aRemoveFile = false;
                    break;
                }
            }

            if (aRemoveFile) {
                zli_FileMap.remove(aTempFullFilePath);

                if (debugEventsOn) {
                    System.out.println("ZLI removeOffline - removed " + aTempFullFilePath);
                }
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
     * exact string matching of filename extension to see if file is in
     * one of the supported container formats
     * @param theFileName String
     * @return boolean - is it supported?
     */
    public boolean theContainerIsSupported(String theFileName) {
        if (theFileName.contains(".")) {
            for (String aSupportedContainer : MediaPlayer.theSupportedContainers) {
                if (theFileName.contains("." + aSupportedContainer)) {
                    return true;
                }
            }
            return theContainerIsPlayList(theFileName);
        }
        return false;
    }

    /**
     * check to see if file is a valid playlist container file based
     * on exact string extension matching
     * @param theFileName String
     * @return boolean - is it a playlist file?
     */
    public boolean theContainerIsPlayList(String theFileName) {
        for (String aPlayListContainer : MediaPlayer.thePlayListContainers) {
            if (theFileName.contains("." + aPlayListContainer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if the passed path string is a blacklisted one
     * @param thePathStr String
     * @return boolean - is it blacklisted?
     */
    protected boolean thePathIsBlackListed(String thePathStr) {
        for (String aPathStr : zli_PathBlackListArray) {
            if (thePathStr.contains(aPathStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * private time task that adds all supported media from CIFS shares
     */
    private class RefreshSearchIndexTask extends TimerTask {

        private String aSmbPrefix = "smb://";

        public void run() {
            //index CIFS network for servers
            LinkedList<SmbFile> aHostList = new LinkedList<SmbFile>();
            String[] aWorkGroupArray = null;
            try {
                SmbFile aRootSmbFile = new SmbFile(aSmbPrefix);
                try {
                    aWorkGroupArray = aRootSmbFile.list();
                } catch (SmbException ex) {
                    Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (aWorkGroupArray == null) {
                System.err.println("ZLI RefreshSearchIndexTask - no workgroups found");
                return;
            }

            for (String aWorkGroup : aWorkGroupArray) {
                SmbFile aWorkGroupSmbFile;
                try {
                    aWorkGroupSmbFile = new SmbFile(aSmbPrefix + aWorkGroup);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.WARNING, null, ex);
                    continue;
                }
                String[] aServerArray;
                try {
                    aServerArray = aWorkGroupSmbFile.list();
                } catch (SmbException ex) {
                    Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.WARNING, null, ex);
                    continue;
                }
                if (aServerArray != null) {
                    for (String aServer : aServerArray) {
                        SmbFile aServerSmbFile;
                        try {
                            aServerSmbFile = new SmbFile(aSmbPrefix + aServer);
                        } catch (MalformedURLException ex) {
                            Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.WARNING, null, ex);
                            continue;
                        }
                        if (!aHostList.contains(aServerSmbFile)) {
                            if (!aServerSmbFile.toString().endsWith("/")) {
                                String aServerStr = aServerSmbFile.toString() + "/";
                                aServerStr = aServerStr.toLowerCase(Locale.ENGLISH);
                                try {
                                    aServerSmbFile = new SmbFile(aServerStr);
                                } catch (MalformedURLException ex) {
                                    Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.SEVERE, null, ex);
                                    continue;
                                }
                            }

                            aHostList.add(aServerSmbFile);

                            if (debugEventsOn) {
                                System.out.println("ZLI RefreshSearchIndexTask - added "
                                        + aServerSmbFile.toString() + " to host cache");
                            }
                        }
                    }
                }
            }

            //remove offline media - indexed media no longer has an indexed server
            removeOffline(aHostList);

            //spider all open shares on indexed servers for media
            Iterator<SmbFile> aServerSmbFileIter = aHostList.iterator();
            while (aServerSmbFileIter.hasNext()) {
                SmbFile aServerSmbFile = aServerSmbFileIter.next();
                try {
                    String[] aSharePathArray = aServerSmbFile.list();
                    for (String aSharePath : aSharePathArray) {
                        aSharePath = aSharePath.toLowerCase(Locale.ENGLISH);
                        if (!thePathIsBlackListed(aSharePath)) {
                            if (!aSharePath.endsWith("/")) {
                                aSharePath = aSharePath + "/";
                            }
                            indexPath(aServerSmbFile.toString() + aSharePath);
                        }
                    }
                } catch (SmbException ex) {
                    Logger.getLogger(ZoneLibraryIndex.class.getName()).log(Level.WARNING, null, ex);
                    continue;
                }
            }
        }
    }
}
