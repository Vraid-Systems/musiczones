/*
 * provides Jetty 6.1.26 embedding bindings and control
 * 
 * THIS IS A SINGLETON IMPLEMENTATION see:
 * http://www.javaworld.com/javaworld/jw-04-2003/jw-0425-designpatterns.html
 */
package contrib;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import multicastmusiccontroller.ProgramConstants;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.core.io.ClassPathResource;
import servlets.ZoneLibrary;
import servlets.ZonePlaylist;
import servlets.ZoneSelection;
import servlets.ZoneAddMedia;

/**
 * @author Mort Bay Consulting / Codehaus / Eclipse
 * @see http://docs.codehaus.org/display/JETTY/Embedding+Jetty#EmbeddingJetty-QuickStartServletsand.jsppages%28hostedwithinthesame.jarastheembedder%29
 */
public class JettyWebServer implements ProgramConstants {

    private static JettyWebServer jws_SingleInstance = null;
    protected Server jws_serverInstance = null;
    protected int jws_serverPortInt = 80;

    protected JettyWebServer(int theServerPortInt) {
        jws_serverInstance = new Server(theServerPortInt);
        jws_serverPortInt = theServerPortInt;

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setServer(jws_serverInstance);
        webAppContext.setContextPath(webAppContextPathStr);
        try {
            webAppContext.setResourceBase(new ClassPathResource(webAppDirStr).getURL().toString());
        } catch (IOException ex) {
            Logger.getLogger(JettyWebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        webAppContext.addServlet(new ServletHolder(new ZoneSelection()), "/servlets/list-zones");
        webAppContext.addServlet(new ServletHolder(new ZonePlaylist()), "/servlets/playlist");
        webAppContext.addServlet(new ServletHolder(new ZoneLibrary()), "/servlets/library");
        webAppContext.addServlet(new ServletHolder(new ZoneAddMedia()), "/servlets/library-add-dialog");
        jws_serverInstance.addHandler(webAppContext);
    }

    public static JettyWebServer getInstance() {
        if (jws_SingleInstance == null) {
            jws_SingleInstance = new JettyWebServer(80);
        }
        return jws_SingleInstance;
    }

    public static JettyWebServer getInstance(int theServerPortInt) {
        if (jws_SingleInstance == null) {
            jws_SingleInstance = new JettyWebServer(theServerPortInt);
        }
        return jws_SingleInstance;
    }

    public int getServerPortInt() {
        return jws_serverPortInt;
    }

    public void startServer() {
        try {
            jws_serverInstance.start();
        } catch (Exception ex) {
            Logger.getLogger(JettyWebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stopServer() {
        try {
            jws_serverInstance.stop();
        } catch (Exception ex) {
            Logger.getLogger(JettyWebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
