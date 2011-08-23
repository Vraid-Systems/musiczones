/*
 * singleton class for managing CIFS network connections
 */
package contrib;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * @author Jason
 */
public class CIFSNetworkInterface {

    private static CIFSNetworkInterface cni_SingleInstance = null;

    protected CIFSNetworkInterface() {
    }

    public static CIFSNetworkInterface getInstance() {
        if (cni_SingleInstance == null) {
            cni_SingleInstance = new CIFSNetworkInterface();
        }
        return cni_SingleInstance;
    }

    /**
     * create a SmbFile from the passed path, and after making sure it is
     * indeed a directory, grab the directory contents
     * @param thePath String
     * @return HashMap<String, String>
     */
    public HashMap<SmbFile, String> getDirectoryList(String thePath) {
        SmbFile aSmbFile = null;
        try {
            aSmbFile = new SmbFile(thePath);
        } catch (MalformedURLException ex) {
            Logger.getLogger(CIFSNetworkInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (aSmbFile == null) {
            return null;
        }

        boolean aSmbFileIsDirectory = false;
        try {
            aSmbFileIsDirectory = aSmbFile.isDirectory();
        } catch (SmbException ex) {
            Logger.getLogger(CIFSNetworkInterface.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (aSmbFileIsDirectory) {
            SmbFile[] aSmbFileArray = null;
            try {
                aSmbFileArray = aSmbFile.listFiles();
            } catch (SmbException ex) {
                Logger.getLogger(CIFSNetworkInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (aSmbFileArray == null) {
                return null;
            }

            HashMap<SmbFile, String> returnFileMap = new HashMap<SmbFile, String>();
            for (SmbFile iSmbFile : aSmbFileArray) {
                boolean iSmbFileIsDirectory = false;
                try {
                    iSmbFileIsDirectory = iSmbFile.isDirectory();
                } catch (SmbException ex) {
                    Logger.getLogger(CIFSNetworkInterface.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (iSmbFileIsDirectory) {
                    returnFileMap.put(iSmbFile, "");
                } else {
                    int aIndexOfExtension = iSmbFile.getName().lastIndexOf(".");
                    if (aIndexOfExtension > 0) {
                        String aFileExtensionStr = iSmbFile.getName().substring(aIndexOfExtension);
                        returnFileMap.put(iSmbFile, aFileExtensionStr);
                    } else {
                        returnFileMap.put(iSmbFile, "");
                    }
                }
            }

            return returnFileMap;
        } else {
            return null;
        }
    }
}
