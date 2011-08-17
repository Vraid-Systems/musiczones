/*
 * provides Jetty 6.1.26 embedding bindings and control
 */
package contrib;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import multicastmusiccontroller.ProgramConstants;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.core.io.ClassPathResource;
import servlets.ZoneControllerListDialog;

/**
 * @author Mort Bay Consulting / Codehaus / Eclipse
 * @see http://docs.codehaus.org/display/JETTY/Embedding+Jetty#EmbeddingJetty-QuickStartServletsand.jsppages%28hostedwithinthesame.jarastheembedder%29
 */
public class JettyWebServer implements ProgramConstants {

    protected Server serverInstance = null;

    public JettyWebServer(int theWebServerPort) {
        serverInstance = new Server(theWebServerPort);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setServer(serverInstance);
        webAppContext.setContextPath(webAppContextPathStr);
        try {
            webAppContext.setResourceBase(new ClassPathResource(webAppDirStr).getURL().toString());
        } catch (IOException ex) {
            Logger.getLogger(JettyWebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        serverInstance.addHandler(webAppContext);

        Context context = new Context(serverInstance, "/servlets", Context.SESSIONS);
        context.addServlet(new ServletHolder(new ZoneControllerListDialog()), "/list-zones-html");
    }

    public boolean startServer() {
        try {
            serverInstance.start();
        } catch (Exception ex) {
            Logger.getLogger(JettyWebServer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public boolean stopServer() {
        try {
            serverInstance.stop();
        } catch (Exception ex) {
            Logger.getLogger(JettyWebServer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
}
