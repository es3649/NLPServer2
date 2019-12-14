package com.studmane.nlpserver.handler;

// import java.io.FileInputStream;
import java.io.IOException;
// import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.studmane.nlpserver.Server;

/**
 * a file handler for serving up webpages
 */
public class FileHandler implements HttpHandler {
    public FileHandler() {};

    private static final String WEB_FILE_PREFIX = "libs/web/";
    private static final String HOME_PAGE = "index.html";

    /**
     * handle method satisfies the HttpHandler interface.
     * This serves up webpages.
     * 
     * @param exchange the HttpExchange to handle
     * @throws IOException if there is an error reading or writing the response body
     */
    public void handle(HttpExchange exchange) throws IOException {
        // go get the web pages we wanted and serve them up
        // with a little parsley and radish cubes for good measure
        OutputStream httpResponse = exchange.getResponseBody();
        try {

            Path sitePath = makeFilePath(exchange.getRequestURI().getRawPath());
            Server.logger.log(Level.INFO, "Sending file at: " + sitePath.toString());
            
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            exchange.setAttribute("Content-Type", "text/html");
            
            Files.copy(sitePath, httpResponse);
        } catch (IOException ex) {
            // this should still fly, but we want to log him before he goes
            Server.logger.log(Level.SEVERE, "Error serving file", ex);
            throw ex;
        } finally {
            // close all and be done
            httpResponse.flush();
            httpResponse.close();
            exchange.close();
        }
    }

    private Path makeFilePath(String URIString) {
        if (URIString.equals("/")) {
            URIString = HOME_PAGE;
        }
        return Paths.get(WEB_FILE_PREFIX, URIString);
    }
}