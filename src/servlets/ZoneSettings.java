/*
 * bridge between frontend jquerymobile and backend java prefrences for
 * managing current zone settings
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
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
            out.println("<div id='zoneSettings' data-role='page' data-theme='d'>");

            out.println("<div data-role='header' data-theme='b' data-position='fixed'>");
            out.println("<a href='#playListPage' data-icon='delete' data-rel='back'>Cancel</a>");
            out.println("<h1>Zone Settings</h1> ");
            out.println("<a href='javascript:document.getElementById(&quot;settingsForm&quot;).submit();' data-icon='check'>Save</a>");
            out.println("</div>");

            out.println("<div data-role='content'>");
            out.println("<form id='settingsForm' name='settingsForm' action='/servlets/settings' method='post'>"
                    + "<input type='hidden' name='opt' value='update' />"
                    + "<div data-role='fieldcontain'>"
                    + "<p>newline seperated list of media directories</p>"
                    + "<textarea cols='30' rows='20' name='directoryList' id='directoryList'>");
            String aMediaDirStr = ZoneServerUtility.getInstance().loadStringPref(prefMediaDirectoryDirKeyStr, "");
            if (!aMediaDirStr.isEmpty()) {
                out.print(aMediaDirStr);
            }
            out.println("</textarea></div></form></div>");

            out.println("</div>");
        }
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
        } else if (opt.equals("update")) {
            String directoryList = req.getParameter("directoryList");
            if ((directoryList != null) || (!directoryList.equals(""))) {
                directoryList = directoryList.replaceAll("\r", "");
                ZoneServerUtility.getInstance().saveStringPref(prefMediaDirectoryDirKeyStr, directoryList);
            }
        }

        resp.sendRedirect("/");
    }
}
