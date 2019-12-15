package com.studmane.nlpserver.service.model;

import org.junit.*;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Calendar;

public class WordLatticeTest {
    public WordLatticeTest () {}

    @Test
    public void TestWordLatticeLoader() throws IOException {
        System.out.println(System.getenv("PWD"));
        WordLattice wl = WordLattice.fromFile("propose.lat");


    }

    @Test
    public void TestWordLatticeGenerator() throws IOException {
        Calendar c = Calendar.getInstance();

        String[] files = {"apptset.lat", "propose.lat", /*"signoff.lat",*/ "correction.lat", "where.lat", "reschedule.lat", /*"when.lat",*/ "yourewelcome.lat"};
        // String[] files = {"propose.lat"};

        for (String filename : files) {
            // what are we generating from?
            System.out.println(String.format("   -Generating from %s...", filename));

            // construct a WordLattice
            WordLattice wl = WordLattice.fromFile(filename);

            // generate some crap and we will check if it looks good
            for (int i = 0; i < 10; i++) {
                String gen = wl.generate(c, "John Doe");
                assert !gen.equals("");
                System.out.println(gen);
            }
        }
    }
}