package com.studmane.nlpserver.service.response;

import com.studmane.nlpserver.Server;

import java.util.logging.Level;

import com.google.gson.Gson;

/** class LoginSuccessResponse
 */
public class LoginSuccessResponse implements Serializable {
    /** Creates an empty LoginSuccessResponse
     */
    public LoginSuccessResponse() {}

    /**
     * Constructs a LoginSuccessResopnse filled out with the given fields
     * @param tok the AuthToken string to set
     * @param username the username to set
     * @param personID the personID to set
     */
    public LoginSuccessResponse(String tok, String username, String personID) {
        this.setAuthToken(tok);
        this.setUsername(username);
        this.setPersonID(personID);
    }

    private String authToken;
    private String username;
    private String personID;

    /** Serialize inmplements the Serializable interface
     * 
     * @return a JSON string containing the LoginSuccessResponse object
     */
    public String Serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * @return the authToken
     */
    public String getAuthToken() {
        return authToken;
    }
    /**
     * @param authToken the authToken to set
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * @return the personID
     */
    public String getPersonID() {
        return personID;
    }
    /**
     * @param personID the personID to set
     */
    public void setPersonID(String personID) {
        this.personID = personID;
    }
};
