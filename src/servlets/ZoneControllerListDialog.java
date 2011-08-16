/*
 * servlet that lists off all the available zone controllers in a generated
 * jquerymobile dialog page
 * 
 * based on: http://jquerymobile.com/demos/1.0a4.1/docs/pages/dialog.html
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Jason Zerbe
 */
public class ZoneControllerListDialog extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        out.println("");
    }
}
