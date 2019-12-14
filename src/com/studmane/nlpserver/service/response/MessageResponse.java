package com.studmane.nlpserver.service.response;

import com.google.gson.Gson;

/** class MessageResponse
 */
public class MessageResponse implements Serializable {
    /** Constructs an empty MessageResponse
     */
    public MessageResponse() {}

    /**
     * Constructs a new MessageResponse containing a message
     * @param Message the message to be stored
     */
    public MessageResponse(String Message) {
        this.setMessage(Message);
    }

    private String message;

    /** Serialize inmplements the Serializable interface
     * 
     * @return a JSON string containing the MessageResponse object
     */
    public String Serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) return false;
        if (this == obj) return true;

        MessageResponse that = (MessageResponse)obj;

        return this.message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }
};
