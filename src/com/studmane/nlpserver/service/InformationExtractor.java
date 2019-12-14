package com.studmane.nlpserver.service;

import java.util.*;

import com.google.protobuf.Message;
import com.studmane.nlpserver.service.exception.BadRequestException;
import com.studmane.nlpserver.service.model.Conversation;
import com.studmane.nlpserver.service.model.MessageIntent;
import com.studmane.nlpserver.service.response.MessageResponse;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.util.Pair;

/**
 * InformationExtractor takes coreNLP annotation information and
 * attempts to extract relevant information which can be queried
 * using the Extractor's public methods
 */
public class InformationExtractor {

    /**
     * constructor for an information extractor
     */
    public InformationExtractor() {}

    /**
     * Extracts information about the message based on the annotation, then updates the information
     * in the conversation object accorginly, the one thing it does not do is mess with the conversation state.
     * The state is to be modified by the DiscourseManager based on the returned MessageIntent and the current state.
     * 
     * @param annotation a edu.stanford.nlp.pipeline.Annotation with annotations for
     *      a given string
     * @param conversation a conversation model holding memory about the conversation.
     *      this conversation object will be updated based on 
     * @return a MessageIntent indicating the intent of the message
     */
    public List<MessageIntent> updateTemplate(Annotation annotation, Conversation conversation) throws BadRequestException {
//        List<MessageIntent> result = new ArrayList<>();

        // evaluate unavailabilities per date recognized by the NER tagger

        // use a state based structure I think
        switch (conversation.getState()) {
        case Conversation.STATE_PROCESSING:
            return updateForProcessing(annotation, conversation);
        case Conversation.STATE_APPT_PROPOSED:
            return updateForApptProposed(annotation, conversation);
        case Conversation.STATE_SET:
        case Conversation.STATE_RESCHEDULING:
        case Conversation.STATE_ARCHIVED:
            return new ArrayList<>();
        default:
            throw new BadRequestException(new MessageResponse("Illegal conversation state: "+conversation.getState()));
        }
    }

    /**
     * Do parses specifically for conversations which are in the processing state.
     * @param annotation the coreNLP annotation that we just produced
     * @param conversation the conversation to update
     * @return a list of MessageIntents which describe the intent of this message
     */
    private List<MessageIntent> updateForProcessing(Annotation annotation, Conversation conversation) {
        // do they actually want an appointment, or something else?
        List<MessageIntent> returnList = new ArrayList<>();

        // get the root of the depparse and explore a little bit...
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
        //    Tree semanticGraph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
            SemanticGraph semGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);

