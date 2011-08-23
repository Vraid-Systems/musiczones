/*
 * bridge between frontend jquerymobile and backend java prefrences for
 * managing current zone settings
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import multicastmusiccontroller.ProgramConstants;
import zoneserver.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class ZoneSettings extends HttpServlet implements ProgramConstants {

    public ZoneSettings() {
    }

    /**
     * get request handler for piecemeal retrieval of settings
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later
        String opt = req.getParameter("opt"); //what are we doing?

        if ((opt == null) || (opt.equals(""))) { //no settings specified
        }

        out.println("<div id='zoneSettings' data-role='page' data-theme='d'>");

        out.println("<div data-role='header' data-theme='b' data-position='fixed'>");
        out.println("<a href='#playListPage' data-icon='delete' data-rel='back'>Cancel</a>");
        out.println("<h1>Zone Settings</h1> ");
        out.println("<a href='javascript:addNewMediaRoot();' data-icon='check'>Save</a>");
        out.println("</div>");

        out.println("<div data-role='content'>");

        //old root media path list/delete
        out.println("<ul id='zoneRootPathList' data-role='listview' data-split-icon='delete' data-split-theme='d'>");
        List<String> rootPathStrList = ZoneServerUtility.getInstance().getMediaDirEntries();
        if (rootPathStrList.size() > 0) {
            int i = 0;
            for (String rootPathStr : rootPathStrList) {
                out.println("<li id='rootpathitem_" + i + "'>");
                out.println("<a href='javascript:browseDirectory(&quot;" + rootPathStr + "&quot;);'>");
                out.println(rootPathStr + "</a><a href='javascript:removeMediaRootPath(&quot;#rootpathitem_"
                        + i + "&quot;);' data-role='button' data-icon='delete'>Remove</a></li>");
                i++;
            }
        }
        out.println("</ul>");

        //new root media path intput form
        out.println("<form id='settingsForm' name='settingsForm'>");
        out.println("<div data-role='fieldcontain'>"
                + "<label for='newServerType'>Choose Server Type:</label>"
                + "<select name='newServerType' id='newServerType'>"
                + "<option value='" + ServerType.smb.toString() + "'>Windows Share</option>"
                + "</select></div>");
        out.println("<div data-role='fieldcontain'>"
                + "<label for='newServerAddress'>Server Address:</label>"
                + "<input type='text' name='newServerAddress' id='newServerAddress' placeholder='myserver' /></div>");
        out.println("<div data-role='fieldcontain'>"
                + "<label for='newFilePath'>Root Path of Media:</label>"
                + "<input type='text' name='newFilePath' id='newFilePath' placeholder='/path/to/media/' /></div>");
        out.println("</form>");

        out.println("</div>"); //end page content

        out.println("</div>"); //end page
    }

    /**
     * post request handler for updating certain settings
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String opt = req.getParameter("opt"); //what are we doing?

        if ((opt == null) || (opt.equals(""))) { //do nothing to settings
        } else if (opt.equals("add")) {
            ServerType newServerType = ServerType.valueOf(req.getParameter("newServerType"));
            String newServerAddress = req.getParameter("newServerAddress");
            String newFilePath = req.getParameter("newFilePath");
            if ((newServerType != null) && (newServerAddress != null)
                    && (!newServerAddress.equals("")) && (newFilePath != null)
                    && (!newFilePath.equals(""))) { //all variables are set
                ZoneServerUtility.getInstance().updateMediaDirEntry(newServerType, newServerAddress, newFilePath);
            }
        } else if (opt.equals("remove")) {
            String aRootIndexStr = req.getParameter("index");
            if ((aRootIndexStr != null) && (!aRootIndexStr.equals(""))) {
                int aRootIndexInt = Integer.valueOf(aRootIndexStr);
                ZoneServerUtility.getInstance().removeMediaDirEntry(aRootIndexInt);
            }
        }
    }
}
