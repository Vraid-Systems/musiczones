/*
 * servlet for displaying and managing library elements
 */
package servlets;

import contrib.CIFSNetworkInterface;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import multicastmusiccontroller.ProgramConstants;
import multicastmusiccontroller.ZoneLibraryIndex;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class ZoneLibrary extends HttpServlet implements ProgramConstants {

    public ZoneLibrary() {
    }

    /**
     * output elements to theOutput when browsing directory
     * @param thePathStr String - the directory to browse (should never have backslashes, and be terminated with a trailing forward slash)
     * @param theStartIndexInt Integer
     * @param theEndIndexInt Integer
     * @param theOutput PrintWriter - the servlet response output object
     * @return Integer - how many files are there in the directory?
     */
    private int mediaElementsBrowseOutput(String thePathStr, int theStartIndexInt, int theEndIndexInt, PrintWriter theOutput) {
        if (thePathStr.contains(FileSystemType.smb.toString().concat(prefixUriStr))) { //CIFS share
            System.out.println("viewing samba dir [" + theStartIndexInt + " - " + theEndIndexInt + "]: " + thePathStr);
            ArrayList<SmbFile> aCIFSDirList = CIFSNetworkInterface.getInstance().getDirectoryList(thePathStr);
            if (aCIFSDirList == null) {
                return 0;
            } else {
                LinkedList<SmbFile> aSmbFileLinkedList = new LinkedList<SmbFile>(aCIFSDirList);
                int i = 0; //number of files output
                while (aSmbFileLinkedList.peek() != null) {
                    SmbFile iSmbFile = aSmbFileLinkedList.pop();

                    if ((i >= theStartIndexInt) && (i <= theEndIndexInt)) {
                        theOutput.println("<li class='zoneLibraryListItem_" + i + "'>");
                        try {
                            if (iSmbFile.isDirectory()) {
                                try {
                                    theOutput.println("<a href='javascript:mediaLibrary_LoadDirectory(&quot;"
                                            + URLEncoder.encode(iSmbFile.getPath(), "UTF-8") + "&quot;);'>"
                                            + iSmbFile.getName() + "</a>");
                                } catch (UnsupportedEncodingException ex) {
                                    Logger.getLogger(ZoneLibrary.class.getName()).log(Level.WARNING, null, ex);
                                }
                            } else {
                                try {
                                    theOutput.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                            + URLEncoder.encode(iSmbFile.getPath(), "UTF-8") + "&quot;);'>"
                                            + iSmbFile.getName() + "</a>");
                                } catch (UnsupportedEncodingException ex) {
                                    Logger.getLogger(ZoneLibrary.class.getName()).log(Level.WARNING, null, ex);
                                }
                            }
                        } catch (SmbException ex) {
                            Logger.getLogger(ZoneLibrary.class.getName()).log(Level.WARNING, null, ex);
                        }
                        theOutput.println("</li>");
                    }

                    i++;
                }
                return (i + 1);
            }
        } else { //local filesytem
            System.out.println("viewing local dir [" + theStartIndexInt + " - " + theEndIndexInt + "]: " + thePathStr);
            File dir = new File(thePathStr);
            File[] files = dir.listFiles();
            LinkedList<File> aFileLinkedList = new LinkedList<File>();
            aFileLinkedList.addAll(Arrays.asList(files));
            if (aFileLinkedList.size() <= 0) {
                return 0;
            } else {
                int i = 0; //number of files output
                while (aFileLinkedList.peek() != null) {
                    File aFile = aFileLinkedList.pop();

                    if ((i >= theStartIndexInt) && (i <= theEndIndexInt)) {
                        String tempFilePathStr = aFile.getAbsolutePath();
                        if (tempFilePathStr.contains("\\")) {
                            tempFilePathStr = tempFilePathStr.replaceAll("\\\\+", "/");
                            //see http://www.java-forums.org/advanced-java/16452-replacing-backslashes-string-object.html#post59396
                        }

                        String tempFileNameStr = aFile.getName();

                        theOutput.println("<li class='zoneLibraryListItem_" + i + "'>");
                        if (aFile.isDirectory()) {
                            try {
                                theOutput.println("<a href='javascript:mediaLibrary_LoadDirectory(&quot;"
                                        + URLEncoder.encode(tempFilePathStr, "UTF-8") + "&quot;);'>"
                                        + tempFileNameStr + "</a>");
                            } catch (UnsupportedEncodingException ex) {
                                Logger.getLogger(ZoneLibrary.class.getName()).log(Level.WARNING, null, ex);
                            }
                        } else {
                            try {
                                theOutput.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                        + URLEncoder.encode(tempFilePathStr, "UTF-8") + "&quot;);'>"
                                        + tempFileNameStr + "</a>");
                            } catch (UnsupportedEncodingException ex) {
                                Logger.getLogger(ZoneLibrary.class.getName()).log(Level.WARNING, null, ex);
                            }
                        }
                        theOutput.println("</li>");
                    }

                    i++;
                }
                return (i + 1);
            }
        }
    }

    /**
     * GET request handler for the ZoneLibrary
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later

        String aPageStr = req.getParameter("page");
        int aPageInt = 0;
        if ((aPageStr != null) && (!aPageStr.equals(""))) {
            aPageInt = Integer.valueOf(aPageStr);
        }
        int aNextPageInt = aPageInt + 1;
        int startIndexInt = aPageInt * mediaPerPage;
        int endIndexInt = ((aPageInt + 1) * mediaPerPage) - 1;

        out.println("<div id='zoneLibraryPage' data-role='page' data-theme='d'>"
                + "<div data-role='header' data-theme='b' data-position='fixed'>"
                + "<a href='javascript:libraryAddPage_Load();' data-role='button' data-icon='plus'>Add</a>"
                + "<h1>Media Library</h1>");

        String aPathStr = req.getParameter("path");
        if ((aPathStr != null) && (!aPathStr.equals(""))) { //make sure this contains a path
            aPathStr = URLDecoder.decode(aPathStr, "UTF-8");

            out.println("<a id='mL_LDP' href='javascript:mediaLibrary_LoadDirectoryPage(&quot;"
                    + aPathStr + "&quot;, " + aNextPageInt + ");' data-role='button' data-icon='grid'>More</a>");

            out.println("</div>" //end header
                    + "<div data-role='content'>"); //and start content

            out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");
            int aFileDirectoryCount = mediaElementsBrowseOutput(aPathStr, startIndexInt, endIndexInt, out);
            out.println("</ul>");
        } else if ((req.getParameter("type") != null)
                && (!req.getParameter("type").equals(""))) { //keywords search
            //get search parameters
            boolean searchMatchAllKeywords = false; //default to any keyword matching (OR)
            String searchTypeStr = MediaSearchType.any.toString(); //default to any keyword matching (OR)
            if (req.getParameter("type").equals(MediaSearchType.all.toString())) {
                searchTypeStr = MediaSearchType.all.toString();
            }
            String rawSearchKeywordsStr = req.getParameter("keywords");
            if (rawSearchKeywordsStr == null) {
                rawSearchKeywordsStr = "";
            }

            //format keywords to properly work with search
            rawSearchKeywordsStr = rawSearchKeywordsStr.replace(",", " ");
            while (rawSearchKeywordsStr.contains("  ")) {
                rawSearchKeywordsStr = rawSearchKeywordsStr.replace("  ", " ");
            }
            rawSearchKeywordsStr = rawSearchKeywordsStr.toLowerCase(Locale.ENGLISH);
            String[] searchKeywordArray = rawSearchKeywordsStr.split(" ");

            //output end of header and start of content
            out.println("<a href='javascript:librarySearchPage_SearchMore(&quot;"
                    + searchTypeStr + "&quot;, &quot;" + rawSearchKeywordsStr
                    + "&quot;, " + aNextPageInt + ");' data-role='button' data-icon='grid'>More</a>");

            out.println("</div>" //end header
                    + "<div data-role='content'>"); //and start content

            //get search results from library index
            out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");
            HashMap<String, String> outputFilesMap = ZoneLibraryIndex.getInstance().getFiles(
                    searchKeywordArray, searchMatchAllKeywords, startIndexInt, endIndexInt); //<full file path, filename>
            if (outputFilesMap.size() > 0) {
                int i = 0;
                for (String aTempFullFilePath : outputFilesMap.keySet()) {
                    out.println("<li class='zoneLibraryListItem_" + i + "'>");
                    out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                            + aTempFullFilePath + "&quot;);'>"
                            + outputFilesMap.get(aTempFullFilePath) + "</a>");
                    out.println("</li>");
                    i++;
                }
            }
            out.println("</ul>");
        } else { //no path or type set, load various base directories from saved preferences
            out.println("<a href='javascript:librarySearchPage_Load();'"
                    + "data-role='button' data-icon='search'>Search</a>");

            out.println("</div>" //end header
                    + "<div data-role='content'>"); //and start content

            out.println("<ul id='zoneLibraryList' data-role='listview' data-split-icon='delete' data-split-theme='d'>");
            List<String> rootPathStrList = ZoneServerUtility.getInstance().getMediaDirEntries();
            if (rootPathStrList.size() > 0) {
                int i = 0;
                for (String rootPathStr : rootPathStrList) {
                    //pre-process strings from db
                    System.out.println("from db: " + rootPathStr);
                    String rootMediaNameStr = rootPathStr;
                    if (rootPathStr.contains(mediaNameSplitStr)) {
                        String[] rootPathStrArray = rootPathStr.split(mediaNameSplitStr);
                        rootMediaNameStr = rootPathStrArray[0];
                        System.out.println("rootMediaNameStr=" + rootMediaNameStr);
                        if (!rootPathStr.contains(FileSystemType.radio.toString().concat(prefixUriStr))) {
                            rootPathStr = rootPathStrArray[1];
                        }
                        System.out.println("rootPathStr=" + rootPathStr);
                    }

                    //output root list elements
                    if (rootPathStr.contains(FileSystemType.radio.toString().concat(prefixUriStr))) {
                        out.println("<li id='zoneLibraryListItem_" + i + "'>");
                        out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                + URLEncoder.encode(rootPathStr, "UTF-8") + "&quot;);'>" + rootMediaNameStr + "</a>"
                                + "<a href='javascript:mediaLibrary_removeMediaRoot(&quot;zoneLibraryListItem_"
                                + i + "&quot;);' data-role='button' data-icon='delete'>Remove</a>");
                        out.println("</li>");
                    } else {
                        out.println("<li id='zoneLibraryListItem_" + i + "'>");
                        out.println("<a href='javascript:mediaLibrary_LoadDirectory(&quot;"
                                + URLEncoder.encode(rootPathStr, "UTF-8") + "&quot;);'>" + rootMediaNameStr + "</a>"
                                + "<a href='javascript:mediaLibrary_removeMediaRoot(&quot;zoneLibraryListItem_"
                                + i + "&quot;);' data-role='button' data-icon='delete'>Remove</a>");
                        out.println("</li>");
                    }
                    i++;
                }
            }
            out.println("</ul>");
        }

        out.println("</div>" //close content
                + "<div data-id='mainNavFooter' data-role='footer' data-position='fixed'>"
                + "<div data-role='navbar'>"
                + "<ul>"
                + "<li><a href='javascript:zoneSelection_Load();'>Zone Selection</a></li>"
                + "<li><a href='javascript:playList_Load();'>Now Playing</a></li>"
                + "<li><a href='javascript:mediaLibrary_Load();' class='ui-btn-active ui-state-persist'>Media Library</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</div>");
    }

    /**
     * allows for adding and removing media root paths via POST requests
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String opt = req.getParameter("opt"); //what are we doing?
        if ((opt != null) && (!opt.equals(""))) { //non-empty
            if (opt.equals("addMediaRoot")) {
                FileSystemType newServerType = FileSystemType.valueOf(req.getParameter("newServerType"));
                String newMediaName = req.getParameter("newMediaName");
                String newServerAddress = req.getParameter("newServerAddress");
                String newFilePath = req.getParameter("newFilePath");
                if ((newServerType != null) && (newMediaName != null) && (!newMediaName.equals(""))
                        && (newServerAddress != null) && (!newServerAddress.equals(""))
                        && (newFilePath != null) && (!newFilePath.equals(""))) { //all variables are set
                    String newRootMediaEntryStr = ZoneServerUtility.getInstance().updateMediaDirEntry(newServerType, newMediaName, newServerAddress, newFilePath);
                    resp.getWriter().println("added root media entry: " + newRootMediaEntryStr);
                }
            } else if (opt.equals("removeMediaRoot")) {
                String aRootIndexStr = req.getParameter("index");
                if ((aRootIndexStr != null) && (!aRootIndexStr.equals(""))) {
                    int aRootIndexInt = Integer.valueOf(aRootIndexStr);
                    ZoneServerUtility.getInstance().removeMediaDirEntry(aRootIndexInt);
                    resp.getWriter().println("removed root media entry #" + String.valueOf(aRootIndexInt));
                }
            }
        }
    }
}