            // get the roots
            Collection<IndexedWord> roots = semGraph.getRoots();
            for (IndexedWord word : roots) {
                // check for lemmas we want
                List<Pair<GrammaticalRelation, IndexedWord>> edges = semGraph.childPairs(word);
                switch (word.lemma()) {
                    case "schedule":
                    case "meet":
                        // we can check further down the graph, but we probably good
                        returnList.add(MessageIntent.SCHEDULE);
                        break;
                    case "see":
                        // should probably check further down the map:
                        // 'see' -(dobj)-> 'bishop'
                        for (Pair<GrammaticalRelation, IndexedWord> edge : edges) {
                            if (edge.second.value().equalsIgnoreCase("bishop")){
                                returnList.add(MessageIntent.SCHEDULE);
                            }
                        }
                        break;
                    case "set":
                        // check for 'set' -(compound:prt)-> 'up'
                        for (Pair<GrammaticalRelation, IndexedWord> edge : edges) {
                            if (edge.second.value().equalsIgnoreCase("up")
                                    && edge.first.getShortName().equalsIgnoreCase("compound:prt")) {
                                returnList.add(MessageIntent.SCHEDULE);
                            }
                        }

                    default:
                        // in all cases consider checking for dobj Bishop or dobj appointment or interview
                }

                // TODO come back to dates later.
                // check the NER tag on the word, if it's a date, then let's take a closer look
//                String nerTag = word.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//                if (word.ner().equalsIgnoreCase("DATE")) {
//                    // TODO what does this date mean??
//                    // is it a recommendation, or a blackout?, check the dependents
//                    // if governor is work, then black this out,
//                    // if it's meet, see, set up, etc then propose it (or resolve it to a single day and propose it
//                }

            }
        }

        return returnList;
    }


    /**
     * Gets the intent based on the annotations
     * @param annotation the annotations to search
     * @param conversation the related conversation object
     * @return a list of intents parsed from this annotation
     */
    private List<MessageIntent> updateForApptProposed(Annotation annotation, Conversation conversation) {
        // look for a positive or negative
        MessageIntent m = findAffirmativeOrNegative(annotation);
        List<MessageIntent> r = new ArrayList<>();
        r.add(m);

        // did they ask where?
        if (askedWhere(annotation)) {
            r.add(MessageIntent.QUERY_WHERE);
        }

        // TODO later mess with dates and see if they suggested something or if they blocked out a time
        return r;
    }

    /**
     * Searched the annotation for an affirmative of negative response
     * @param annotation the annotated sentence to search
     * @return either MessageIntent.AFFIRMATIVE of MessageIntent.NEGATIVE
     */
    private MessageIntent findAffirmativeOrNegative(Annotation annotation) {
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

        // construct some sets for checking positive and negative responses
        String[] pos = {"yeah", "yup", "yep", "sure", "yes"};
        String[] neg = {"no", "nope", "nah"};
        Set<String> affirmativeValues = new ArraySet<>(Arrays.asList(pos));
        Set<String> negativeValues = new ArraySet<>(Arrays.asList(neg));

        // start looking through the sentences
        for (CoreMap sentence : sentences) {

            // get the sem graph and its roots
            SemanticGraph semGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);

            for (IndexedWord root : semGraph.getRoots()) {
                if (affirmativeValues.contains(root.lemma().toLowerCase())) {
                    // if it's positive, return it
                    return MessageIntent.AFFIRMATIVE;
                } else if (negativeValues.contains(root.lemma().toLowerCase())) {
                    // if it's negative, return it
                    return MessageIntent.NEGATIVE;
                }

                List<Pair<GrammaticalRelation, IndexedWord>> edges = semGraph.childPairs(root);

                // look for some obvious affirmative or negative triggers, on discourse edges
                for (Pair<GrammaticalRelation, IndexedWord> edge : edges) {
                    if (edge.first.getShortName().equalsIgnoreCase("discourse")) {
                        if (affirmativeValues.contains(edge.second.lemma().toLowerCase())) {
                            return MessageIntent.AFFIRMATIVE;
                        } else if (negativeValues.contains(edge.second.lemma().toLowerCase())) {
                            return MessageIntent.NEGATIVE;
                        }
                    }
                }
            }
        }

        return MessageIntent.OTHER;
    }

    /**
     * determines if the user asked "where" in the sentence
     * @param annotation the annotation to search for the query
     * @return the truthiness of "did they ask where?"
     */
    boolean askedWhere(Annotation annotation) {
        // loop over the sentences
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // get the semantic graph
            SemanticGraph semGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);

            // loop over the vertices in the sentence
            Set<IndexedWord> vertices = semGraph.vertexSet();
            for (IndexedWord vertex : vertices) {
                // if the lemma is "where"
                if (vertex.lemma().equalsIgnoreCase("where")) {
                    // look through the parents
                    List<Pair<GrammaticalRelation, IndexedWord>> parents = semGraph.parentPairs(vertex);
                    for (Pair<GrammaticalRelation, IndexedWord> parent : parents) {
                        // if the relation is advmod and the parent is some form of be, then they asked where
                        if (parent.second.lemma().equalsIgnoreCase("be") && parent.first.getShortName().equalsIgnoreCase("advmod")) {
                            return true;
                        }
                    }
                }

            }
        }
        return false;
    }


    // coreNLP can resolve this itself with the NER parser
    // /**
    //  * Gets "next wednesday"
    //  * @param after the reference starting date
    //  * @return The Wednesday most closely following the given date
    //  */
    // public static Calendar nextWednesday(Calendar after) {
    //     Calendar start = new GregorianCalendar(after.get(Calendar.YEAR), 
    //             after.get(Calendar.MONTH), after.get(Calendar.DAY_OF_MONTH) );

    //     while (start.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY) {
    //         start.add(Calendar.DAY_OF_WEEK, -1);
    //     }

    //     Calendar end = (Calendar) start.clone();
    //     end.add(Calendar.DAY_OF_MONTH, 7);

    //     return end;
    // }

    // /**
    //  * Gets "next sunday"
    //  * @param after the reference starting date
    //  * @return the sunday most closely folloring the given date
    //  */
    // public static Calendar nextSunday(Calendar after) {

    //     Calendar start = new GregorianCalendar(after.get(Calendar.YEAR), 
    //             after.get(Calendar.MONTH), after.get(Calendar.DAY_OF_MONTH) );

    //     while (start.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
    //         start.add(Calendar.DAY_OF_WEEK, -1);
    //     }

    //     Calendar end = (Calendar) start.clone();
    //     end.add(Calendar.DAY_OF_MONTH, 7);

    //     return end;
    // }
    
}