/**
 * The model package declares data types which are of integral importance to the function of the project
 * These classes are mostly used for information storage, not functionality.
 * 
 * The Conversation class carries data about a conversation, including the state, which is to be used in
 * the DiscourseManager, and appointment times and unavailabilities, to be used for scheduling.
 * 
 * The MessageIntent enum declares basic categories of messages. More advanced informaiton will be
 * extracted from messages by coreNLP, but this basic categorization helps simplify some operations.
 */
package com.studmane.nlpserver.service.model;