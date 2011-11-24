/*
 * servlet for displaying indexed media
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import multicastmusiccontroller.MediaSearchType;
import multicastmusiccontroller.ZoneLibraryIndex;

/**
 * @author Jason Zerbe
 */
public class ZoneLibrary extends HttpServlet {

    private static final long serialVersionUID = 42L;
    private static final int mediaItemsPerPage = 30;

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
                + "<h1>Media Library</h1>");

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
        } else { //general browsing - dump EVERATHANG
            //output end of header and start of content
            out.println("<a href='javascript:mediaLibrary_LoadMore("
                    + aNextPageInt + ");' data-role='button' data-icon='grid'>More</a>");

            out.println("</div>" //end header
                    + "<div data-role='content'>"); //and start content

            //get search results from library index
            out.println("<ul id='zoneLibraryList' data-role='listview' data-theme='d'>");
            HashMap<String, String> outputFilesMap = ZoneLibraryIndex.getInstance().getFiles(
                    startIndexInt, endIndexInt); //<full file path, filename>
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
}
