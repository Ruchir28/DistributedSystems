package org.cs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.zookeeper.KeeperException;
import org.cs.cluster_management.ServiceRegistry;
import org.cs.model.proto.SearchModel;
import org.cs.networking.OnRequestCallback;
import org.cs.networking.WebClient;

import java.util.List;

public class ApiHandlerCallback implements OnRequestCallback {

    public final static String ENDPOINT = "/api";
    private final ServiceRegistry coorDinatorServiceRegistry;
    private final WebClient webClient;

    public ApiHandlerCallback(ServiceRegistry coorDinatorServiceRegistry, WebClient webClient) {
        this.coorDinatorServiceRegistry = coorDinatorServiceRegistry;
        this.webClient = webClient;
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        // TODO: Implement
        return new byte[0];
    }



    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
