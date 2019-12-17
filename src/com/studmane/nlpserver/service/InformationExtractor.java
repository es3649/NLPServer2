package com.studmane.nlpserver.service;

import java.util.*;
import java.util.logging.Level;

import com.studmane.nlpserver.Server;
import com.studmane.nlpserver.service.exception.BadRequestException;
import com.studmane.nlpserver.service.model.Conversation;
import com.studmane.nlpserver.service.model.MessageIntent;
import com.studmane.nlpserver.service.response.MessageResponse;
import edu.stanford.nlp.pipeline.Annotation;
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
            return updateForApptSet(annotation, conversation);
        case Conversation.STATE_RESCHEDULING:
            MessageIntent[] arr = {findAffirmativeOrNegative(annotation)};
            return new ArrayList<>(Arrays.asList(arr));
        case Conversation.STATE_ARCHIVED:
            return updateForArchived(annotation);
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
    List<MessageIntent> updateForProcessing(Annotation annotation, Conversation conversation) {
        // do they actually want an appointment, or something else?
        List<MessageIntent> returnList = new ArrayList<>();

        // get the root of the depparse and explore a little bit...
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
        //    Tree semanticGraph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
            SemanticGraph semGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);

            // get the vertices
            for (IndexedWord word : semGraph.vertexSet()) {
                // check for lemmas we want
                List<Pair<GrammaticalRelation, IndexedWord>> edges = semGraph.childPairs(word);
                switch (word.lemma()) {
                    case "schedule":
                        // todo assert that is a verb
                    case "meet":
                        // we can check further down the graph, but we probably good
                        returnList.add(MessageIntent.SCHEDULE);
                        Server.logger.log(Level.INFO, "Found SCHEDULE intent for 'meet' or 'schedule'");
                        break;
                    case "see":
                        // should probably check further down the map:
                        // 'see' -(dobj)-> 'bishop'
                        for (Pair<GrammaticalRelation, IndexedWord> edge : edges) {
                            if (edge.second.value().equalsIgnoreCase("bishop")){
                                returnList.add(MessageIntent.SCHEDULE);
                                Server.logger.log(Level.INFO, "Found SCHEDULE intent for 'see bishop'");
                            }
                        }
                        break;
                    case "set":
                        // check for 'set' -(compound:prt)-> 'up'
                        for (Pair<GrammaticalRelation, IndexedWord> edge : edges) {
                            if (edge.second.value().equalsIgnoreCase("up")
                                    && edge.first.getShortName().equalsIgnoreCase("compound:prt")) {
                                Server.logger.log(Level.INFO, "Found SCHEDULE intent for 'set up'");
                                returnList.add(MessageIntent.SCHEDULE);
                            }
                        }

                    default:
                        // in all cases consider checking for dobj Bishop or dobj appointment or interview
//                        Server.logger.log(Level.INFO, "SCHEDULE intent not found");
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

        if (returnList.size() == 0) {
            Server.logger.log(Level.INFO, "No intent found");
        }

        return returnList;
    }


    /**
     * Gets the intent based on the annotations
     * @param annotation the annotations to search
     * @param conversation the related conversation object
     * @return a list of intents parsed from this annotation
     */
    List<MessageIntent> updateForApptProposed(Annotation annotation, Conversation conversation) {
        // look for a positive or negative
        MessageIntent m = findAffirmativeOrNegative(annotation);
        List<MessageIntent> r = new ArrayList<>();
        r.add(m);

        // did they ask where?
        if (askedWhere(annotation)) {
            r.add(MessageIntent.QUERY_WHERE);
        }

        // TODO later mess with dates and see if they suggested something or if they blocked out a time
        if (r.size() == 0) {
            Server.logger.log(Level.INFO, "No intent found");
        }

        return r;
    }

    /**
     * Gets the intent based on the annotations assuming we are in the "appt set" state
     * @param annotation the annotated message to look over
     * @param conversation the conversation of which this message is a part
     * @return a list of intents encountered
     */
    List<MessageIntent> updateForApptSet(Annotation annotation, Conversation conversation) {
        List<MessageIntent> response = new ArrayList<>();

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            SemanticGraph semGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);
            Collection<IndexedWord> vertices = semGraph.vertexSet();

            for (IndexedWord vertex : vertices) {
                List<Pair<GrammaticalRelation,IndexedWord>> children = semGraph.childPairs(vertex);
                // when should be easy:
                // case 1: when is
                //    when <-advmod- be(root)
                if (vertex.lemma().equalsIgnoreCase("when")) {
//                    for (Pair<GrammaticalRelation, IndexedWord> parent : semGraph.parentPairs(vertex)) {
//                        if (parent.first.getShortName().equalsIgnoreCase("advmod") && parent.second.lemma().equalsIgnoreCase("be")) {
                            response.add(MessageIntent.QUERY_WHEN);
                            Server.logger.log(Level.INFO, "Found QUERY_WHERE intent for 'where be'");
//                        }
//                    }
                }

                // case 2: what time:
                //    what <-det- time
                if (vertex.lemma().equalsIgnoreCase("time")) {
                    for (Pair<GrammaticalRelation, IndexedWord> child : children) {
                        if (child.first.getShortName().equalsIgnoreCase("det") && child.second.lemma().equalsIgnoreCase("what")) {
                            response.add(MessageIntent.QUERY_WHEN);
                            Server.logger.log(Level.INFO, "Found QUERY_WHERE intent for 'what time'");
                        }
                    }
                }


                // check for a reschedule or a not make it request
                //   ?? -cop-> be
                //      -neg-> ??
                //   be -advmod-> [there,here]
                //      -neg-> ??
                //   [make,come] -neg-> ??

                boolean foundCop = false;
                boolean foundNeg = false;
                boolean foundBeAdvmod = false;

                String[] acceptableNegatablesArr = {"make","come"};
                Set<String> acceptableNegatables= new HashSet<>(Arrays.asList(acceptableNegatablesArr));

                for (Pair<GrammaticalRelation,IndexedWord> child : children) {
                    // covers   -neg-> ??
                    if (child.first.getShortName().equalsIgnoreCase("neg")) {
                        if (acceptableNegatables.contains(vertex.lemma().toLowerCase())) {
                            // covers [make,com] -neg-> ??
                            response.add(MessageIntent.APPOINTMENT_INVALID);
                            Server.logger.log(Level.INFO, "Found APPOINTMENT_INVALID intent for <neg> and '[meet,come]'");
                            break;
                        } else {
                            foundNeg = true;
                        }
                    // covers   -cop-> be
                    } else if (child.first.getShortName().equalsIgnoreCase("cop") && child.second.lemma().equalsIgnoreCase("be")) {
                        foundCop = true;
                    // covers   be -advmod-> there
                    } else if (vertex.lemma().equalsIgnoreCase("be") &&
                            child.first.getShortName().equalsIgnoreCase("advmod") &&
                            (child.second.lemma().equalsIgnoreCase("there") ||
                                    child.second.lemma().equalsIgnoreCase("here"))) {
                        foundBeAdvmod = true;
                    }

                    // which conditions have we filled?
                    if ((foundCop && foundNeg) ||
                            (foundBeAdvmod && foundNeg)) {
                        response.add(MessageIntent.APPOINTMENT_INVALID);
                        Server.logger.log(Level.INFO, "Found APPOINTMENT_INVALID intent");
                        break;
                    }
                }

                // handle the exact words reschedule and cancel
                if (vertex.lemma().equalsIgnoreCase("cancel")) {
                    Server.logger.log(Level.INFO, "Found CANCEL intent");
                    response.add(MessageIntent.CANCEL);
                } else if (vertex.lemma().equalsIgnoreCase("reschedule")) {
                    response.add(MessageIntent.REQUEST_RESCHEDULE);
                    Server.logger.log(Level.INFO, "Found REQUEST_RESCHEDULE intent");
                }
            }

            // the acceptable nouns upon which an appointment can be rescheduled
            String[] acceptableReschedulesArr = {"day","time","week"};
            Set<String> acceptableRechedules = new HashSet<>(Arrays.asList(acceptableReschedulesArr));

            // look only over roots
            // find some root -> [day,time,week] -> [other,another]
            boolean done = false;
            for (IndexedWord root : semGraph.getRoots()) {
                for (Pair<GrammaticalRelation, IndexedWord> child : semGraph.childPairs(root)) {
                    if (acceptableRechedules.contains(child.second.lemma().toLowerCase())) {
                        for (Pair<GrammaticalRelation, IndexedWord> grandchild : semGraph.childPairs(child.second)) {
                            if (grandchild.second.lemma().equalsIgnoreCase("other") || grandchild.second.lemma().equalsIgnoreCase("another")) {
                                response.add(MessageIntent.REQUEST_RESCHEDULE);
                                Server.logger.log(Level.INFO, "Found REQUEST_RESCHEDULE intent for '[other,another]' and '[day,time,week]'");
                                done = true;
                                break;
                            }
                        }
                    }
                    if (done) break;
                }
                if (done) break;
            }
        }




        // check if they (also) asked where
        if (askedWhere(annotation)) {
            response.add(MessageIntent.QUERY_WHERE);
        }

        if (response.size() == 0) {
            response = findThanks(annotation);
        }

        if (response.size() == 0) {
            Server.logger.log(Level.INFO, "No intent found");
        }

        return response;
    }

    /**
     * Looks for the word thank or thanks.
     * @param annotation the sentence to extract from
     * @return null or the "thanks" intent in a lost be itself
     */
    private List<MessageIntent> updateForArchived(Annotation annotation) {
        return findThanks(annotation);
    }

    /**
     * Looks for the word thank or thanks.
     * @param annotation the sentence to extract from
     * @return null or the "thanks" intent in a lost be itself
     */
    List<MessageIntent> findThanks(Annotation annotation) {
        Collection<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

        MessageIntent[] arr = {MessageIntent.THANKS};
        List<MessageIntent> ret = new ArrayList<>(Arrays.asList(arr));

        for (CoreMap sentence : sentences) {
            SemanticGraph semGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);

            for (IndexedWord vertex : semGraph.vertexSet()) {
//                Server.logger.log(Level.FINEST, String.format("examining lemma '%s'", vertex.lemma()));
                if (vertex.lemma().equalsIgnoreCase("thank") || vertex.lemma().equalsIgnoreCase("thanks")) {
                    Server.logger.log(Level.INFO, "Found THANKS intent");
                    return ret;
                }
            }
        }
        Server.logger.log(Level.INFO, "No intent found");
        return new ArrayList<>();
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
                    Server.logger.log(Level.INFO, "Found AFFIRMATIVE intent");
                    return MessageIntent.AFFIRMATIVE;
                } else if (negativeValues.contains(root.lemma().toLowerCase())) {
                    // if it's negative, return it
                    Server.logger.log(Level.INFO, "Found AFFIRMATIVE intent");
                    return MessageIntent.NEGATIVE;
                }

                List<Pair<GrammaticalRelation, IndexedWord>> edges = semGraph.childPairs(root);

                // look for some obvious affirmative or negative triggers, on discourse edges
                for (Pair<GrammaticalRelation, IndexedWord> edge : edges) {
                    if (edge.first.getShortName().equalsIgnoreCase("discourse")) {
                        if (affirmativeValues.contains(edge.second.lemma().toLowerCase())) {
                            Server.logger.log(Level.INFO, "Found AFFIRMATIVE intent");
                            return MessageIntent.AFFIRMATIVE;
                        } else if (negativeValues.contains(edge.second.lemma().toLowerCase())) {
                            Server.logger.log(Level.INFO, "Found NEGATIVE intent");
                            return MessageIntent.NEGATIVE;
                        }
                    }
                }
            }
        }
        Server.logger.log(Level.INFO, "No intent found");
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
                List<Pair<GrammaticalRelation, IndexedWord>> parents = semGraph.parentPairs(vertex);
                List<Pair<GrammaticalRelation, IndexedWord>> children = semGraph.childPairs(vertex);
                // if the lemma is "where"
                if (vertex.lemma().equalsIgnoreCase("where")) {
                    // look through the parents
//                    for (Pair<GrammaticalRelation, IndexedWord> parent : parents) {
//                        // if the relation is advmod and the parent is some form of be, then they asked where
//                        if (parent.second.lemma().equalsIgnoreCase("be") && parent.first.getShortName().equalsIgnoreCase("advmod")) {
                            Server.logger.log(Level.INFO, "Found QUERY_WHERE intent for 'where be'");
                            return true;
//                        }
//                    }
                }

                if (vertex.lemma().equalsIgnoreCase("room")) {
                    for (Pair<GrammaticalRelation, IndexedWord> child : children) {
                        // if the relation is advmod and the parent is some form of be, then they asked where
                        if (child.first.getShortName().equalsIgnoreCase("det")) {
                            String lemma = child.second.lemma();
                            if (lemma.equalsIgnoreCase("what") || lemma.equalsIgnoreCase("which")) {
                                Server.logger.log(Level.INFO, "Found QUERY_WHERE intent for '[what,which] room'");
                                return true;
                            }
                        }
                    }
                }

            }
        }
        Server.logger.log(Level.INFO, "No intent found");
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