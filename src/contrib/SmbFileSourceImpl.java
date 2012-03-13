/*
 * Samba implementation of IFileSource for use with JID3
 */
package contrib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import musiczones.MusicZones;
import org.blinkenlights.jid3.io.IFileSource;

/**
 * @author Jason Zerbe
 */
public class SmbFileSourceImpl implements IFileSource {

    private SmbFile mySmbFile;

    public SmbFileSourceImpl(SmbFile theSmbFile) {
        mySmbFile = theSmbFile;
    }

    @Override
    public IFileSource createTempFile(String string, String string1) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean delete() {
        try {
            mySmbFile.delete();
            return true;
        } catch (SmbException ex) {
            if (MusicZones.getIsDebugOn()) {
            	System.err.println(ex);
            }
            return false;
        }
    }

    @Override
    public String getName() {
        return mySmbFile.getName();
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        InputStream aReturnInputStream = null;
        try {
            aReturnInputStream = mySmbFile.getInputStream();
        } catch (IOException ex) {
        	System.err.println(ex);
        }
        return aReturnInputStream;
    }

    @Override
    public OutputStream getOutputStream() throws FileNotFoundException {
        OutputStream aReturnOutputStream = null;
        try {
            aReturnOutputStream = mySmbFile.getOutputStream();
        } catch (IOException ex) {
        	System.err.println(ex);
        }
        return aReturnOutputStream;
    }

    @Override
    public long length() {
        long aReturnLong = 0L;
        try {
            aReturnLong = mySmbFile.length();
        } catch (SmbException ex) {
        	System.err.println(ex);
        }
        return aReturnLong;
    }

    @Override
    public boolean renameTo(IFileSource theIFileSource) throws IOException {
        if (theIFileSource instanceof SmbFileSourceImpl) {
            try {
                mySmbFile.renameTo(((SmbFileSourceImpl) theIFileSource).mySmbFile);
                return true;
            } catch (SmbException ex) {
            	System.err.println(ex);
                return false;
            }
        } else {
            return false;
        }
    }
}
