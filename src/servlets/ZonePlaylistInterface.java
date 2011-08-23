/*
 * servlet that allows for management and retrieval of the internal playlist
 */
package servlets;

import contrib.MediaPlayer;
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

    public ZonePlaylistInterface() {
    }

    /**
     * for updating and reloading the playlist items in the same HTTP GET call
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later
        String opt = req.getParameter("opt"); //what are we doing?

        int aCurrentPlayListIndex = MediaPlayer.getInstance().getCurrentIndex();

        if ((opt == null) || opt.equals("")) { //do nothing to playlist
        } else if (opt.equals("shuffle")) { //shuffle the playlist
            MediaPlayer.getInstance().shufflePlayList();
        } else if (opt.equals("remove") || opt.equals("play")) {
            String indexStr = req.getParameter("index");
            if ((indexStr != null) && (!indexStr.equals(""))) {
                int indexInt = Integer.valueOf(indexStr);
                if (opt.equals("remove")) {
                    MediaPlayer.getInstance().removeIndex(indexInt);
                } else if (opt.equals("toggle")) {
                    if (aCurrentPlayListIndex == indexInt) {
                        MediaPlayer.getInstance().togglePlayPause();
                    } else {
                        MediaPlayer.getInstance().playIndex(indexInt);
                    }
                }
            }
        }

        aCurrentPlayListIndex = MediaPlayer.getInstance().getCurrentIndex();

        if (MediaPlayer.getInstance().getPlayList().size() > 0) {
            out.println("<ul id='zonePlayList' data-role='listview' data-split-icon='delete' data-split-theme='d'>");
            int i = 0;
            for (String aMediaUrlStr : MediaPlayer.getInstance().getPlayList()) {
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
            out.println("</ul>");
        }
    }

    /**
     * allow for the posting of data to the playlist storage
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String opt = req.getParameter("opt"); //what are we doing?
        if ((opt != null) && (!opt.equals(""))) { //non-empty
            if (opt.equals("add")) {
                String path = req.getParameter("path");
                if ((path != null) && (!path.equals(""))) {
                    MediaPlayer.getInstance().addMediaUrl(path);
                }
            }
        }
    }
}
