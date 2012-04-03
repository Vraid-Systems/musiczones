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

/**
 * @author Jason Zerbe
 */
public class ZoneSearchMedia_DialogPage extends HttpServlet {

    private static final long serialVersionUID = 42L;

    public ZoneSearchMedia_DialogPage() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later

        out.println("<div id='zoneSearchMediaPage' data-role='page' data-theme='d'>");

        out.println("<div data-role='header' data-theme='b'>");
        out.println("<a href='javascript:goBack();' data-role='button' data-icon='back'>Back</a>");
        out.println("<h1>Search Media</h1> ");
        out.println("<a href='javascript:librarySearchPage_Search();' data-role='button' data-icon='search'>Search</a>");
        out.println("</div>");

        out.println("<div data-role='content'>");

        out.println("<div data-role='fieldcontain' id='searchKeywordsContain'>"
                + "<label for='searchKeywords'>Keywords:</label>"
                + "<input type='text' name='searchKeywords' id='searchKeywords' placeholder='Filename Keyword' /></div>");

        out.println("<div data-role='fieldcontain' id='searchTypeContain'>"
                + "<label for='searchType'>Match:</label>"
                + "<select name='searchType' id='searchType' data-native-menu='false'>"
                + "<option value='" + ZoneSearchMedia_SearchType.all.toString() + "'>All Keywords</option>"
                + "<option value='" + ZoneSearchMedia_SearchType.any.toString() + "'>Any Keywords</option>"
                + "</select></div>");

        out.println("</div>"); //end page content

        out.println("</div>"); //end page
    }
}
