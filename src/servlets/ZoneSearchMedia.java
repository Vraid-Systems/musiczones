/*
 * servlet class that generates the search dialog page for searching the media library
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import multicastmusiccontroller.ProgramConstants;

/**
 * @author Jason Zerbe
 */
public class ZoneSearchMedia extends HttpServlet implements ProgramConstants {

    public ZoneSearchMedia() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later

        out.println("<div id='zoneSearchMediaPage' data-role='page' data-theme='d'>");

        out.println("<div data-role='header' data-theme='b' data-position='fixed'>");
        out.println("<a href='javascript:mediaLibrary_Load();' data-icon='delete'>Cancel</a>");
        out.println("<h1>Search Media</h1> ");
        out.println("<a href='javascript:librarySearchPage_Search();' data-icon='search'>Search</a>");
        out.println("</div>");

        out.println("<div data-role='content'>");

        out.println("<form id='zoneSearchMediaForm' name='zoneSearchMediaForm'>");

        out.println("<div data-role='fieldcontain' id='searchKeywordsContain'>"
                + "<label for='searchKeywords'>Search Keywords:</label>"
                + "<input type='text' name='searchKeywords' id='searchKeywords' placeholder='Filename Keyword' /></div>");

        out.println("<div data-role='fieldcontain' id='searchTypeContain'>"
                + "<label for='searchType'>Choose Keyword Matching:</label>"
                + "<select name='searchType' id='searchType' data-native-menu='false'>"
                + "<option value='" + MediaSearchType.all.toString() + "'>All Keywords</option>"
                + "<option value='" + MediaSearchType.any.toString() + "'>Any Keywords</option>"
                + "</select></div>");

        out.println("</form>");

        out.println("</div>"); //end page content

        out.println("</div>"); //end page
    }
}
