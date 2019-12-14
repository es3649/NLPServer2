package com.studmane.nlpserver.service.response;
/** Serializable Interface
 * 
 * Serializable objects can be serialized to JSON strings
 */
public interface Serializable {

    /** Serializes the object
     * 
     * @return a JSON string containing the object
     */
    public String Serialize();
};
