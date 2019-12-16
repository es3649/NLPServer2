package com.studmane.nlpserver.service;

import com.studmane.nlpserver.service.model.MessageIntent;
import edu.stanford.nlp.pipeline.Annotation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class InformationExtractorTest {
    public InformationExtractorTest() {}

    @Test
    public void askedWhereTest() {
        InformationExtractor ie = new InformationExtractor();
        Annotation annotation = AnalysisService.annotate("Where will that be?");
        assert ie.askedWhere(annotation);
        annotation = AnalysisService.annotate("Where is their office?");
        assert ie.askedWhere(annotation);
        annotation = AnalysisService.annotate("Remind me where that will be?");
        assert ie.askedWhere(annotation);
        annotation = AnalysisService.annotate("Where are we meeting?");
        assert ie.askedWhere(annotation);
        annotation = AnalysisService.annotate("What room will that be in?");
        assert ie.askedWhere(annotation);
        annotation = AnalysisService.annotate("What time will that be?");
        assert !ie.askedWhere(annotation);
    }

    @Test
    public void updateForProcessingTest() {
        InformationExtractor ie = new InformationExtractor();

        Annotation annotation = AnalysisService.annotate("Can I schedule an appointment with bishop?");
        List<MessageIntent> returnList = ie.updateForProcessing(annotation, null);
        assert returnList.contains(MessageIntent.SCHEDULE);

        annotation = AnalysisService.annotate("Can I meet with bishop this week");
        returnList = ie.updateForProcessing(annotation, null);
        assert returnList.contains(MessageIntent.SCHEDULE);

        annotation = AnalysisService.annotate("Hey, I need to see bishop soon");
        returnList = ie.updateForProcessing(annotation, null);
        assert returnList.contains(MessageIntent.SCHEDULE);

        annotation = AnalysisService.annotate("Can I set up a meeting with bishop for me?");
        returnList = ie.updateForProcessing(annotation, null);
        assert returnList.contains(MessageIntent.SCHEDULE);

        annotation = AnalysisService.annotate("I want a sandwich");
        returnList = ie.updateForProcessing(annotation, null);
        assert !returnList.contains(MessageIntent.SCHEDULE);

        annotation = AnalysisService.annotate("Please set the failbit");
        returnList = ie.updateForProcessing(annotation, null);
        assert !returnList.contains(MessageIntent.SCHEDULE);
    }

    @Test
    public void updateForApptProposedTest() {
        InformationExtractor ie = new InformationExtractor();

        Annotation annotation = AnalysisService.annotate("That should be great. Where will that be?");
        List<MessageIntent> returnList = ie.updateForApptProposed(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHERE);

        annotation = AnalysisService.annotate("Yeah, what room?");
        returnList = ie.updateForApptProposed(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHERE);
        assert returnList.contains(MessageIntent.AFFIRMATIVE);

        annotation = AnalysisService.annotate("No, I can't");
        returnList = ie.updateForApptProposed(annotation, null);
        assert returnList.contains(MessageIntent.NEGATIVE);

        annotation = AnalysisService.annotate("For sure. Where will that be?");
        returnList = ie.updateForApptProposed(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHERE);
        assert returnList.contains(MessageIntent.AFFIRMATIVE);

        annotation = AnalysisService.annotate("Wut?");
        returnList = ie.updateForApptProposed(annotation, null);
        assert returnList.contains(MessageIntent.OTHER);
        assert !returnList.contains(MessageIntent.AFFIRMATIVE);
        assert !returnList.contains(MessageIntent.NEGATIVE);
        assert !returnList.contains(MessageIntent.QUERY_WHERE);
    }

    @Test
    public void findThanksTest() {
        InformationExtractor ie = new InformationExtractor();

        Annotation annotation = AnalysisService.annotate("Thanks Eric");
        List<MessageIntent> returnList = ie.findThanks(annotation);
        assert returnList.size() == 1;
        assert returnList.contains(MessageIntent.THANKS);

        annotation = AnalysisService.annotate("Thank you!");
        returnList = ie.findThanks(annotation);
        assert returnList.contains(MessageIntent.THANKS);

        annotation = AnalysisService.annotate("I'd like to thank you for your excellent service");
        returnList = ie.findThanks(annotation);
        assert returnList.contains(MessageIntent.THANKS);
        assert returnList.size() == 1;

        annotation = AnalysisService.annotate("I like to eat apples and bananas and chinese chicken salad. :P");
        returnList = ie.findThanks(annotation);
        assert !returnList.contains(MessageIntent.THANKS);
        assert returnList.size() == 0;
    }

    @Test
    public void updateForApptSetTest() {
        InformationExtractor ie = new InformationExtractor();

        // where
        Annotation annotation = AnalysisService.annotate("Where are we meeting?");
        List<MessageIntent> returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHERE);

        annotation = AnalysisService.annotate("Thanks. What room will that be in?");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHERE);
        // this isn't contained if there is anything else contained
        assert !returnList.contains(MessageIntent.THANKS);

        // when
        annotation = AnalysisService.annotate("Hey, when is my appointment?");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHEN);

        annotation = AnalysisService.annotate("When are we meeting");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHEN);

        annotation = AnalysisService.annotate("What time is the appointment");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.QUERY_WHEN);

        // thanks
        annotation = AnalysisService.annotate("Thanks :)");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.THANKS);

        annotation = AnalysisService.annotate("Thank you!!");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.THANKS);

        // cancel
        annotation = AnalysisService.annotate("Hey, I'm going to have to cancel, sorry :(");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.CANCEL);

        // reschedule
        annotation = AnalysisService.annotate("Hey, can we reschedule?");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.REQUEST_RESCHEDULE);

        // invalid
        // case1
        annotation = AnalysisService.annotate("I won't be able to do it this sunday");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.APPOINTMENT_INVALID);

        // case 2
        annotation = AnalysisService.annotate("I won't be there sunday");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.APPOINTMENT_INVALID);

        annotation = AnalysisService.annotate("I won't be here this week");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.APPOINTMENT_INVALID);

        // case3
        annotation = AnalysisService.annotate("I'm sick so I probably can't make it this week");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.APPOINTMENT_INVALID);

        annotation = AnalysisService.annotate("I just found out I'll be out of town so I can't come in on sunday");
        returnList = ie.updateForApptSet(annotation, null);
        assert returnList.contains(MessageIntent.APPOINTMENT_INVALID);
    }

}