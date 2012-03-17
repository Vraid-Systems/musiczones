/*
 * static resource GET requests are made through this Android AssetManager proxy
 */
package servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import musiczones.MusicZones;

/**
 * @author Jason Zerbe
 */
public class StaticProxy extends HttpServlet {

	private static final long serialVersionUID = 42L;
	protected static HashMap<String, String> myExtensionMap;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		myExtensionMap = new HashMap<String, String>();
		myExtensionMap.put("css", "text/css");
		myExtensionMap.put("html", "text/html");
		myExtensionMap.put("ico", "image/vnd.microsoft.icon");
		myExtensionMap.put("jpeg", "image/jpeg");
		myExtensionMap.put("jpg", "image/jpeg");
		myExtensionMap.put("js", "text/javascript");
		myExtensionMap.put("png", "image/png");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String aRequestedUri = req.getRequestURI();
		if ((aRequestedUri == null) || (aRequestedUri.equals(""))) {
			resp.sendError(400, "Missing resource string");
		} else {
			if (aRequestedUri.equals("/")) {
				aRequestedUri = aRequestedUri + "index.html";
			}

			String[] aRequestedUriArray = aRequestedUri.split("/");
			String aRequestedFileName = aRequestedUriArray[(aRequestedUriArray.length - 1)];
			String[] aRequestedFileNameArray = aRequestedFileName.split("\\.");
			String aRequestedFileExtension = aRequestedFileNameArray[(aRequestedFileNameArray.length - 1)];

			String aRequestAssetsPath = "webapp" + aRequestedUri;
			InputStream aResourceInputStream = MusicZones.getAssets().open(
					aRequestAssetsPath);

			resp.setContentType(myExtensionMap.get(aRequestedFileExtension));

			byte[] aBuffer = new byte[4096];
			int aBytesReadCount = 0;
			while ((aBytesReadCount = aResourceInputStream.read(aBuffer)) != -1) {
				resp.getOutputStream().write(aBuffer, 0, aBytesReadCount);
			}
			aResourceInputStream.close();
			resp.getOutputStream().flush();
			resp.getOutputStream().close();
		}
	}

}
