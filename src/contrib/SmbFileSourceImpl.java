/*
 * Samba implementation of IFileSource for use with JID3
 */
package contrib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public IFileSource createTempFile(String string, String string1) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean delete() {
        try {
            mySmbFile.delete();
            return true;
        } catch (SmbException ex) {
            if (MusicZones.getIsDebugOn()) {
                Logger.getLogger(SmbFileSourceImpl.class.getName()).log(Level.INFO, null, ex);
            }
            return false;
        }
    }

    public String getName() {
        return mySmbFile.getName();
    }

    public InputStream getInputStream() throws FileNotFoundException {
        InputStream aReturnInputStream = null;
        try {
            aReturnInputStream = mySmbFile.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(SmbFileSourceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return aReturnInputStream;
    }

    public OutputStream getOutputStream() throws FileNotFoundException {
        OutputStream aReturnOutputStream = null;
        try {
            aReturnOutputStream = mySmbFile.getOutputStream();
        } catch (IOException ex) {
            Logger.getLogger(SmbFileSourceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return aReturnOutputStream;
    }

    public long length() {
        long aReturnLong = 0L;
        try {
            aReturnLong = mySmbFile.length();
        } catch (SmbException ex) {
            Logger.getLogger(SmbFileSourceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return aReturnLong;
    }

    public boolean renameTo(IFileSource theIFileSource) throws IOException {
        if (theIFileSource instanceof SmbFileSourceImpl) {
            try {
                mySmbFile.renameTo(((SmbFileSourceImpl) theIFileSource).mySmbFile);
                return true;
            } catch (SmbException ex) {
                Logger.getLogger(SmbFileSourceImpl.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } else {
            return false;
        }
    }
}
