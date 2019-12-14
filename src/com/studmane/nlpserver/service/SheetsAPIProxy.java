package com.studmane.nlpserver.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.studmane.nlpserver.Server;


/**
 * SheetsAPIProxy provides a class interface for the google sheets API
 * a majority of this was taken from the Google sheets API quickstart at
 * https://developers.google.com/sheets/api/quickstart/java
 * 
 * Also use this later to write values:
 * https://developers.google.com/sheets/api/guides/values
 */
public class SheetsAPIProxy {
    private static final String APPLICATION_NAME = "NLPServer";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * global instance of scopes required by the application
     * we may need to update this to use writing as well
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = Server.password.GoogleCredentialsFile();
 
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        //load the secrets
        InputStream in = SheetsAPIProxy.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: "+CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static List<List<Object>> getSheet() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetID = Server.password.AppointmentSheetID();
        // TODO we need to figure out how to get the range.
        // perhaps we will automate creation of new sub-sheets
        final String range = null;

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();
        ValueRange response = service.spreadsheets().values()
            .get(spreadsheetID, range)
            .execute();
        return response.getValues();
    }

    /**
     * Gets the open apointment slots on the given day
     * @param timesOnThisDay the day on which to get appointment times
     * @param withBishop do we require these times to be with bishop, or not?
     * @return a list of available times on given date
     */
    public List<Calendar> getAvailableTimes(Calendar timesOnThisDay, boolean withBishop) {
        // get sheet
        // read values for dates and times
        // get closest times to church (for sundays) or to 6:45 (for Wednesdays) (with pseudo BFS)
        assert false;
        return null;
    }

    /**
     * reserves an appointment in the spreadsheet
     * @param appointment the time of the appointment to reserve
     * @param name the nape of the person tp reserve it for
     * @param purpose
     */
    public void reserveAppointment(Calendar appointment, String name, String purpose) {
        // create 
        assert false;
    }

    /**
     * create a new sheet in the target streadsheet for a new month of appointments
     * @param month some date in the month for which we will create the new sheet
     */
    public void createNewMonthSheet(Calendar month) {
        assert false;
    }
}