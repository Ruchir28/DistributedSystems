package org.cs;

import org.cs.model.DocumentData;
import org.cs.search.TFIDF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SequentialSearch {
    public static final String BOOKS_DIRECTORY = "./Core/src/main/resources/books";
    public static final String SEARCH_QUERY1 = "The best detective that";
    public static final String SEARCH_QUERY2 = "The girl that falls ";

    public static final String SEARCH_QUERY3 = "A war between Russia";

    public static void main(String[] args) throws FileNotFoundException {
        File documentDirectory  = new File(BOOKS_DIRECTORY);
        System.out.println(documentDirectory.getAbsoluteFile());
        List<String> documents = Arrays.asList(documentDirectory.list())
                .stream()
                .map(documentName -> BOOKS_DIRECTORY +"/" + documentName)
                .collect(Collectors.toList());

        List<String> terms = TFIDF.getWordsFromLine(SEARCH_QUERY1);
        findMostRelevantDocuments(documents,terms);

    }

    private static void findMostRelevantDocuments(List<String> documents,List<String> terms) throws FileNotFoundException {
        Map<String,DocumentData> documentDataMap = new HashMap<String, DocumentData>();
        for (String document : documents) {
            BufferedReader reader = new BufferedReader(new FileReader(document));
            List<String> lines = reader.lines().collect(Collectors.toList());
            List<String> words = TFIDF.getWordsFromLines(lines);
            DocumentData documentData = TFIDF.createDocumentData(words,terms);
            documentDataMap.put(document, documentData);
        }
        Map<Double,List<String>> documentsByScore = TFIDF.getDocumentSortedByScore(terms,documentDataMap);
        for(Map.Entry<Double,List<String>> entry : documentsByScore.entrySet()) {
            System.out.printf("Score: %f, Documents: %s \n",entry.getKey(),entry.getValue().toString());
        }
    }
}
