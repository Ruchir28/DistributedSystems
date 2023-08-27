package org.cs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.zookeeper.KeeperException;
import org.cs.cluster_management.ServiceRegistry;
import org.cs.model.proto.SearchModel;
import org.cs.networking.OnRequestCallback;
import org.cs.networking.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ApiHandlerCallback implements OnRequestCallback {

    public final static String ENDPOINT = "/api";
    private final ServiceRegistry coorDinatorServiceRegistry;
    private final WebClient webClient;

    ObjectMapper objectMapper;

    public ApiHandlerCallback(ServiceRegistry coorDinatorServiceRegistry, WebClient webClient) {
        this.coorDinatorServiceRegistry = coorDinatorServiceRegistry;
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        try {
            QueryRequest frontendSearchRequest =
                    objectMapper.readValue(requestPayload, QueryRequest.class);

            QueryResponse frontendSearchResponse = createFrontendResponse(frontendSearchRequest);

            return objectMapper.writeValueAsBytes(frontendSearchResponse);
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private QueryResponse createFrontendResponse(QueryRequest frontendSearchRequest) {
        SearchModel.Response searchClusterResponse = sendRequestToSearchCluster(frontendSearchRequest.query);

        List<QueryResponse.SearchResultInfo> filteredResults =
                createResults(searchClusterResponse);

        return new QueryResponse(filteredResults);
    }

    private List<QueryResponse.SearchResultInfo> createResults(SearchModel.Response searchClusterResponse) {
        return searchClusterResponse.getRelevantDocumentsList().stream().map(documentStats -> {
            String docName = documentStats.getDocumentName();
            double docScore = documentStats.getScore();
            return new QueryResponse.SearchResultInfo(docName, docScore);
        }).collect(Collectors.toList());
    }

    private SearchModel.Response sendRequestToSearchCluster(String query) {
        SearchModel.Request searchRequest = SearchModel.Request.newBuilder()
                .setSearchQuery(query)
                .build();
        try {
            // TODO: GET RANDOM COORDINATOR
            String coordinatorAddress = coorDinatorServiceRegistry.getAllServiceAddresses().get(0);
            if (coordinatorAddress == null) {
                System.out.println("Search Cluster Coordinator is unavailable");
                return SearchModel.Response.getDefaultInstance();
            }

            byte[] payloadBody = webClient.sendTask(coordinatorAddress, searchRequest.toByteArray()).join();

            return SearchModel.Response.parseFrom(payloadBody);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return SearchModel.Response.getDefaultInstance();
        }
    }



    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
