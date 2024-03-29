package com.studmane.nlpserver.service;

import com.studmane.nlpserver.Server;
import com.studmane.nlpserver.service.exception.BadRequestException;
import com.studmane.nlpserver.service.response.MessageResponse;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

import com.studmane.nlpserver.service.DiscourseManager;
import com.studmane.nlpserver.service.exception.ServiceErrorException;
import com.studmane.nlpserver.service.model.Conversation;
import com.studmane.nlpserver.service.model.MessageIntent;
import com.studmane.nlpserver.service.request.AnalysisRequest;
import com.studmane.nlpserver.service.response.AnalysisResponse;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Analysis service handles the processing of incoming text with Stanford coreNLP
 */
public class AnalysisService {
    // this should probably have a sync lock? we don't really want more than 1 instance of coreNLP running at once
    public AnalysisService() {
        
    }

    static {
        // initialize the pipeline
        Properties props = new Properties();
        props.setProperty("annotators","tokenize,ssplit,pos,lemma,ner,depparse,natlog,openie");
//        props.setProperty("sutime.includeRange", "true");
//        props.setProperty("sutime.markTimeRanges", "true");
        // props.setProperty("annotators","tokenize,ssplit,pos,lemma,ner,parse,coref,kbp");

        pipeline = new StanfordCoreNLP(props);
    }

    
    private static final StanfordCoreNLP pipeline;
    private static ReentrantLock lock = new ReentrantLock();

    /**
     * Does all the work in analyzing a message and deciding what to do with it
     * @param req the request object for this service
     * @return the response object for this service
     */
    public AnalysisResponse serve(AnalysisRequest req) 
            throws ServiceErrorException {
        // start by annotating the thing
        Annotation annotation = annotate(req.getMessage());

        Conversation conv = req.getConversation();
        if (conv == null) {
            conv = new Conversation(req.getName());
        }

        Server.logger.log(Level.INFO, String.format("Sentence is: %s", req.getMessage()));

        // run the information extractor with the conversation and the annotation
        // this updates the conversation in place
        InformationExtractor ie = new InformationExtractor();
        List<MessageIntent> messageIntents = ie.updateTemplate(annotation, conv);
        
        // instantiate a discourse manager and a response
        DiscourseManager dm = new DiscourseManager();
        AnalysisResponse resp = new AnalysisResponse();

        // generate a response and store it in the response
        try {
            StringBuilder sb = new StringBuilder();
            for (MessageIntent intent : messageIntents) {
                sb.append(" ");
                sb.append(dm.reply(intent, conv));
            }
            String message = sb.toString();
            resp.setMessage(message);
            Server.logger.log(Level.INFO,String.format("Created response: %s", message));

        } catch (IllegalStateException ex) {
            // also deal with this
            Server.logger.log(Level.SEVERE,"Found illegal conversation state", ex);
            throw new BadRequestException(new MessageResponse("Found illegal conversation state"));
        } catch (IOException ex) {
            Server.logger.log(Level.SEVERE,"Failed to generate resonses.", ex);
            throw new ServiceErrorException(new MessageResponse("Failed to generate responses"));
        }

        // save the number and the old conversation
        resp.setConversation(conv);
        resp.setNumber(req.getNumber());

        return resp;
    }

    /**
     * annotate takes a string and smacks it with coreNLP
     * It uses the following annotators:
     *   tokenize, sentence split, part of speech, lemmatization,
     *   named entity recognition, dependency parsing, natural logic,
     *   information extraction
     * @param input the input string to deal with
     * @return an annotation object generated by coreNLP
     */
    static Annotation annotate(String input) {
        // No more than one process should be allowed to use coreNLP at a time
        AnalysisService.lock.lock();

        // create a new annotation
        Annotation annotation = new Annotation(input);
        
        // annotate
        pipeline.annotate(annotation);

        // unlock and return
        AnalysisService.lock.unlock();
        return annotation;
    }
}
