/*
 * manages pulling and displaying Internet radio stations
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Jason Zerbe
 */
public class ZoneRadio extends HttpServlet {

    private static final long serialVersionUID = 42L;

    public ZoneRadio() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //setup the response writer
        PrintWriter out = resp.getWriter();

        //build the zone selection page
        out.println("<div id='zoneSelectionPage' data-role='page' data-theme='d'>"
                + "<div data-role='header' data-theme='b' data-position='fixed'>"
                + "<h1>Radio</h1>"
                + "</div>"
                + "<div data-role='content'>"
                + "<ul data-role='listview'>");

        //build the list elements
        out.println("<li><a href='#holder'>holder</a></li>");

        out.println("</ul>" //close list
                + "</div>" //close content
                + "<div data-id='mainNavFooter' data-role='footer' data-position='fixed'>"
                + "<div data-role='navbar'>"
                + "<ul>"
                + "<li><a href='javascript:zoneSelection_Load();'>Other Zones</a></li>"
                + "<li><a href='javascript:playList_Load();'>Now Playing</a></li>"
                + "<li><a href='javascript:mediaLibrary_Load();'>Library</a></li>"
                + "<li><a href='javascript:radioPage_Load();' class='ui-btn-active ui-state-persist'>Radio</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</div>");
    }
}
