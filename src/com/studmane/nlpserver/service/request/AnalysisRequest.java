package com.studmane.nlpserver.service.request;

import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import com.studmane.nlpserver.Server;
import com.studmane.nlpserver.service.exception.BadRequestException;
import com.studmane.nlpserver.service.model.Conversation;
import com.studmane.nlpserver.service.response.MessageResponse;
import com.studmane.nlpserver.service.response.Serializable;

public class AnalysisRequest implements Serializable {
    AnalysisRequest() {}

    private String message;
    private String name;
    private String number;
    private Conversation conversation;

    /**
     * Creates a new AnalysisRequest object from a json string
     * @param requestJson the strin containing the json data
     * @return the AnalysisRequest instance represented by the json
     * @throws BadRequestException if the json is invalid
     */
    public static AnalysisRequest fromJson(String requestJson) throws BadRequestException {
        AnalysisRequest req = new AnalysisRequest();
        Gson gson = new Gson();
        TypeToken<AnalysisRequest> typTok = new TypeToken<AnalysisRequest>() {};
        try {
            req = gson.fromJson(requestJson, typTok.getType());
        } catch (JsonSyntaxException ex) {
            throw new BadRequestException(
                new MessageResponse("Unable to parse request body"), ex);
        }

        Server.logger.log(Level.INFO, 
            String.format("Parsed AnalysisRequest from Json: %s", req.toString()));

        // validate
        if (!req.isValid()) {
            throw new BadRequestException(
                new MessageResponse("Request has null or empty values"));
        }

        return req;
    }

    @Override
    public String Serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Checks whether or not the AnalysisRequest object is valid
     * TODO define valid for this object
     * @return the validity of this instance
     */
    public boolean isValid() {
        // none of these fields should be null
        // name and conversation should not both be null 
        // (a name can take the place of a conversation, a conversation will be constructed)
        if (message == null
            || number == null
            || (name == null && conversation == null)) {
            return false;
        }

        // If there is a conversation, make sure it is valid.
        if (conversation != null) {
            return conversation.isValid();
        }

        return true;
    }

    //---------------------------------- getters ----------------------------------//

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
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
}