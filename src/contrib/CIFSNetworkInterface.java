/*
 * singleton class for getting CIFS information
 */
package contrib;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * @author Jason Zerbe
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
	 * create a SmbFile from the passed path, and after making sure it is indeed
	 * a directory, grab the directory contents
	 * 
	 * @param thePath
	 *            String
	 * @return ArrayList<SmbFile>, on error null
	 */
	public ArrayList<SmbFile> getDirectoryList(String thePath) {
		SmbFile aSmbFile = null;
		try {
			aSmbFile = new SmbFile(thePath);
		} catch (MalformedURLException ex) {
			Logger.getLogger(CIFSNetworkInterface.class.getName()).log(
					Level.SEVERE, null, ex);
			return null;
		}

		boolean aSmbFileIsDirectory = false;
		try {
			aSmbFileIsDirectory = aSmbFile.isDirectory();
		} catch (SmbException ex) {
			Logger.getLogger(CIFSNetworkInterface.class.getName()).log(
					Level.SEVERE, null, ex);
			return null;
		}

		if (aSmbFileIsDirectory) {
			SmbFile[] aSmbFileArray = null;
			try {
				aSmbFileArray = aSmbFile.listFiles();
			} catch (SmbException ex) {
				Logger.getLogger(CIFSNetworkInterface.class.getName()).log(
						Level.WARNING, null, ex);
				return null;
			}
			if (aSmbFileArray == null) {
				return null;
			}

			ArrayList<SmbFile> returnFileList = new ArrayList<SmbFile>();
			returnFileList.addAll(Arrays.asList(aSmbFileArray));
			return returnFileList;
		} else {
			return null;
		}
	}
}
