package com.studmane.nlpserver.service.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.studmane.nlpserver.Server;
import edu.stanford.nlp.util.StringUtils;

public class WordLattice {
    private String root;
    private Map<String, LatticeNode> lattice;

    private static final String LATTICE_LOC = "./NLPServer2/libs/lattices/";
    private static final Random generator = new Random(System.currentTimeMillis());

    private WordLattice(String latticeData) {}

    /**
     * Generates a WordLattice from json contained in a file
     * @param filename the name of the file with the lattice
     * @return the WordLattice instance described in the file
     */
    public static WordLattice fromFile(String filename) 
            throws IOException {
        Server.logger.log(Level.INFO, String.format("Generating from %s", filename));
        // read teh bytes from the file
        File file = new File(LATTICE_LOC + filename);
        String json = new String(Files.readAllBytes(file.toPath()));

        // prepare the deserializer
        Gson gson = new Gson();
        TypeToken<WordLattice> typTok = new TypeToken<WordLattice>() {};
        
        // // deserialize the json into a WordLattice
        WordLattice wl = gson.fromJson(json, typTok.getType());

        // normalize the weights in the lattice
        wl.assertValid();
        wl.normalize();
        wl.assertValid();
        return wl;
    }

    /**
     * Use randomness to traverse the lattice and build text
     * 
     * It takes a couple parameters so that it can encode relevant information
     * and act personable
     * @param date the date of the appointment
     * @param name the name of the person
     * @return a sentence generated by traversing the lattice, then performing
     *      appropriate formatting using the given name and date
     */
    public String generate(Calendar date, String name) {
        // declare a string builder
        StringBuilder sb = new StringBuilder();

        // start with the root node of the lattice
        LatticeNode cur = lattice.get(root);
        // sb.append(cur.v);

        // as long as there is a place to transition to...
        while (!cur.to.isEmpty()) {
            // move to the next state
            double rand = generator.nextDouble();
            // System.out.println(rand);
            
            // do a manual multinomial distribution (this is why things really need to be normalized)
            for (int i = 0; i < cur.to.size(); i++) {
                // System.out.println(cur.v);
                // diminish rand by the weight
                rand -= cur.w.get(i);

                // if rand is totally diminished, then we have what we are looking for
                if (rand <= 0) {
                    // advance cur
                    cur = lattice.get(cur.to.get(i));
                    break;
                }
            }

            // append the transition string
            sb.append(cur.v);
        }

        String resultRaw = sb.toString();
        // TODO this needs to be formatted using the provided args
        // TODO also be sure that the first character is capitalized
        String fname = name.split(" ")[0];
        resultRaw = resultRaw.replace("<f-name>",fname);
        SimpleDateFormat sdfDate = new SimpleDateFormat("MMM d");
        resultRaw = resultRaw.replace("<f-date>",sdfDate.format(date.getTime()));
        SimpleDateFormat sdfTime = new SimpleDateFormat("K:mm");
        resultRaw = resultRaw.replace("<f-time>",sdfTime.format(date.getTime()));
        resultRaw = resultRaw.replace("<f-date-relative>",relativize(date));
        return StringUtils.capitalize(resultRaw);
    }

    private String relativize(Calendar date) {
        String prefix;
        long daysBetween = ChronoUnit.DAYS.between(date.toInstant(), Calendar.getInstance().toInstant());
        if (daysBetween > 7 && daysBetween < 14) {
            prefix = "next";
        } else if (daysBetween <= 7){
            prefix = "this";
        } else {
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
            return sdfTime.format(date.getTime());
        }

        return String.format("%s %s", prefix, date.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()));
    }

    /**
     * Normailzes the weights in the w members of each LatticeNode.
     * the weights will sum to 1 afterward 
     * (up to floating point arithmetic error)
     */
    private void normalize() {
        for (Map.Entry<String,LatticeNode> entry : this.lattice.entrySet()) {

            // define a sum and a new list for the weights
            double sum = 0;
            List<Double> newWeights = new ArrayList<>(entry.getValue().w.size());

            for (double weight : entry.getValue().w) {
                sum += weight;
            }

            if (sum == 0 && !entry.getValue().w.isEmpty()) {
                // then the sum of weights is 0, but there need to be weights
                // let's default to a uniform distribution
                double weight = 1.0/entry.getValue().w.size();
                Server.logger.log(Level.FINE, String.format("Smoothing 0s in key %s", entry.getKey()));

                for (int i = 0; i < entry.getValue().w.size(); i++) {
                    newWeights.add(weight);
                }

            } else {                
                // then we need to normalize
                for (int i = 0; i < entry.getValue().w.size(); i++) {
                    newWeights.add(entry.getValue().w.get(i)/sum);
                }

            }

            // Build the new lattice node
            LatticeNode n = new LatticeNode();
            n.w = newWeights;
            n.to = entry.getValue().to;
            n.v = entry.getValue().v;

            // update the new node
            entry.setValue(n);
            // System.out.println(entry.getValue().w);
        }
    }

    /**
     * Throws an IOException if the word lattice is not valid
     * 
     * Explicitly checks that the lengths of the 'to' and 'w' members of
     * each Lattice node are the same length.
     */
    private void assertValid() throws IOException {
        for (Map.Entry<String, LatticeNode> entry : this.lattice.entrySet()) {

            if (entry.getValue().w== null) {
                throw new IOException(String.format("Null weight list on key %s", entry.getKey()));
            }

            if (entry.getValue().to == null) {
                throw new IOException(String.format("Null transition list on key %s", entry.getKey()));
            }

            if (entry.getValue().v == null) {
                throw new IOException(String.format("Null vlaue on key %s", entry.getKey()));
            }

            if (entry.getValue().to.size() != entry.getValue().w.size()) {
                throw new IOException(String.format("List length mismatch on key %s", entry.getKey()));
            }
        }
    }

    /**
     * Lattice node is a node in a WordLattice Digraph.
     * 
     * The field names are shortened so that they are more manageable 
     * to write in json, as most of these json files will have to be 
     * created manually
     */
    class LatticeNode {
        LatticeNode() {}
        String v;           // the string value in this node
        List<String> to;    // the nodes to which this node can transition
        List<Double> w;      // the weights on those transitions
    }

    public static void main(String args[]) {
        try {
            WordLattice wl = WordLattice.fromFile(args[1]);
            int ct;
            try {
                ct = Integer.parseInt(args[2]);
            } catch (Exception e) {
                ct = 1;
            }
            for (int i = 0; i < ct; i++) {
                System.out.println(wl.generate(Calendar.getInstance(), "John Doe"));
            }
        } catch (IOException ex) {
            System.out.println(String.format("`%s' doesn't seem to exist", args[1]));
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("usage: java com.studmane.nlpserver.service.model.Wordlattice FILE [COUNT]");
        }
    }
}