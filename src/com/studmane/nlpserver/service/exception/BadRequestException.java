package com.studmane.nlpserver.service.exception;

import com.studmane.nlpserver.service.response.MessageResponse;

/**
 * an exception class denoting a bad request
 */
public class BadRequestException extends ServiceErrorException {
    static final long serialVersionUID = 15356134;

    /**
     * Constructs a BadRequestException with a MessageResponse detailing
     * why the request was bad
     * @param msg a MessageResonse telling why the request was bad
     */
    public BadRequestException(MessageResponse msg) {
        super(msg);
    }

    /**
     * constructs a BadRequestException with a cause
     * @param msg a MessageResponse describing the error
     * @param cause the cause of this error
     */
    public BadRequestException(MessageResponse msg, Throwable cause) {
        super(msg, cause);
    }
}