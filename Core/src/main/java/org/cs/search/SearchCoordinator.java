package org.cs.search;

import com.google.protobuf.InvalidProtocolBufferException;
import org.cs.cluster_management.ServiceRegistry;
import org.cs.model.DocumentData;
import org.cs.model.Result;
import org.cs.model.SerializationUtils;
import org.cs.model.Task;
import org.cs.model.proto.SearchModel;
import org.cs.networking.OnRequestCallback;
import org.cs.networking.WebClient;
import org.cs.networking.WebServer;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of {@link OnRequestCallback} for Driver Nodes (i.e. Leader) that will be passed to {@link WebServer} and
 * will handle query requests, which comes from users, and will distribute the task in worker nodes.
 */
public class SearchCoordinator implements OnRequestCallback {

    private static final String ENDPOINT = "/search";
    private static final String BOOKS_DIRECTORY = "./books";
    private final ServiceRegistry workersServiceRegistry;
    private final WebClient client;
    private final List<String> documents;

    public SearchCoordinator(ServiceRegistry workersServiceRegistry, WebClient client) {
        this.workersServiceRegistry = workersServiceRegistry;
        this.client = client;
        this.documents = readDocumentsList();
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        try {
            SearchModel.Request request = SearchModel.Request.parseFrom(requestPayload);
            SearchModel.Response response = createResponse(request);
            return response.toByteArray();
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

    private SearchModel.Response createResponse(SearchModel.Request request) {
        SearchModel.Response.Builder searchResponse = SearchModel.Response.newBuilder();
        System.out.println("Received search query: " + request.getSearchQuery());
        List<String> searchTerms = TFIDF.getWordsFromLine(request.getSearchQuery());
        List<String> workers = workersServiceRegistry.getAllServiceAddresses();
        if(workers.isEmpty()) {
            System.out.println("No workers currently available");
            return searchResponse.build();
        }
        List<Task> tasks = createTasks(workers.size(),searchTerms);
        List<Result> results = sendTasksToWorkers(workers,tasks);

        List<SearchModel.Response.DocumentStats> sortedDocuments = aggregateResults(results,searchTerms);
        searchResponse.addAllRelevantDocuments(sortedDocuments);
        return searchResponse.build();
    }

    private List<SearchModel.Response.DocumentStats> aggregateResults(List<Result> results,List<String> searchTerms) {
        List<SearchModel.Response.DocumentStats> documentStats = new ArrayList<>();
        Map<String, DocumentData> documentDataMap = results.stream()
                .flatMap(result -> result.getDocumentToDocumentData().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
        Map<Double, List<String>> documentsSortedByScore = TFIDF.getDocumentSortedByScore(searchTerms,documentDataMap);
        for(Map.Entry<Double, List<String>> entry: documentsSortedByScore.entrySet()) {
            double score = entry.getKey();
            for(String document: entry.getValue()) {
                SearchModel.Response.DocumentStats documentStat = SearchModel.Response.DocumentStats.newBuilder()
                                .setDocumentName(document)
                                .setScore(score)
                                .build();
                documentStats.add(documentStat);
            }
        }
        return documentStats;
    }

    private List<Result> sendTasksToWorkers(List<String> workers,List<Task> tasks) {
        List<CompletableFuture<Result>> futures = new ArrayList<>();
        int taskIndex = 0;
        for(String worker: workers) {
            Task task = tasks.get(taskIndex++);
            byte[] serializedTask = SerializationUtils.serialize(task);
            CompletableFuture<byte[]> completableFuture = client.sendTask(worker,serializedTask);
            CompletableFuture<Result> resultCompletableFuture = completableFuture.thenApply(bytes -> (Result) SerializationUtils.deserialize(bytes));
            futures.add(resultCompletableFuture);
        }
        List<Result> results = new ArrayList<>();
        for(CompletableFuture<Result> completableFuture: futures) {
            try {
                Result result = completableFuture.get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    private List<Task> createTasks(int numberOfWorkers,List<String> searchTerms) {
        List<Task> tasks = new ArrayList<>();
        List<List<String>> splitDocuments = splitDocumentsList(numberOfWorkers,documents);
        for(int i = 0; i < splitDocuments.size(); i++) {
            Task task = new Task(searchTerms,splitDocuments.get(i));
            tasks.add(task);
        }
        return tasks;
    }

    private static List<String> readDocumentsList() {
        File documentsDirectory = new File(BOOKS_DIRECTORY);
        System.out.println(documentsDirectory.getAbsolutePath());
        return Arrays.asList(documentsDirectory.list())
                .stream()
                .map(documentName -> BOOKS_DIRECTORY + "/" + documentName)
                .collect(Collectors.toList());
    }


    private static List<List<String>> splitDocumentsList(int numberOfWorkers,List<String> documents) {
        List<List<String>> result = new ArrayList<>();
        int documentsPerWorker = documents.size() / numberOfWorkers;
        List<String> workerDocs = new ArrayList<>();
        for(String document: documents) {
            workerDocs.add(document);
            if(workerDocs.size() >= documentsPerWorker) {
                result.add(workerDocs);
                workerDocs = new ArrayList<>();
            }
        }
        if(!workerDocs.isEmpty()) {
            result.add(workerDocs);
        }
        return result;
    }

}
