/*
 * singleton class for getting CIFS information
 */
package netutil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

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
	 * copy a remote SMB file to local storage
	 * 
	 * @param theRemotePath
	 *            String
	 * @param theLocalPath
	 *            String
	 * @return boolean - was the remote file copied to local storage?
	 */
	public boolean copyRemoteFileToLocalFile(String theRemotePath,
			String theLocalPath) {
		SmbFileInputStream aSmbFileInputStream = null;
		try {
			aSmbFileInputStream = new SmbFileInputStream(theRemotePath);
		} catch (SmbException e) {
			System.err.println(e);
			return false;
		} catch (MalformedURLException e) {
			System.err.println(e);
			return false;
		} catch (UnknownHostException e) {
			System.err.println(e);
			return false;
		}
		FileOutputStream aFileOutputStream = null;
		try {
			aFileOutputStream = new FileOutputStream(theLocalPath);
		} catch (FileNotFoundException e) {
			System.err.println(e);
			return false;
		}

		boolean aTransferWorkedFlag = transferSmbInputStreamToFileOutputStream(
				aSmbFileInputStream, aFileOutputStream);
		if (!aTransferWorkedFlag) {
			return false;
		}

		return true;
	}

	/**
	 * transfer bytes between SmbFileInputStream and FileOutputStream closes
	 * both streams when done
	 * 
	 * @param theSmbFileInputStream
	 *            SmbFileInputStream
	 * @param theFileOutputStream
	 *            FileOutputStream
	 * @return boolean - did the byte transfer work?
	 */
	public boolean transferSmbInputStreamToFileOutputStream(
			SmbFileInputStream theSmbFileInputStream,
			FileOutputStream theFileOutputStream) {
		if ((theSmbFileInputStream == null) || (theFileOutputStream == null)) {
			return false;
		}

		byte[] aByteArray = new byte[4096];
		int aBytesReadCount = 0;
		try {
			while ((aBytesReadCount = theSmbFileInputStream.read(aByteArray)) != -1) {
				theFileOutputStream.write(aByteArray, 0, aBytesReadCount);
			}
		} catch (IOException e) {
			System.err.println(e);
			return false;
		}

		try {
			theSmbFileInputStream.close();
		} catch (IOException e) {
			System.err.println(e);
		}
		try {
			theFileOutputStream.flush();
		} catch (IOException e) {
			System.err.println(e);
			return false;
		}
		try {
			theFileOutputStream.close();
		} catch (IOException e) {
			System.err.println(e);
		}

		return true;
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
			System.err.println(ex);
			return null;
		}

		boolean aSmbFileIsDirectory = false;
		try {
			aSmbFileIsDirectory = aSmbFile.isDirectory();
		} catch (SmbException ex) {
			System.err.println(ex);
			return null;
		}

		if (aSmbFileIsDirectory) {
			SmbFile[] aSmbFileArray = null;
			try {
				aSmbFileArray = aSmbFile.listFiles();
			} catch (SmbException ex) {
				System.err.println(ex);
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
