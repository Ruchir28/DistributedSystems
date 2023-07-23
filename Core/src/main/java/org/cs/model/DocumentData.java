package org.cs.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model for Document, contains Term Frequency for queried words
 */
public class DocumentData {
    private Map<String,Double> termToFrequency = new HashMap<>();

    public void putTermFrequency(String term,double frequency) {
        termToFrequency.put(term,frequency);
    }

    public double getTermFrequency(String term) {
        return termToFrequency.get(term);
    }
}
