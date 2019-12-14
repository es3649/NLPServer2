package com.studmane.nlpserver.service;

import java.util.Calendar;
import java.util.List;

import com.studmane.nlpserver.service.model.Conversation;

public class ScheduleManager {
    public ScheduleManager() {}

    public static final String BISHOP = "bishop";
    public static final String BISHOPRIC = "bishopric";

    /**
     * Invalidates a block of time in the conversation object
     * @param date the time to block out
     * @param conv the conversation from which time will be blocked out
     */
    public void invalidateBlock(Calendar date, Conversation conv) {
        assert false;
    }

    /**
     * Reserves an appointment
     * @param date the date and time of the appointment
     * @param name the name of the person the appt is for
     * @param purpose the reason for the appointment
     */
    public void scheduleAppt(Calendar date, String name, String purpose) {
        assert false;
    }

    /**
     * unschedules an appointment for a person
     * @param date the date and time of the appointment to remove
     */
    public void removeAppt(Calendar date) {
        assert false;
    }

    /**
     * given a list of appointment options, chooses the optimal one
     * @param options the list of appointments to choose from
     * @return the "optimal" choice from these options
     */
    private Calendar chooseOptimal(List<Calendar> options) {
        assert false;
        return null;
    }

    /**
     * Based on the purpose of the appointment, decides
     * who they need to meet with:
     *  Bishop:
     *  - Interview
     *  - Live Recommend
     *  - Expired Recommend
     * 
     *  Bishopric:
     *  - calling
     *  - temple recommend
     *  - Ecclesiastical endorsement
     *  - Set apart
     */
    public String resolvePersonByPurpose(String purpose) {
        assert false;
        return null;
    }

    /**
     * Gets the best available time.
     * @return the best available appointment slot
     */
    public Calendar getApptSlot() {
        assert false;
        return null;
    }

    /**
     * Get apointment availibilities on the given date
     * Calls the getAvailabilities with exceptions=null
     * 
     * @param date the date for which to query availabilities
     * @return a list of all available slots
     */
    public List<Calendar> getAvailibilities(Calendar date, String person) {
        return getAvailabilities(date, person, null);
    }

    /**
     * Get apointment availibilities on the given date.
     * Gets the list of all appts, then if exceptions is not null,
     * it filters the raw list so as not to include any values from the exceptions
     * 
     * @param date the date for which to query availabilities
     * @param exceptions a list of time which definitely won't work.
     * @return a list of all acceptable and available slots
     */
    public List<Calendar> getAvailabilities(Calendar date, String person, List<Calendar> exceptions) {
        assert false;
        return null;
    }
}