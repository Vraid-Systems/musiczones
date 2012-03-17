/*
 * provides Jetty 6.1.26 embedding bindings and control
 *
 * THIS IS A SINGLETON IMPLEMENTATION see:
 * http://www.javaworld.com/javaworld/jw-04-2003/jw-0425-designpatterns.html
 */
package contrib;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import servlets.StaticProxy;
import servlets.ZoneLibrary;
import servlets.ZonePlaylist;
import servlets.ZoneRadio;
import servlets.ZoneSelection_Page;
import servlets.ZoneSearchMedia_DialogPage;

/**
 * @author Jason Zerbe
 */
public class JettyWebServer {

	private static JettyWebServer jws_SingleInstance = null;
	protected Server jws_serverInstance = null;
	protected static int jws_serverPortInt = 2320;

	protected JettyWebServer(int theServerPortInt) {
		jws_serverInstance = new Server(theServerPortInt);
		jws_serverPortInt = theServerPortInt;

		ServletHandler aServletHandler = new ServletHandler();
		aServletHandler.addServletWithMapping(new ServletHolder(
				new ZoneSelection_Page()), "/servlets/list-zones");
		aServletHandler.addServletWithMapping(new ServletHolder(
				new ZonePlaylist()), "/servlets/playlist");
		aServletHandler.addServletWithMapping(new ServletHolder(
				new ZoneLibrary()), "/servlets/library");
		aServletHandler.addServletWithMapping(new ServletHolder(
				new ZoneSearchMedia_DialogPage()),
				"/servlets/library-search-dialog");
		aServletHandler.addServletWithMapping(
				new ServletHolder(new ZoneRadio()), "/servlets/radio");
		aServletHandler.addServletWithMapping(new ServletHolder(
				new StaticProxy()), "/*");
		jws_serverInstance.addHandler(aServletHandler);
	}

	public static JettyWebServer getInstance() {
		if (jws_SingleInstance == null) {
			jws_SingleInstance = new JettyWebServer(jws_serverPortInt);
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
			System.err.println(ex);
		}
	}

	public boolean isServerRunning() {
		return jws_serverInstance.isRunning();
	}

	public void stopServer() {
		try {
			jws_serverInstance.stop();
		} catch (Exception ex) {
			System.err.println(ex);
		}
	}
}
