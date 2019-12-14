package com.studmane.nlpserver;

/**
 * PasswordProvider provides an interface for the hidden password files
 * 
 * Passwords and sensitive informaiton must be provided at various times.
 * They should all be distributed through this class.
 */
public interface PasswordProvider {
    /**
     * Returns the Keystore filename
     * @return keystore filename as a string
     */
    public String KeystoreFilename();

    /**
     * Returns the password to the keystore
     * @return keystore password as a string
     */
    public String KeystorePassword();

    /**
     * returns the ID of the google sheet we are accessing
     * @return google sheet ID as a string
     */
    public String AppointmentSheetID();

    /**
     * returns the name of the credentials file
     * @return the name of the credentials file as a string
     */
    public String GoogleCredentialsFile();

    public static final String PASSWORD_REQUEST_HEADER = "Password";

    /**
     * the API requests are password protected.
     * This method returns the password which is to be used
     * @return the API password
     */
    public String APIPassword();
}