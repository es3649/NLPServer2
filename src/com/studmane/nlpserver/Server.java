package com.studmane.nlpserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ArrayIndexOutOfBoundsException;
import java.lang.NumberFormatException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.sql.Timestamp;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;


// import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import com.studmane.nlpserver.handler.*;
import com.studmane.nlpserver.Password;
import com.studmane.nlpserver.PasswordProvider;


/** class Server
 */
public class Server {
    public Server() {}

    public static Logger logger;
    public static PasswordProvider password;

    static {
        Level logLevel = Level.FINEST;
        password = new Password();

        logger = Logger.getLogger("server");
        logger.setLevel(logLevel);
        logger.setUseParentHandlers(false);
        
        try {
            // try to set up the file handler, if it fails, the set up
            // the console handler

            // build a filename with date-time
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
            Date date = new Date();
            Timestamp ts = new Timestamp(date.getTime());

            String filename = "log/server"+ sdf.format(ts) +".txt";
            new File(filename).createNewFile();
            
            FileHandler fileHandler = new FileHandler(filename);
            fileHandler.setLevel(logLevel);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);


        } catch (IOException ex) {
            // log the error we already got:
            logger.log(Level.WARNING, "Failed to initialize file logger", ex);
        } finally {
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(logLevel);
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);

        }
    }

    /**
     * main begins hosting the Server
     * @param args command line arguments, a port number to host on, defaults to 443
     */
    public static void main(String args[]) {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            // usage();
            System.out.println("Failed to parse int from argument");
            return;
        } catch (ArrayIndexOutOfBoundsException ex) {
            logger.log(Level.INFO, "No port provided, defaulting to 443");
            port = 443;
        }

        Server s = new Server();
        s.run(port);
    }

    private static final int MAX_WAITING_CONNS = 24;
    private HttpsServer server;

    /**
     * runs the server
     * @param port the port number on which to run the server
     */
    public void run(int port) {
        // initialize the server
        System.out.println("Initializing server");
        try {
            // load certificate
            char[] password = Server.password.KeystorePassword().toCharArray();        // keystore password
            KeyStore ks = KeyStore.getInstance("JKS");                  // create keystore, throws KeystoreException
            FileInputStream fis = new FileInputStream(Server.password.KeystoreFilename());  // read and load the key
            // throws KeystoreException, NoSuchAlgorithmException, CertificateException
            ks.load(fis, password);

            // display certificate, throws KeystoreException
            Certificate cert = ks.getCertificate("self_signed");
            System.out.printf("using certificate %s\n", cert);

            // set up key manager
            // throws UnrecoverableKeyException, KeystoreException, NoSuchAlgorithmException
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            // set up trust manager factory
            // Throws KeystoreException, NoSuchAlgorithmException
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // create the server, use TLS protocol
            server = HttpsServer.create(
                new InetSocketAddress(port), 
                MAX_WAITING_CONNS);
            // throws NoSuchAlgorithmException
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // set up HTTPS context and parameters
            // throws KeyManagementException
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    try {
                        // initialiaze SSL context
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // get default parameters
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Failed to create HTTPS port", ex);
                    }
                }
            });

        // account for various classes of errors
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Caught IOException", ex);
            System.out.println("Failed to initialize");
            return;
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "Algorithm not found", ex);
            return;
        } catch (KeyStoreException ex) {
            logger.log(Level.SEVERE, "Keystore Error", ex);
            return;
        } catch (KeyManagementException ex) {
            logger.log(Level.SEVERE, "Key management Error", ex);
            return;
        } catch (UnrecoverableKeyException ex) {
            logger.log(Level.SEVERE, "Key is unrecoverable", ex);
            return;
        } catch (CertificateException ex) {
            logger.log(Level.SEVERE, "Certificate Error", ex);
            return;
        }

        // don't know what this does, apparently it's important
        server.setExecutor(null);

        // create the contexts
        System.out.println("Creating contexts");

        // a file handler in case we want to serve up webpages
        server.createContext("/", new com.studmane.nlpserver.handler.FileHandler());
        // we need a process handler
        server.createContext("/analyze", new AnalysisHandler());
        // TODO consider adding a handler with a more granular readout, for debugging

        // start the server!
        System.out.println("Starting server...");
        server.start();
        System.out.printf("Server started on port %d\n", port);
        logger.log(Level.INFO, "Server started");
    }

    /**
     * prints the usage information
     */
    private static void usage() {
        System.out.println("Usage: java main/Server <port number>");
    }
};
