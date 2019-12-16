package com.studmane.nlpserver.service;

import org.junit.*;

import com.studmane.nlpserver.service.model.Conversation;
import com.studmane.nlpserver.service.model.MessageIntent;
import com.studmane.nlpserver.service.DiscourseManager;

import java.util.Calendar;

import static org.junit.Assert.*;

public class DiscourseManagerTest {
    public DiscourseManagerTest() {}

    private DiscourseManager dm = new DiscourseManager();

    @Test
    public void TestS0() throws Exception {
        Conversation conv = new Conversation("Jimmy John");

        dm.reply(MessageIntent.OTHER, conv);

        assertEquals (conv.getState(), Conversation.STATE_PROCESSING);

        dm.reply(MessageIntent.SCHEDULE, conv);

        assertEquals (conv.getState(), Conversation.STATE_APPT_PROPOSED);
    }

    @Test
    public void TestS1() throws Exception {
        Conversation conv = new Conversation("Jimmy John");
        dm.reply(MessageIntent.SCHEDULE, conv);

        Calendar old = conv.getAppointmentTime();
        // make sure that the next calendar is different by at least a second
        Thread.sleep(1000);

        // transition
        dm.reply(MessageIntent.NEGATIVE, conv);
        assertEquals(conv.getState(), Conversation.STATE_APPT_PROPOSED);
        // appt time should have updated
        assertNotEquals(old,conv.getAppointmentTime());

        // try affirmative transition
        dm.reply(MessageIntent.AFFIRMATIVE, conv);
        assertEquals(conv.getState(), Conversation.STATE_SET);

    }

    @Test
    public void TestS2() throws Exception {
        Conversation conv = new Conversation("Jimmy John");
        dm.reply(MessageIntent.SCHEDULE, conv);
        dm.reply(MessageIntent.AFFIRMATIVE, conv);

        // try when
        dm.reply(MessageIntent.QUERY_WHEN, conv);
        assertEquals(Conversation.STATE_SET, conv.getState());

        // cancel
        dm.reply(MessageIntent.CANCEL, conv);
        assertEquals(Conversation.STATE_RESCHEDULING, conv.getState());

        // reset
        conv = new Conversation("Jimmy John");
        dm.reply(MessageIntent.SCHEDULE, conv);
        dm.reply(MessageIntent.AFFIRMATIVE, conv);

        // invalidate appt
        dm.reply(MessageIntent.APPOINTMENT_INVALID, conv);
        assertEquals(Conversation.STATE_RESCHEDULING, conv.getState());

        // reset
        conv = new Conversation("Jimmy John");
        dm.reply(MessageIntent.SCHEDULE, conv);
        dm.reply(MessageIntent.AFFIRMATIVE, conv);

        // reschedule
        Calendar old = conv.getAppointmentTime();
        Thread.sleep(1000);
        dm.reply(MessageIntent.REQUEST_RESCHEDULE, conv);
        assertEquals(conv.getState(),Conversation.STATE_APPT_PROPOSED);
        assertNotEquals(old,conv.getAppointmentTime());
    }

    @Test
    public void TestS3() throws Exception {
        Conversation conv = new Conversation("Jimmy John");
        dm.reply(MessageIntent.SCHEDULE, conv);
        dm.reply(MessageIntent.AFFIRMATIVE, conv);
        dm.reply(MessageIntent.APPOINTMENT_INVALID, conv);

        // affirmative
        Calendar old = conv.getAppointmentTime();
        Thread.sleep(1000);
        dm.reply(MessageIntent.AFFIRMATIVE,conv);
        assertEquals(Conversation.STATE_APPT_PROPOSED,conv.getState());
        assertNotEquals(old,conv.getAppointmentTime());

        //reset
        conv = new Conversation("Jimmy John");
        dm.reply(MessageIntent.SCHEDULE, conv);
        dm.reply(MessageIntent.AFFIRMATIVE, conv);
        dm.reply(MessageIntent.APPOINTMENT_INVALID, conv);

        // negative
        dm.reply(MessageIntent.NEGATIVE, conv);
        assertEquals(Conversation.STATE_ARCHIVED, conv.getState());
    }
}