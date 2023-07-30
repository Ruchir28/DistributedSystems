package org.cs.search;

import org.cs.model.DocumentData;

import java.util.*;

/**
 *  Utility class for calculating Term Frequency and Inverse Document Frequency.
 */
public class TFIDF {
    public static double calculateTermFrequency(List<String> words, String term) {
        long count = 0;
        for (String word : words) {
            if (word.equals(term)) {
                count++;
            }
        }
        return ((double) count )/ words.size();
    }

    public static DocumentData createDocumentData(List<String> words, List<String> terms) {
        DocumentData documentData = new DocumentData();
        for(String term:terms) {
            double termFrequency = calculateTermFrequency(words,term);
            documentData.putTermFrequency(term, termFrequency);
        }
        return documentData;
    }

    private static double getInverseDocumentFrequency(Map<String,DocumentData> documentDataList, String term) {
        int totalDocuments = documentDataList.size();
        int termInversedocumentFrequency = 0;
        for(DocumentData documentData : documentDataList.values()) {
            if(documentData.getTermFrequency(term) > 0.0) {
                termInversedocumentFrequency++;
            }
        }
        return termInversedocumentFrequency == 0 ? 0 : Math.log10(((double)totalDocuments)/termInversedocumentFrequency);
    }

    private static Map<String,Double> getTermToInverseDocumentFrequencyMap(List<String> terms, Map<String,DocumentData> documentData) {
        Map<String,Double> termToInverseDocumentFrequency = new HashMap<String,Double>();
        for(String term : terms) {
            termToInverseDocumentFrequency.put(term, getInverseDocumentFrequency(documentData,term));
        }
        return termToInverseDocumentFrequency;
    }

    private static double getDocumentScore(List<String> terms,DocumentData documentData,
                                          Map<String,Double> termToInverseDocumentFrequency) {
        double score = 0.0;
        for(String term : terms) {
            score += documentData.getTermFrequency(term) * termToInverseDocumentFrequency.get(term);
        }
        return score;
    }

    public static Map<Double,List<String>> getDocumentSortedByScore(List<String> terms,
                                                                    Map<String,DocumentData> documentDataList) {
        TreeMap<Double,List<String>> scoreToDocument = new TreeMap<Double,List<String>>();
        Map<String,Double> termToInverseDocumentFrequency = getTermToInverseDocumentFrequencyMap(terms,documentDataList);
        for(Map.Entry<String, DocumentData> entry : documentDataList.entrySet()) {
            String document = entry.getKey();
            DocumentData documentData = entry.getValue();
            double documentScore = getDocumentScore(terms,documentData,termToInverseDocumentFrequency);
            List<String> documents = scoreToDocument.getOrDefault(documentScore,new ArrayList<>());
            documents.add(document);
            scoreToDocument.put(documentScore,documents);
        }
        return scoreToDocument.descendingMap();
    }

    public static List<String> getWordsFromLine(String line) {
        return Arrays.asList(line.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(\\d)+|(\\n)+"));
    }

    public static List<String> getWordsFromLines(List<String> lines) {
        List<String> words = new ArrayList<>();
        for(String line : lines) {
            words.addAll(getWordsFromLine(line));
        }
        return words;
    }


}
