/*
 * class for controlling the VLC application depending on the
 * command recieved over the network
 */
package multicastmusiccontroller;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Zerbe
 */
public class VlcController {

    protected String vlcPath = null;
    protected Runtime runTime = null;

    public VlcController(String theVlcPath) {
        vlcPath = theVlcPath;
        runTime = Runtime.getRuntime();
    }

    /**
     * play a URI using VLC's dummy interface
     * @param theUriToPlay String
     * @return boolean - did the Uri play?
     */
    public boolean playFileWithDummy(String theUriToPlay) {
        String aVlcCommand = vlcPath + " -I dummy " + theUriToPlay + " vlc://quit";
        return executeCommand(aVlcCommand);
    }

    /**
     * allows for access to OS for running external commands
     * @param theCommand String
     * @return boolean - did the command execute?
     */
    private boolean executeCommand(String theCommand) {
        try {
            runTime.exec(theCommand);
        } catch (IOException ex) {
            Logger.getLogger(VlcController.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }
}
