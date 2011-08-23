/*
 * servlet that lists off all the available zone controllers
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
public class ZoneSelectionPage extends HttpServlet {

    public ZoneSelectionPage() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //get the data necessary
        HashMap<String, String> zcld_ZoneInfoMap = ZoneServerLogic.getInstance().getNodeInfoMap();
        HashMap<String, String> zcld_ZoneDashBoardMap = ZoneServerLogic.getInstance().getNodeDashBoardMap();

        //setup the response
        PrintWriter out = resp.getWriter();

        //build the zone selection page
        String aPageContentPartTopStr = "<div id='selectZonePage' data-role='page' data-theme='d'>"
                + "<div data-role='header' data-theme='b' data-position='fixed'>"
                + "<a href='javascript:shuffleNowPlaying();' data-role='button' data-icon='grid'>Shuffle</a>"
                + "<h1>Zone Selection</h1>"
                + "<a href='javascript:settingsPage();' data-role='button' data-icon='gear'>Settings</a>"
                + "</div>"
                + "<div data-role='content'>"
                + "<ul data-role='listview'>\n";
        String aPageContentPartListStr = "";
        for (String aNodeUUIDStr : zcld_ZoneInfoMap.keySet()) {
            String aNodeDashBoardUrlStr = zcld_ZoneDashBoardMap.get(aNodeUUIDStr);
            String aNodeName = zcld_ZoneInfoMap.get(aNodeUUIDStr);
            aPageContentPartListStr += "<li><a href='" + aNodeDashBoardUrlStr + "'>" + aNodeName + "</a></li>\n";
        }
        String aPageContentPartBottomStr = "</ul>\n"
                + "</div>"
                + "<div data-id='mainNavFooter' data-role='footer' data-position='fixed'>"
                + "<div data-role='navbar'>"
                + "<ul>"
                + "<li><a href='javascript:selectZonePage();' class='ui-btn-active ui-state-persist'>Zone Selection</a></li>"
                + "<li><a href='#playListPage'>Now Playing</a></li>"
                + "<li><a href='javascript:loadLibrary();'>Library</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</div>";
        String aPageContentStr = aPageContentPartTopStr + aPageContentPartListStr + aPageContentPartBottomStr;

        //and output it
        out.write(aPageContentStr);
    }
}
