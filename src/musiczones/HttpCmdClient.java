/*
 * singleton class for issuing HTTP GET requests to master server
 */
package musiczones;

import contrib.JettyWebServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import zonecontrol.ZoneServerLogic;

/**
 * @author Jason Zerbe
 */
public class HttpCmdClient {

    private static HttpCmdClient hcc_singleInstance = null;
    private boolean hcc_DebugOn = false;
    private String hcc_theMasterServerUrlStr = "http://mz.vraidsys.com/";

    protected HttpCmdClient(boolean theDebugOn) {
        hcc_DebugOn = theDebugOn;
    }

    public static HttpCmdClient getInstance() {
        if (hcc_singleInstance == null) {
            hcc_singleInstance = new HttpCmdClient(false);
        }
        return hcc_singleInstance;
    }

    public static HttpCmdClient getInstance(boolean theDebugOn) {
        if (hcc_singleInstance == null) {
            hcc_singleInstance = new HttpCmdClient(theDebugOn);
        }
        return hcc_singleInstance;
    }

    public void setMasterSeverUrlStr(String theServerUrlStr) {
        hcc_theMasterServerUrlStr = theServerUrlStr;
    }

    public String getMasterSeverUrlStr() {
        return hcc_theMasterServerUrlStr;
    }

    /**
     * notify master server (calling) node is up, empty response = no errors
     * @param thePersistentFlagIsOn boolean - shall it keep retrying?
     * @return boolean - did server respond with empty page?
     */
    public boolean notifyUp(boolean thePersistentFlagIsOn) {
        String aURLConnectionParamStr = "opt=ping"
                + "&uuid=" + ZoneServerLogic.getInstance().getUUID()
                + "&http=" + String.valueOf(JettyWebServer.getInstance().getServerPortInt())
                + "&address=" + Layer3Info.getInstance().getValidIPAddress(Layer3Info.IpAddressType.IPv4)
                + "&name=" + replaceSpacesWithUnderscores(ZoneServerLogic.getInstance().getZoneName());

        boolean loggedToMasterServer = false;
        while (!loggedToMasterServer) {
            loggedToMasterServer = voidServerMethod(hcc_theMasterServerUrlStr, aURLConnectionParamStr);
            if (!thePersistentFlagIsOn) {
                break;
            }
            if (!loggedToMasterServer) {
                try {
                    Thread.sleep((1 * 1000)); //sleep in milliseconds
                } catch (InterruptedException ex) {
                    Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return loggedToMasterServer;
    }

    protected String replaceSpacesWithUnderscores(String theSpacedString) {
        return theSpacedString.replace(" ", "_");
    }

    /**
     * notify master server node is down/offline, empty response = no errors
     * @return boolean - did server respond with empty page?
     */
    public boolean notifyDown() {
        String aURLConnectionParamStr = "opt=ping"
                + "&uuid=" + ZoneServerLogic.getInstance().getUUID()
                + "&remove=true";
        return voidServerMethod(hcc_theMasterServerUrlStr, aURLConnectionParamStr);
    }

    /**
     * does the string contain shit from the free hosting provider
     * @param theRawLine String
     * @return boolean
     */
    protected boolean containsFreeHostShit(String theRawLine) {
        if (theRawLine.contains("Analytics Code")) {
            return true;
        } else if (theRawLine.contains("hosting24")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * HTTP GET should have a void response - empty single line returned
     * @param theUrlBase String - part before "?"
     * @param theUrlArgs String - part after "?"
     * @return boolean - was there a local or server side error?
     */
    protected boolean voidServerMethod(String theUrlBase, String theUrlArgs) {
        ArrayList<String> returnArrayList = returnServerMethod(theUrlBase, theUrlArgs);
        if ((returnArrayList != null) && (returnArrayList.size() > 1)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * internal method for doing a GET with response retrieval. if theUrlArgs
     * is null, then just use theUrlBase without trailing "?"
     * @param theUrlBase String - part before "?"
     * @param theUrlArgs String - part after "?"
     * @return ArrayList<String>
     */
    protected ArrayList<String> returnServerMethod(String theUrlBase, String theUrlArgs) {
        //be able to function w/o master server
        if ((theUrlBase == null) || (theUrlBase.equals(""))) {
            return null;
        }

        //create the HTTP buffered reader
        BufferedReader aBufferedReader = null;
        try {
            aBufferedReader = new BufferedReader(new InputStreamReader(getURLConnection(theUrlBase, theUrlArgs).getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //read from the HTTP stream
        ArrayList<String> returnArrayList = new ArrayList<String>();
        try {
            String aString = null;
            while ((aString = aBufferedReader.readLine()) != null) {
                if (!containsFreeHostShit(aString) && !aString.equals("")) {
                    returnArrayList.add(aString);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //close up buffered HTTP stream reader
        try {
            aBufferedReader.close();
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.WARNING, null, ex);
        }

        return returnArrayList;
    }

    /**
     * helper method for constructing a URLConnection. if theUrlArgs is null
     * then just use theUrlBase with no trailing "?"
     * @param theUrlBase String - part before "?"
     * @param theUrlArgs String - part after "?"
     * @return URLConnection
     */
    protected URLConnection getURLConnection(String theUrlBase, String theUrlArgs) {
        //create the resolvable url
        URL aUrl = null;
        try {
            if (theUrlArgs == null) {
                aUrl = new URL(theUrlBase);
            } else {
                aUrl = new URL(theUrlBase + "?" + theUrlArgs);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //output debug
        if (hcc_DebugOn) {
            System.out.println(this.getClass().getName() + " - URL Connection - " + aUrl.toString());
        }

        //make the connection to the url
        URLConnection aURLConnection = null;
        try {
            aURLConnection = aUrl.openConnection();
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //url connection is ready
        return aURLConnection;
    }
}
