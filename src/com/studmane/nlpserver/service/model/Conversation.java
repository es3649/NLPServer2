package com.studmane.nlpserver.service.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
// import java.util.Map;
// import java.util.TreeMap;
import java.util.UUID;

/**
 * The Conversation class defines an information template which will be used to persistently store
 * data bout a conversation
 */
public class Conversation {
    public Conversation(String _name) {
        this.name = _name;
        this.ID = UUID.randomUUID().toString();
        this.state = Conversation.STATE_PROCESSING;
    }

    private String ID;
    private String name;
    private String purpose = "Interview";
    private List<Unavailability> unavailable;
    // the time when the appointment will happen, 
    // if this is filled and the state = PROCESSING_STATE, then it is a proposed appt date
    private Calendar appointmentTime;
    private Calendar timeAppointmentSet;    // the time when the appointment was set by the system
    private String state;

    public static final String STATE_ARCHIVED = "Archived";
    public static final String STATE_SET = "Appt set";
    public static final String STATE_APPT_PROPOSED = "Appt proposed";
    public static final String STATE_PROCESSING = "Processing";
    public static final String STATE_RESCHEDULING = "Rescheduling";

    // public static final String DIFF_STATE = "StateChanges";
    // public static final String DIFF_UNAVAIL = "UnavailabilityChanges";
    // public static final String DIFF_APPT = "AppointmentChanged";


    /**
     * Checks that the Conversation object is "valid." There are particular fields that should not be null
     * @return the validity of this object
     */
    public boolean isValid() {
        return (ID != null
            && name != null
            && purpose != null
            && state != null);
    }    

    /**
     * Clones the object
     * @return a copy of this conversation object
     */
    @Override
    public Conversation clone() {
        return (Conversation)this.clone();
    }

    /**
     * This class is essentially a c++ pair with hashing. 
     * It holds 2 Calendar objects and represents a range
     */
    public static class Unavailability {
        public Unavailability(Calendar d1, Calendar d2) {
            begin = d1;
            end = d2;
        }

        public Calendar begin;
        public Calendar end;

        /**
         * yeah
         * @return uh-huh
         */
        @Override
        public int hashCode() {
            return (begin.hashCode() << 1) ^ end.hashCode();
        }

        /**
         * Checks if the given time is during an unavailability
         * @param time the time to check
         * @param unavailability the range to check in
         * @return true if it's during, else false.
         */
        public boolean isDuring(Calendar time, Unavailability unavailability) {
            return !(time.before(unavailability.begin) || time.after(unavailability.end));
        }
    }


    ///////////////// Getters and Setters /////////////////
    /**
     * @return the appointmentTime
     */
    public Calendar getAppointmentTime() {
        return appointmentTime;
    }
    /**
     * @return the iD
     */
    public String getID() {
        return ID;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @return the purpose
     */
    public String getPurpose() {
        return purpose;
    }
    /**
     * @return the state
     */
    public String getState() {
        return state;
    }
    /**
     * @return the timeAppointmentSet
     */
    public Calendar getTimeAppointmentSet() {
        return timeAppointmentSet;
    }
    /**
     * @return the unavailable
     */
    public List<Unavailability> getUnavailable() {
        return unavailable;
    }

    /**
     * @param appointmentTime the appointmentTime to set
     */
    public void setAppointmentTime(Calendar appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @param purpose the purpose to set
     */
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }
    /**
     * @param timeAppointmentSet the timeAppointmentSet to set
     */
    public void setTimeAppointmentSet(Calendar timeAppointmentSet) {
        this.timeAppointmentSet = timeAppointmentSet;
    }
    /**
     * @param unavailable the unavailable to set
     */
    public void setUnavailable(List<Unavailability> unavailable) {
        this.unavailable = unavailable;
    }
    /**
     * Adds the unavailibility to the running list
     * @param unavailability the unavailability to add
     */
    public void addUnavailability(Unavailability unavailability) {
        if (this.unavailable == null) {
            this.unavailable = new ArrayList<>();
        }
        this.unavailable.add(unavailability);
    }
}