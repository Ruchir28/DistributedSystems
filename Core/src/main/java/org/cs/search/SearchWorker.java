package org.cs.search;

import org.cs.model.DocumentData;
import org.cs.model.Result;
import org.cs.model.SerializationUtils;
import org.cs.model.Task;
import org.cs.networking.OnRequestCallback;
import org.cs.networking.WebServer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link OnRequestCallback} For Worker Nodes, that will be passed to {@link WebServer},
 * It receives Task from Leader Node and will calculate TF-IDF for each document and will return the result to Leader Node.
 */
public class SearchWorker implements OnRequestCallback {

    private static final String ENDPOINT = "/task";

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        Task task = (Task) SerializationUtils.deserialize(requestPayload);
        Result result = createResult(task);
        return SerializationUtils.serialize(result);
    }

    private Result createResult(Task task) {
        List<String> documents = task.getDocuments();
        Result result = new Result();
        for(String document: documents) {
            List<String> words = parseWordsFromDocument(document);
            DocumentData documentData = TFIDF.createDocumentData(words,task.getSearchTerms());
            result.addDocumentData(document,documentData);
        }
        return result;
    }

    private List<String> parseWordsFromDocument(String document)  {
        try {
            FileReader file = new FileReader(document);
            BufferedReader bufferedReader = new BufferedReader(file);
            List<String> lines = bufferedReader.lines().collect(Collectors.toList());
            List<String> words = TFIDF.getWordsFromLines(lines);
            return words;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();

    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
