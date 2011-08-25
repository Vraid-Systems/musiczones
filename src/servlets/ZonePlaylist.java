/*
 * servlet that allows for management and retrieval of the internal playlist
 */
package servlets;

import contrib.MediaPlayer;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import zoneserver.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class ZonePlaylist extends HttpServlet {

    public ZonePlaylist() {
    }

    /**
     * for displaying the playlist page
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter(); //get the response writer for later

        out.println("<div id='zonePlaylistPage' data-role='page' data-theme='d'>"
                + "<div data-role='header' data-theme='b' data-position='fixed'>"
                + "<a href='javascript:playList_Stop();' data-role='button' data-icon='delete'>Stop</a>"
                + "<h1>Now Playing</h1>"
                + "<a href='javascript:playList_Shuffle();' data-role='button' data-icon='grid'>Shuffle</a>"
                + "</div>"
                + "<div data-role='content'>");

        int aCurrentPlayListIndex = MediaPlayer.getInstance().getCurrentIndex();

        out.println("<ul id='zonePlaylist' data-role='listview' data-split-theme='d'>");
        if (MediaPlayer.getInstance().getPlayList().size() > 0) {
            int i = 0;
            for (String aMediaUrlStr : MediaPlayer.getInstance().getPlayList()) {
                out.println("<li id='zonePlaylistItem_" + i + "'>");
                out.println("<a href='javascript:playList_ToggleItem(&quot;zonePlaylistItem_" + i + "&quot;);'>");
                if (i == aCurrentPlayListIndex) {
                    out.println("<img src='/speaker.png' class='ui-li-icon' />");
                }
                out.println(ZoneServerUtility.getInstance().getFileNameFromUrlStr(aMediaUrlStr)
                        + "</a><a href='javascript:playList_RemoveItem(&quot;zonePlaylistItem_"
                        + i + "&quot;);' data-role='button' data-icon='delete'>Remove</a></li>");
                i++;
            }
        } else {
            out.println("<li><a href='javascript:mediaLibrary_Load();'>Add some media from the Library!</a>"
                    + "<a href='javascript:mediaLibrary_Load();' data-role='button' data-icon='plus'>Add</a></li>");
        }
        out.println("</ul>");

        out.println("</div>" //close content
                + "<div data-id='mainNavFooter' data-role='footer' data-position='fixed'>"
                + "<div data-role='navbar'>"
                + "<ul>"
                + "<li><a href='javascript:zoneSelection_Load();'>Zone Selection</a></li>"
                + "<li><a href='javascript:playList_Load();' class='ui-btn-active ui-state-persist'>Now Playing</a></li>"
                + "<li><a href='javascript:mediaLibrary_Load();'>Media Library</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</div>");
    }

    /**
     * allow updating of playlist via post requests
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
                    MediaPlayer.getInstance().addMediaUrl(URLDecoder.decode(path, "UTF-8"));
                    resp.getOutputStream().println("added " + path + "to playlist");
                }
            } else if (opt.equals("remove") || opt.equals("toggle")) {
                String indexStr = req.getParameter("index");
                if ((indexStr != null) && (!indexStr.equals(""))) {
                    int indexInt = Integer.valueOf(indexStr);
                    if (opt.equals("remove")) {
                        MediaPlayer.getInstance().removeIndex(indexInt);
                        resp.getOutputStream().println("removed playlist item #" + indexInt);
                    } else if (opt.equals("toggle")) {
                        if (MediaPlayer.getInstance().getCurrentIndex() == indexInt) {
                            MediaPlayer.getInstance().togglePlayPause();
                            resp.getOutputStream().println("playlist item #" + indexInt + " toggled");
                        } else {
                            MediaPlayer.getInstance().playIndex(indexInt);
                            resp.getOutputStream().println("playing playlist item #" + indexInt);
                        }
                    }
                }
            } else if (opt.equals("shuffle")) { //shuffle the playlist
                MediaPlayer.getInstance().shufflePlayList();
                resp.getOutputStream().println("shuffled playlist");
            } else if (opt.equals("stop")) { //stop now playing
                MediaPlayer.getInstance().stop();
                resp.getOutputStream().println("playback stopped");
            }
        }
    }
}
