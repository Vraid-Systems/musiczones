/*
 * servlet that allows for management and retrieval of the internal playlist
 */
package servlets;

import contrib.VLCMediaPlayer;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import zoneserver.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class ZonePlaylistInterface extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later
        String opt = req.getParameter("opt"); //what are we doing?

        if ((opt == null) || opt.equals("")) { //do nothing to playlist
        } else if (opt.equals("shuffle")) { //shuffle the playlist
            VLCMediaPlayer.getInstance().shufflePlayList();
        } else if (opt.equals("remove")) {
            String index = req.getParameter("index");
            if ((index != null) && (!index.equals(""))) {
                VLCMediaPlayer.getInstance().removeIndex(Integer.valueOf(index));
            }
        }

        int i = 0;
        for (String aMediaUrlStr : VLCMediaPlayer.getInstance().getPlayList()) {
            out.println("<li id='playlistitem_" + i + "'><a href='" + aMediaUrlStr + "'>"
                    + ZoneServerUtility.getInstance().getFileNameFromUrlStr(aMediaUrlStr)
                    + "</a><a href='javascript:removePlayListItem('#playlistitem_"
                    + i + "');' data-role='button' data-icon='delete'>Remove</a></li>");
            i++;
        }
    }
}
