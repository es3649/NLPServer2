package com.studmane.nlpserver.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.studmane.nlpserver.Server;
import com.studmane.nlpserver.service.exception.BadRequestException;
// import service.exception.InvalidLoginException;
import com.studmane.nlpserver.service.exception.ServiceErrorException;
// import service.exception.ThatsNotYoursException;
import com.studmane.nlpserver.service.response.MessageResponse;
import com.studmane.nlpserver.service.response.Serializable;

/**
 * Handler class is an abstract superclass that holds code relevant to all handlers
 */
public abstract class Handler implements HttpHandler {
    Handler() {};

    /**
     * implements the HttpHandler interface
     * handles an http request
     * @param exchange the current http exchange
     * @throws IOException if there are troubles reading/writing the http exchange
     */
    public void handle (HttpExchange exchange) throws IOException {
        
        // make a printwriter and get the response body
        OutputStream responseBody = exchange.getResponseBody();
        PrintWriter pw = new PrintWriter(responseBody);

        Server.logger.log(Level.INFO, "Event request. URI is: " + exchange.getRequestURI().toString());
        
        // verify request method
        if (!isProperRequestMethod(exchange)) {
            Server.logger.log(Level.INFO, "Failed for Bad request (405): Wrong request method");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, 0);

            MessageResponse response = new MessageResponse("Bad request method");
            String json = response.Serialize();
            pw.write(json);
            closeAll(pw, responseBody, exchange);
            return;
        }

        // authenticate
        if(!isAuthentic(exchange)) {
            Server.logger.log(Level.INFO, "Failed for Bad request (401): Authentication failed");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, 0);
            
            MessageResponse response = new MessageResponse("Failed to authenticate");
            String json = response.Serialize();
            pw.write(json);
            closeAll(pw, responseBody, exchange);
            return;
        }

        try {
            Serializable response = handleAndServe(exchange);

            // 200 - OK
            Server.logger.log(Level.INFO, "Request successful (200)");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);

            String json = response.Serialize();
            pw.write(json);

        } catch (BadRequestException ex) { // got bad args
            // 400 - Bad Request
            Server.logger.log(Level.INFO, "Failed for Bad request (400)", ex);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
            
            String json = ex.getMessageResponse().Serialize();
            Server.logger.log(Level.INFO, "Returning response: " + json);
            pw.write(json);

        // } catch (InvalidLoginException ex) { // failed to authenticate Login
        //     // 401 failed to authenticate
        //     Server.logger.log(Level.INFO, "Failed by bad login (401)", ex);
        //     exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, 0);

        //     String json = ex.getMessageResponse().Serialize();
        //     Server.logger.log(Level.INFO, "Returning response: " + json);
        //     pw.write(json);

        // } catch (ThatsNotYoursException ex) { // requested assets that weren't theirs
        //     // 403 - Forbidden
        //     Server.logger.log(Level.INFO, "Failed by permissions (403)", ex);
        //     exchange.sendResponseHeaders(HttpURLConnection.HTTP_FORBIDDEN, 0);
            
        //     String json = ex.getMessageResponse().Serialize();
        //     Server.logger.log(Level.INFO, "Returning response: " + json);
        //     pw.write(json);

        } catch (ServiceErrorException ex) { // check this last because the first 2 inherit from it
            // 500 - internal error
            Server.logger.log(Level.SEVERE, "Failed internally (500)", ex);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);

            String json = ex.getMessageResponse().Serialize();
            Server.logger.log(Level.INFO, "Returning response: " + json);
            pw.write(json);

        } // some other kind of not exist error?

        // c\lose the exchange and we're done
        closeAll(pw, responseBody, exchange);
        return;

    }

    /**
     * checks if the string there has the correct request method (GET or POST)
     * @param exchange the relevant HttpExchange
     * @return a boolean indicating whether the request method was correct
     */
    private boolean isProperRequestMethod(HttpExchange exchange) {
        return exchange.getRequestMethod().toUpperCase().equals(properRequestMethod());
    }


    protected static final String GET_REQUEST_METHOD = "GET";
    protected static final String POST_REQUEST_METHOD = "POST";
    /**
     * a function to be overloaded by the subclasses which will specify the
     * correct request type
     * @return a string representing the correct request method
     */
    protected abstract String properRequestMethod();

    /**
     * returns a boolean indicating whether or not the request has
     * a proper authentication WHERE NEEDED
     * i.e.: /clear requests will always return true, whereas
     * /person requests will actaully verify authTokens
     * @param exchange the relevant httpexcahnge
     * @return a booleanf indicating if the exchange is authentic
     */
    protected abstract boolean isAuthentic(HttpExchange exchange);

    /**
     * handles other arguments coming into the exchange and calls
     * the relevant service method
     * @param exchange the current HttpExchange
     * @return a Serializable indicating the result of serving
     * @throws ServiceErrorException if the services encounter some
     *      type of unresolvable internal error
     */
    protected abstract Serializable handleAndServe(HttpExchange exchange) 
            throws ServiceErrorException;

    private void closeAll(PrintWriter pw, OutputStream os, HttpExchange exchange)
            throws IOException {
        pw.flush();
        pw.close();
        os.close();
        exchange.close();
    }
}