/*
 * servlet that lists off all the available zone controllers in a generated
 * jquerymobile dialog page
 * 
 * based on: http://jquerymobile.com/demos/1.0a4.1/docs/pages/dialog.html
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import zoneserver.ZoneServerLogic;

/**
 * @author Jason Zerbe
 */
public class ZoneControllerListDialog extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //get the data necessary
        HashMap<String, String> global_NodeInfoMap = ZoneServerLogic.getInstance().getNodeInfoMap();
        HashMap<String, String> global_NodeDashBoardMap = ZoneServerLogic.getInstance().getNodeDashBoardMap();

        //setup the response
        PrintWriter out = resp.getWriter();

        //build the zone selection page
        String aPageContentPartTopStr = "<div id='playListPage' data-role='page' data-theme='d'>"
                + "<div data-role='header' data-theme='b' data-position='fixed'>"
                + "<a href='javascript:selectZonePage();' data-role='button' data-icon='grid'>Select</a>"
                + "<h1>Zone Selection</h1>"
                + "<a href='javascript:settingsPage();' data-role='button' data-icon='gear'>Settings</a>"
                + "</div>"
                + "<div data-role='content'>"
                + "<ul data-role='listview'>\n";
        String aPageContentPartListStr = "";
        for (String aNodeUUIDStr : global_NodeInfoMap.keySet()) {
            String aNodeDashBoardUrlStr = global_NodeDashBoardMap.get(aNodeUUIDStr);
            String aNodeName = global_NodeInfoMap.get(aNodeUUIDStr);
            aPageContentPartListStr += "<li><a href='" + aNodeDashBoardUrlStr + "'>" + aNodeName + "</a></li>\n";
        }
        String aPageContentPartBottomStr = "</ul>\n"
                + "</div>"
                + "<div data-id='mainNavFooter' data-role='footer' data-position='fixed'>"
                + "<div data-role='navbar'>"
                + "<ul>"
                + "<li><a href='javascript:shuffleNowPlaying();'>Shuffle</a></li>"
                + "<li><a href='#playListPage'>Now Playing</a></li>"
                + "<li><a href='#libraryPage'>Library</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</div>";
        String aPageContentStr = aPageContentPartTopStr + aPageContentPartListStr + aPageContentPartBottomStr;

        //and output it
        out.write(aPageContentStr);
    }
}
