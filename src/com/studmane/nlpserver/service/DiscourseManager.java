package com.studmane.nlpserver.service;

import com.studmane.nlpserver.service.model.Conversation;
import com.studmane.nlpserver.service.model.MessageIntent;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;

import com.studmane.nlpserver.Server;
import com.studmane.nlpserver.service.ScheduleManager;
import com.studmane.nlpserver.service.model.WordLattice;


/**
 * DiscourseManager uses coreNLP annotation information and previously extracted information to generate a response.
 * 
 * Each Conversation is essentially a state machine. (see https://drive.google.com/file/d/1lFISFnhQ_wPcQpdqLnQWy5_dTsdnq6Dy/view?usp=sharing)
 * The discourse manager handles executing state transitions and executing outputs. These outputs take the form of
 * English conversation. The responses are randomly selected from a set of predefined options with formatting.
 */
public class DiscourseManager {
    public DiscourseManager() {
        this.scheduleManager = new ScheduleManager();
    }

    private ScheduleManager scheduleManager;

    /**
     * Using an existing conversation template, generate a textual response
     * to the individual to further the conversation
     * @param intent a generalized summary of the message contents
     * @param conv an updated convesation model with data about the conversation so far
     * @return a string as a proposed response to the conversation
     */
    public String reply(MessageIntent intent, Conversation conv) 
            throws IOException, IllegalStateException {
        // if we query where at any point, then tell them where
        if (intent == MessageIntent.QUERY_WHERE) {
            return WordLattice.fromFile("where.lat")
                .generate(conv.getAppointmentTime(), conv.getName());
        }
        // switch the state
        switch (conv.getState()) {    
        case Conversation.STATE_PROCESSING:
            // we are in processing state
            // propose a time block
            if (intent == MessageIntent.SCHEDULE) {
                return proposeTime(conv, false);
            }
            break;
        case Conversation.STATE_APPT_PROPOSED:
            // we previously proposed an appointment, now we have a response
            // resolve that
            return resolveProposal(intent, conv);
        case Conversation.STATE_ARCHIVED:
            if (intent == MessageIntent.THANKS) {
                return WordLattice.fromFile("yourewelcome.lat").generate(conv.getAppointmentTime(), conv.getName());
            } else {
                return null;
            }
        case Conversation.STATE_RESCHEDULING:
            // then we are in the process of rescheduling, deal with that
            return reschedule(intent, conv);
        case Conversation.STATE_SET:
            // We received a message after the appt was set
            return set(intent, conv);
        default:
            throw new IllegalStateException(String.format("Got state %s", conv.getState()));
        }
        
        return null;
    }

    /**
     * S0
     * 
     * Proposes a time for a meeting
     * @param conv the relevant conversation object
     * @param correction Are we recovering from a prevous failed proposal?
     * @return the English sentence proposing the appointment time as a string
     */
    private String proposeTime(Conversation conv, boolean correction)
            throws IOException {
        // propose a time block
        Calendar appt = scheduleManager.getApptSlot();
        conv.setAppointmentTime(appt);
        // transition to a the message proposed state
        conv.setState(Conversation.STATE_APPT_PROPOSED);

        // generate the message
        if (correction) {
            return WordLattice.fromFile("correction.lat")
                .generate(conv.getAppointmentTime(), conv.getName());
        } else {
            return WordLattice.fromFile("propose.lat")
                .generate(conv.getAppointmentTime(), conv.getName());
        }
    }

    /**
     * S1
     * 
     * Handling a response to a proposed meeting time
     * @param intent the generalized meaning of the message
     * @param conv the conversation of which this message is a part
     * @return the English response to the message, as a string
     */
    private String resolveProposal(MessageIntent intent, Conversation conv)
            throws IOException, IllegalStateException {
        switch (intent) {
        case AFFIRMATIVE:
            // save time
            conv.setState(Conversation.STATE_SET);
            // generate message
            return WordLattice.fromFile("apptset.lat")
                .generate(conv.getAppointmentTime(), conv.getName());

        case NEGATIVE:
            // block out time
            scheduleManager.invalidateBlock(conv.getAppointmentTime(), conv);
            // repropose
            return proposeTime(conv, true);

        case CANCEL:
            // archive the conversation
            conv.setState(Conversation.STATE_ARCHIVED);

            // signoff
            return WordLattice.fromFile("signoff.lat")
                .generate(conv.getAppointmentTime(), conv.getName());

        default:
            
            Server.logger.log(Level.SEVERE, String.format("Got signal %s", intent));
            throw new IllegalStateException("Reached an illegal action out of S1");
        }
    }

    /**
     * S2
     * 
     * Handle messages coming in after an appointment is set
     * @param intent the generalized intent of the message
     * @param conv 
     * @return
     */
    private String set(MessageIntent intent, Conversation conv) 
            throws IOException, IllegalStateException {
        switch (intent) {
        case QUERY_WHEN:
            // construct the response using conv.getAppointmentTime();
            return WordLattice.fromFile("when.lat")
                .generate(conv.getAppointmentTime(), conv.getName());

        case CANCEL:
            // cancel
            scheduleManager.removeAppt(conv.getAppointmentTime());
            conv.setState(Conversation.STATE_RESCHEDULING);

            return WordLattice.fromFile("signoff.lat")
                .generate(conv.getAppointmentTime(), conv.getName());

        case APPOINTMENT_INVALID:
            // start rescheduling
            scheduleManager.invalidateBlock(conv.getAppointmentTime(), conv);
            conv.setState(Conversation.STATE_RESCHEDULING);

            // gen response
            return WordLattice.fromFile("reschedule.lat")
                .generate(conv.getAppointmentTime(), conv.getName());

        case REQUEST_RESCHEDULE:
            // then they already requested to reschedule the appointment, 
            // no need to offer to reschedule
            scheduleManager.invalidateBlock(conv.getAppointmentTime(), conv);
            return proposeTime(conv, true);
            
        case THANKS:
            // then thank the person
            return WordLattice.fromFile("yourewelcome.lat")
                .generate(conv.getAppointmentTime(), conv.getName());
            
        default:
            Server.logger.log(Level.SEVERE, String.format("Got signal %s", intent));
            throw new IllegalStateException("Reached an illegal action out of S2");
        }
    }

    /**
     * S3
     * 
     * How to manage responses which come in while we are in 
     * a state of rescheduling the appoinrment
     * @param intent the generalized content of the message
     * @param conv the conversation of which this message is a part
     * @return an English sentence expressing the action of the computer, as a string
     */
    private String reschedule(MessageIntent intent, Conversation conv) 
            throws IOException, IllegalStateException {
        switch (intent) {
        case AFFIRMATIVE:
            // schedule an appointment from scratch
            return proposeTime(conv, true);

        case CANCEL:        // proceed to NEGATIVE
        case NEGATIVE:
            // archive, probably still do a message
            conv.setState(Conversation.STATE_ARCHIVED);

            // signoff
            return WordLattice.fromFile("yourewelcome.lat")
                .generate(conv.getAppointmentTime(), conv.getName());

        default:
            Server.logger.log(Level.SEVERE, String.format("Got signal %s", intent));
            throw new IllegalStateException("Reached an illegal action out of S3");
        }
    }
}