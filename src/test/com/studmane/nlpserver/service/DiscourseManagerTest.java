package com.studmane.nlpserver.service;

import org.junit.*;

import com.studmane.nlpserver.service.model.Conversation;
import com.studmane.nlpserver.service.model.MessageIntent;
import com.studmane.nlpserver.service.DiscourseManager;

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

        dm.reply(MessageIntent.AFFIRMATIVE, conv);

        assertEquals(conv.getState(), Conversation.STATE_SET);
    }

    @Test
    public void TestS1() {}

    @Test
    public void TestS2() {}

    @Test
    public void TestS3() {}
}