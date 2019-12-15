package com.studmane.nlpserver.service;

import edu.stanford.nlp.pipeline.Annotation;
import org.junit.Test;

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
        annotation = AnalysisService.annotate("What time will that be?");
        assert !ie.askedWhere(annotation);
    }
}