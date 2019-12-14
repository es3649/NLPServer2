package com.studmane.nlpserver.service.response;

import com.google.gson.Gson;

import com.studmane.nlpserver.service.model.Conversation;

/**
 * The response object for the analysis response
 */
public class AnalysisResponse implements Serializable {
    public AnalysisResponse() {}

    // the message is the message to be sent
    private String message;
    // the number to send the message to
    private String number;
    private Conversation conversation;

    /**
     * Uses GSON to serialize the response
     * @return a json representation of the response
     */
    @Override
    public String Serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    //////////////////// Getters and setters ////////////////////
    /**
     * @return the number
     */
    public String getNumber() {
        return number;
    }
    /**
     * @return the conversation
     */
    public Conversation getConversation() {
        return conversation;
    }
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    /**
     * @param conversation the conversation to set
     */
    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }
    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    /**
     * @param number the number to set
     */
    public void setNumber(String number) {
        this.number = number;
    }
}