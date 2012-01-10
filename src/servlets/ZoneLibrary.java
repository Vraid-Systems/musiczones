/*
 * servlet for displaying indexed media
 */
package servlets;

import contrib.MediaPlayer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import musiczones.ID3FieldList;
import musiczones.MediaSearchType;
import musiczones.MusicZones;
import musiczones.ZoneLibraryIndex;

/**
 * @author Jason Zerbe
 */
public class ZoneLibrary extends HttpServlet {

    private static final long serialVersionUID = 42L;
    private static final int mediaItemsPerPage = 30;
    private static final String kListParamStr = "list";
    private static final String kFilterParamStr = "filter";
    public static final String kQuoteReplaceStr = "!!quote!!";

    public ZoneLibrary() {
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
        int startIndexInt = aPageInt * mediaItemsPerPage;
        int endIndexInt = ((aPageInt + 1) * mediaItemsPerPage) - 1;

        out.println("<div id='zoneLibraryPage' data-role='page' data-theme='d'>"
                + "<div data-role='header' data-theme='b' data-position='fixed'>"
                + "<a href='javascript:librarySearchPage_Load();' "
                + "data-role='button' data-icon='search'>Search</a>"
                + "<h1>Library</h1>");

        if ((req.getParameter("type") != null)
                && (!req.getParameter("type").equals(""))) { //keywords search
            //get search parameters
            boolean searchMatchAllKeywords = false; //default to any keyword matching (OR)
            String searchTypeStr = MediaSearchType.any.toString(); //default to any keyword matching (OR)
            if (req.getParameter("type").equals(MediaSearchType.all.toString())) {
                searchTypeStr = MediaSearchType.all.toString();
                searchMatchAllKeywords = true;
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
                    + "&quot;, " + aNextPageInt + ");' data-role='button' data-icon='plus'>More</a>");

            out.println("</div>" //end header
                    + "<div data-role='content'>"); //and start content

            //get search results from library index
            out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");
            TreeMap<String, String> outputFilesMap = ZoneLibraryIndex.getInstance().getFiles(
                    searchKeywordArray, searchMatchAllKeywords, startIndexInt, endIndexInt); //<filename, full file path>
            if (outputFilesMap.size() > 0) {
                int i = 0;
                for (String aTempFileName : outputFilesMap.keySet()) {
                    out.println("<li id='zoneLibraryListItem_" + i + "'>");
                    out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                            + outputFilesMap.get(aTempFileName) + "&quot;);'>"
                            + aTempFileName + "</a>");
                    out.println("</li>");
                    i++;
                }
            }
            out.println("</ul>");
        } else if ((req.getParameter(kListParamStr) != null)
                && (!req.getParameter(kListParamStr).equals(""))) { //sort on ID3 param
            if (req.getParameter(kListParamStr).equals(ID3FieldList.Album.toString())) {
                out.println("</div>" //end header
                        + "<div data-role='content'>"); //and start content

                if ((req.getParameter(kFilterParamStr) != null) && (!req.getParameter(kFilterParamStr).equals(""))) {
                    out.println("<ul id='zoneLibraryList' data-role='listview' data-inset='true' data-filter='true' data-theme='d'>");

                    if (req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'").length() == 1) { //get first char match
                        TreeMap<String, LinkedList<String>> outputAlbumMap = ZoneLibraryIndex.getInstance().getAlbumMap();
                        if (outputAlbumMap.size() > 0) {
                            String aFirstCharStr = req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'").substring(0, 1);
                            int aFirstChar = aFirstCharStr.charAt(0);
                            String aNextChar = String.valueOf((char) (aFirstChar + 1));
                            SortedMap<String, LinkedList<String>> aSubMap = outputAlbumMap.subMap(
                                    aFirstCharStr, aNextChar);
                            if (aSubMap.size() > 0) {
                                int i = 0;
                                for (String aTempAlbumName : aSubMap.keySet()) {
                                    out.println("<li id='zoneLibraryListItem_" + i + "'>");
                                    out.println("<a href='javascript:mediaLibrary_SubList(&quot;"
                                            + ID3FieldList.Album.toString() + "&quot;, "
                                            + "&quot;" + aTempAlbumName.replace("'", kQuoteReplaceStr) + "&quot;);'>"
                                            + aTempAlbumName + "</a>");
                                    out.println("</li>");
                                    i++;
                                }
                            }
                        }
                    } else { //get complete album match
                        TreeMap<String, String> outputAlbumMap = ZoneLibraryIndex.getInstance().getTitlesFromAlbum(req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'"));
                        if (outputAlbumMap.size() > 0) {
                            int i = 0;
                            for (String aTempAlbum : outputAlbumMap.keySet()) {
                                out.println("<li id='zoneLibraryListItem_" + i + "'>");
                                out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                        + outputAlbumMap.get(aTempAlbum) + "&quot;);'>"
                                        + aTempAlbum + "</a>");
                                out.println("</li>");
                                i++;
                            }
                        }
                    }
                } else { //dump out first chars
                    out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");

                    TreeMap<String, LinkedList<String>> outputAlbumMap = ZoneLibraryIndex.getInstance().getAlbumMap();

                    String aFirstCharStr = "";
                    int i = 0;
                    for (String aTempAlbum : outputAlbumMap.keySet()) {
                        String aNewFirstCharStr = aTempAlbum.substring(0, 1);
                        if (aFirstCharStr.equals(aNewFirstCharStr)) {
                            continue;
                        } else {
                            aFirstCharStr = aNewFirstCharStr;
                            out.println("<li id='zoneLibraryListItem_" + i + "'>");
                            out.println("<a href='javascript:mediaLibrary_SubList(&quot;"
                                    + ID3FieldList.Album.toString() + "&quot;, "
                                    + "&quot;" + aFirstCharStr.replace("'", kQuoteReplaceStr) + "&quot;);'>"
                                    + aFirstCharStr + "</a>");
                            out.println("</li>");
                            i++;
                        }
                    }
                }
            } else if (req.getParameter(kListParamStr).equals(ID3FieldList.Artist.toString())) {
                out.println("</div>" //end header
                        + "<div data-role='content'>"); //and start content

                if ((req.getParameter(kFilterParamStr) != null) && (!req.getParameter(kFilterParamStr).equals(""))) {
                    out.println("<ul id='zoneLibraryList' data-role='listview' data-inset='true' data-filter='true' data-theme='d'>");

                    if (req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'").length() == 1) { //get first char match
                        TreeMap<String, LinkedList<String>> outputArtistMap = ZoneLibraryIndex.getInstance().getArtistMap();
                        if (outputArtistMap.size() > 0) {
                            String aFirstCharStr = req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'").substring(0, 1);
                            int aFirstChar = aFirstCharStr.charAt(0);
                            String aNextChar = String.valueOf((char) (aFirstChar + 1));
                            SortedMap<String, LinkedList<String>> aSubMap = outputArtistMap.subMap(
                                    aFirstCharStr, aNextChar);
                            if (aSubMap.size() > 0) {
                                int i = 0;
                                for (String aTempArtistName : aSubMap.keySet()) {
                                    out.println("<li id='zoneLibraryListItem_" + i + "'>");
                                    out.println("<a href='javascript:mediaLibrary_SubList(&quot;"
                                            + ID3FieldList.Artist.toString() + "&quot;, "
                                            + "&quot;" + aTempArtistName.replace("'", kQuoteReplaceStr) + "&quot;);'>"
                                            + aTempArtistName + "</a>");
                                    out.println("</li>");
                                    i++;
                                }
                            }
                        }
                    } else { //get complete artist match
                        TreeMap<String, String> outputTitlesMap = ZoneLibraryIndex.getInstance().getTitlesFromArtist(req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'"));
                        if (outputTitlesMap.size() > 0) {
                            int i = 0;
                            for (String aTempTitle : outputTitlesMap.keySet()) {
                                out.println("<li id='zoneLibraryListItem_" + i + "'>");
                                out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                        + outputTitlesMap.get(aTempTitle) + "&quot;);'>"
                                        + aTempTitle + "</a>");
                                out.println("</li>");
                                i++;
                            }
                        }
                    }
                } else { //dump out first chars
                    out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");

                    TreeMap<String, LinkedList<String>> outputArtistMap = ZoneLibraryIndex.getInstance().getArtistMap();

                    String aFirstCharStr = "";
                    int i = 0;
                    for (String aTempArtist : outputArtistMap.keySet()) {
                        String aNewFirstCharStr = aTempArtist.substring(0, 1);
                        if (aFirstCharStr.equals(aNewFirstCharStr)) {
                            continue;
                        } else {
                            aFirstCharStr = aNewFirstCharStr;
                            out.println("<li id='zoneLibraryListItem_" + i + "'>");
                            out.println("<a href='javascript:mediaLibrary_SubList(&quot;"
                                    + ID3FieldList.Artist.toString() + "&quot;, "
                                    + "&quot;" + aFirstCharStr.replace("'", kQuoteReplaceStr) + "&quot;);'>"
                                    + aFirstCharStr + "</a>");
                            out.println("</li>");
                            i++;
                        }
                    }
                }
            } else if (req.getParameter(kListParamStr).equals(ID3FieldList.Genre.toString())) {
                out.println("</div>" //end header
                        + "<div data-role='content'>"); //and start content
                out.println("<ul id='zoneLibraryList' data-role='listview' data-inset='true' data-filter='true' data-theme='d'>");

                if ((req.getParameter(kFilterParamStr) != null) && (!req.getParameter(kFilterParamStr).equals(""))) {
                    TreeMap<String, String> outputTitlesMap = ZoneLibraryIndex.getInstance().getTitlesFromGenre(req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'"));
                    if (outputTitlesMap.size() > 0) {
                        int i = 0;
                        for (String aTempTitle : outputTitlesMap.keySet()) {
                            out.println("<li id='zoneLibraryListItem_" + i + "'>");
                            out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                    + outputTitlesMap.get(aTempTitle) + "&quot;);'>"
                                    + aTempTitle + "</a>");
                            out.println("</li>");
                            i++;
                        }
                    }
                } else {
                    TreeMap<String, LinkedList<String>> outputGenreMap = ZoneLibraryIndex.getInstance().getGenreMap();
                    if (outputGenreMap.size() > 0) {
                        int i = 0;
                        for (String aTempGenre : outputGenreMap.keySet()) {
                            out.println("<li id='zoneLibraryListItem_" + i + "'>");
                            out.println("<a href='javascript:mediaLibrary_SubList(&quot;"
                                    + ID3FieldList.Genre.toString() + "&quot;, "
                                    + "&quot;" + aTempGenre.replace("'", kQuoteReplaceStr) + "&quot;);'>"
                                    + aTempGenre + "</a>");
                            out.println("</li>");
                            i++;
                        }
                    }
                }
            } else if (req.getParameter(kListParamStr).equals(ID3FieldList.Random.toString())) { //get a list of 20 random files not in current Now Playing
                out.println("</div>" //end header
                        + "<div data-role='content'>"); //and start content
                out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");

                HashMap<String, String> outputFilesMap = ZoneLibraryIndex.getInstance().getAllFiles(); //<filename, full file path>
                if (outputFilesMap.size() > 0) {
                    Random aRandom = new Random();
                    List<String> aFileNameArray = Arrays.asList(outputFilesMap.keySet().toArray(new String[0]));
                    ArrayList<String> aPreviousPathList = new ArrayList<String>(); //for tracking already output files
                    int i = 0;
                    while (i < 20) {
                        String aRandomFileName = aFileNameArray.get(aRandom.nextInt(aFileNameArray.size()));
                        String aFullPathFromRandomFileName = ZoneLibraryIndex.getInstance().getFullPathFromFileName(aRandomFileName);
                        if (MediaPlayer.getInstance().getPlayList().contains(aFullPathFromRandomFileName)
                                || aPreviousPathList.contains(aFullPathFromRandomFileName)) {
                            continue;
                        } else {
                            aPreviousPathList.add(aFullPathFromRandomFileName);
                            out.println("<li id='zoneLibraryListItem_" + i + "'>");
                            out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                    + aFullPathFromRandomFileName + "&quot;);'>"
                                    + aRandomFileName + "</a>");
                            out.println("</li>");
                            i++;
                        }
                    }
                }
            } else if (req.getParameter(kListParamStr).equals(ID3FieldList.Title.toString())) {
                out.println("</div>" //end header
                        + "<div data-role='content'>"); //and start content

                TreeMap<String, LinkedList<String>> outputAllTitles = ZoneLibraryIndex.getInstance().getAllTitles();

                if ((req.getParameter(kFilterParamStr) != null)
                        && (!req.getParameter(kFilterParamStr).equals(""))) { //dump all titles in single letter range
                    out.println("<ul id='zoneLibraryList' data-role='listview' data-inset='true' data-filter='true' data-theme='d'>");

                    String aFirstCharStr = req.getParameter(kFilterParamStr).replace(kQuoteReplaceStr, "'").substring(0, 1);
                    int aFirstChar = aFirstCharStr.charAt(0);
                    String aNextChar = String.valueOf((char) (aFirstChar + 1));
                    if (outputAllTitles.size() > 0) {
                        SortedMap<String, LinkedList<String>> aSubMap = outputAllTitles.subMap(
                                aFirstCharStr, aNextChar);
                        if (aSubMap.size() > 0) {
                            int i = 0;
                            for (String aTempTitle : aSubMap.keySet()) {
                                for (String aTempFileName : aSubMap.get(aTempTitle)) {
                                    out.println("<li id='zoneLibraryListItem_" + i + "'>");
                                    out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                            + ZoneLibraryIndex.getInstance().getFullPathFromFileName(aTempFileName)
                                            + "&quot;);'>" + aTempTitle + "</a>");
                                    out.println("</li>");
                                    i++;
                                }
                            }
                        }
                    }
                } else { //dump all set alphabet chars
                    out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");
                    String aFirstCharStr = "";
                    int i = 0;
                    for (String aTempTitle : outputAllTitles.keySet()) {
                        String aNewFirstCharStr = aTempTitle.substring(0, 1);
                        if (aFirstCharStr.equals(aNewFirstCharStr)) {
                            continue;
                        } else {
                            aFirstCharStr = aNewFirstCharStr;
                            out.println("<li id='zoneLibraryListItem_" + i + "'>");
                            out.println("<a href='javascript:mediaLibrary_SubList(&quot;"
                                    + ID3FieldList.Title.toString() + "&quot;, "
                                    + "&quot;" + aFirstCharStr.replace("'", kQuoteReplaceStr) + "&quot;);'>"
                                    + aFirstCharStr + "</a>");
                            out.println("</li>");
                            i++;
                        }
                    }
                }
            } else { //ALL
                out.println("<a href='javascript:mediaLibrary_ListMore(&quot;"
                        + ID3FieldList.All.toString() + "&quot;, "
                        + aNextPageInt + ");' data-role='button' "
                        + "data-icon='plus'>More</a>");

                out.println("</div>" //end header
                        + "<div data-role='content'>"); //and start content
                out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");

                HashMap<String, String> outputFilesMap = ZoneLibraryIndex.getInstance().getAllFiles(); //<filename, full file path>
                if (outputFilesMap.size() > 0) {
                    int i = 0;
                    for (String aTempFileName : outputFilesMap.keySet()) {
                        if ((i >= startIndexInt) && (i <= endIndexInt)) {
                            out.println("<li id='zoneLibraryListItem_" + i + "'>");
                            out.println("<a href='javascript:playList_addMediaPath_NoRedir(&quot;"
                                    + outputFilesMap.get(aTempFileName) + "&quot;);'>"
                                    + aTempFileName + "</a>");
                            out.println("</li>");
                        }
                        i++;
                    }
                }
            }
            out.println("</ul>");
        } else { //iOS inspired start page
            if (!ZoneLibraryIndex.getInstance().getIndexIsBuilding()) {
                if ((req.getParameter("rebuild") != null) && (req.getParameter("rebuild").equals("true"))) {
                    ZoneLibraryIndex.getInstance().manualRebuildIndex();
                } else {
                    out.println("<a href='javascript:mediaLibrary_Rebuild();' data-role='button' data-icon='gear'>Rebuild</a>");
                }
            }
            out.println("</div>" //end header
                    + "<div data-role='content'>"); //and start content
            out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");
            if (!MusicZones.getIsLowMem()) {
                out.println("<li><a href='javascript:mediaLibrary_List(&quot;Album&quot;);'>Albums</a></li>");
                out.println("<li><a href='javascript:mediaLibrary_List(&quot;Artist&quot;);'>Artists</a></li>");
                out.println("<li><a href='javascript:mediaLibrary_List(&quot;Genre&quot;);'>Genres</a></li>");
                out.println("<li><a href='javascript:mediaLibrary_List(&quot;Title&quot;);'>Songs</a></li>");
            }
            out.println("<li><a href='javascript:mediaLibrary_List(&quot;Random&quot;);'>20 Random Files</a></li>");
            out.println("<li><a href='javascript:mediaLibrary_List(&quot;All&quot;);'>All Files</a></li>");
            out.println("</ul>");
        }

        out.println("</div>" //close content
                + "<div data-id='mainNavFooter' data-role='footer' data-position='fixed'>"
                + "<div data-role='navbar'>"
                + "<ul>"
                + "<li><a href='javascript:zoneSelection_Load();'>Other Zones</a></li>"
                + "<li><a href='javascript:playList_Load();'>Now Playing</a></li>"
                + "<li><a href='javascript:mediaLibrary_Load();' class='ui-btn-active ui-state-persist'>Library</a></li>"
                + "<li><a href='javascript:radioPage_Load();'>Radio</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</div>");
    }
}
