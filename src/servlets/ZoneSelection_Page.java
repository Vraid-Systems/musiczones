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
import zonecontrol.ZoneServerLogic;

/**
 * @author Jason Zerbe
 */
public class ZoneSelection_Page extends HttpServlet {

    private static final long serialVersionUID = 42L;

    public ZoneSelection_Page() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //get the data necessary
        HashMap<String, String> zcld_ZoneInfoMap = ZoneServerLogic.getInstance().getNodeInfoMap();
        HashMap<String, String> zcld_ZoneDashBoardMap = ZoneServerLogic.getInstance().getNodeDashBoardMap();

        //setup the response writer
        PrintWriter out = resp.getWriter();

        //build the zone selection page
        out.println("<div id='zoneSelectionPage' data-role='page' data-theme='d'>"
                + "<div data-role='header' data-theme='b' data-position='fixed'>"
                + "<h1>Zone Selection</h1>"
                + "</div>"
                + "<div data-role='content'>"
                + "<ul data-role='listview' data-inset='true' data-filter='true'>");

        String aPageContentPartListStr = "";
        for (String aNodeUUIDStr : zcld_ZoneInfoMap.keySet()) {
            String aNodeDashBoardUrlStr = zcld_ZoneDashBoardMap.get(aNodeUUIDStr);
            String aNodeName = zcld_ZoneInfoMap.get(aNodeUUIDStr);
            aPageContentPartListStr += "<li><a href='" + aNodeDashBoardUrlStr + "'>" + aNodeName + "</a></li>\n";
        }
        out.print(aPageContentPartListStr);

        out.println("</ul>" //close list
                + "</div>" //close content
                + "<div data-id='mainNavFooter' data-role='footer' data-position='fixed'>"
                + "<div data-role='navbar'>"
                + "<ul>"
                + "<li><a href='javascript:zoneSelection_Load();' class='ui-btn-active ui-state-persist'>Zone Selection</a></li>"
                + "<li><a href='javascript:playList_Load();'>Now Playing</a></li>"
                + "<li><a href='javascript:mediaLibrary_Load();'>Media Library</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</div>");
    }
}
