/*
 * page for adding network media
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
public class ZoneAddMedia extends HttpServlet implements ProgramConstants {

    public ZoneAddMedia() {
    }

    /**
     * get request handler for displaying add network media dialog page
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later

        out.println("<div id='zoneAddMediaPage' data-role='page' data-theme='d'>");

        out.println("<div data-role='header' data-theme='b' data-position='fixed'>");
        out.println("<a href='javascript:mediaLibrary_Load();' data-icon='delete'>Cancel</a>");
        out.println("<h1>Add Media</h1> ");
        out.println("<a href='javascript:mediaLibrary_addMediaRoot();' data-icon='check'>Save</a>");
        out.println("</div>");

        out.println("<div data-role='content'>");

        out.println("<form id='zoneAddMediaPageForm' name='zoneAddMediaPageForm'>");
        out.println("<div data-role='fieldcontain' id='newServerTypeContain'>"
                + "<label for='newServerType'>Choose Server Type:</label>"
                + "<select name='newServerType' id='newServerType'"
                + "data-native-menu='false' onchange='libraryAddPage_changeSelectHandler(this.id);'>"
                + "<option value='" + FileSystemType.smb.toString() + "'>Windows Share</option>"
                + "<option value='" + FileSystemType.file.toString() + "'>Local Filesytem</option>"
                + "</select></div>");
        out.println("<div data-role='fieldcontain' id='newServerAddressContain'>"
                + "<label for='newServerAddress'>Server Address:</label>"
                + "<input type='text' name='newServerAddress' id='newServerAddress' placeholder='myserver' /></div>");
        out.println("<div data-role='fieldcontain' id='newFilePathContain'>"
                + "<label for='newFilePath'>Root Path of Media:</label>"
                + "<input type='text' name='newFilePath' id='newFilePath' placeholder='/path/to/media/' /></div>");
        out.println("</form>");

        out.println("</div>"); //end page content

        out.println("</div>"); //end page
    }
}
