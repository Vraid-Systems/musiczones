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

        int aCurrentPlayListIndex = VLCMediaPlayer.getInstance().getCurrentIndex();

        if ((opt == null) || opt.equals("")) { //do nothing to playlist
        } else if (opt.equals("shuffle")) { //shuffle the playlist
            VLCMediaPlayer.getInstance().shufflePlayList();
        } else if (opt.equals("remove") || opt.equals("play")) {
            String indexStr = req.getParameter("index");
            if ((indexStr != null) && (!indexStr.equals(""))) {
                int indexInt = Integer.valueOf(indexStr);
                if (opt.equals("remove")) {
                    VLCMediaPlayer.getInstance().removeIndex(indexInt);
                } else if (opt.equals("toggle")) {
                    if (aCurrentPlayListIndex == indexInt) {
                        if (VLCMediaPlayer.getInstance().isPlaying()) {
                            VLCMediaPlayer.getInstance().pause();
                        } else {
                            VLCMediaPlayer.getInstance().play();
                        }
                    } else {
                        VLCMediaPlayer.getInstance().playIndex(indexInt);
                    }
                }
            }
        }

        aCurrentPlayListIndex = VLCMediaPlayer.getInstance().getCurrentIndex();

        if (VLCMediaPlayer.getInstance().getPlayList().size() > 0) {
            int i = 0;
            for (String aMediaUrlStr : VLCMediaPlayer.getInstance().getPlayList()) {
                out.println("<li id='playlistitem_" + i + "'>");
                out.println("<a href='javascript:togglePlayListItem(&quot;#playlistitem_" + i + "&quot;);'>");
                if (i == aCurrentPlayListIndex) {
                    out.println("<img src='/speaker.png' class='ui-li-icon' />");
                }
                out.println(ZoneServerUtility.getInstance().getFileNameFromUrlStr(aMediaUrlStr)
                        + "</a><a href='javascript:removePlayListItem(&quot;#playlistitem_"
                        + i + "&quot;);' data-role='button' data-icon='delete'>Remove</a></li>");
                i++;
            }
        }
    }
}
