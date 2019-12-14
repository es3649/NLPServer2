package com.studmane.nlpserver.service.exception;

import com.studmane.nlpserver.service.response.MessageResponse;

/**
 * thrown form services in the event of an internal error
 */
public class ServiceErrorException extends Exception {
    static final long serialVersionUID = 124;

    /**
     * constructs a ServiceErrorException
     * @param msg a MessageResponse describing the error
     */
    public ServiceErrorException(MessageResponse msg) {
        super();
        messageResponse = msg;
    }

    /**
     * constructs a ServiceErrorException with a cause
     * @param msg a MessageResponse describing the error
     * @param cause the cause of this error
     */
    public ServiceErrorException(MessageResponse msg, Throwable cause) {
        super(cause);
        messageResponse = msg;
    }

    private MessageResponse messageResponse;

    /**
     * @return the messageResponse
     */
    public MessageResponse getMessageResponse() {
        return messageResponse;
    }
}