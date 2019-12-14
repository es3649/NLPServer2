package com.studmane.nlpserver.service.model;

/**
 * A Message Intent declares the intent of the message
 * A message should parse intents in the order in which they appear here
 * Theoretically, a message can meet multiple of these intents,
 * we will probably deal with each one separately
 */
public enum MessageIntent {
    SCHEDULE            {public String toString() {return "SCHEDULE";}}, 
    REQUEST_RESCHEDULE  {public String toString() {return "REQUEST_RESCHEDULE";}},
    APPOINTMENT_INVALID {public String toString() {return "APPOINTMENT_INVALID";}},

    CANCEL              {public String toString() {return "CANCEL";}},

    NEGATIVE            {public String toString() {return "NEGATIVE";}},
    AFFIRMATIVE         {public String toString() {return "AFFIRMATIVE";}},

    // QUERY_WHEN shall entail QUERY_WHERE
    QUERY_WHEN          {public String toString() {return "QUERY_WHEN";}}, 
    QUERY_WHERE         {public String toString() {return "QUERY_WHERE";}},

    THANKS              {public String toString() {return "THANKS";}},

    OTHER               {public String toString() {return "OTHER";}}
}