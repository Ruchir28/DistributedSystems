package org.cs;

import org.apache.zookeeper.KeeperException;
import org.cs.cluster_management.OnElectionCallback;
import org.cs.cluster_management.ServiceRegistry;
import org.cs.networking.WebClient;
import org.cs.networking.WebServer;
import org.cs.search.SearchCoordinator;
import org.cs.search.SearchWorker;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry serviceRegistry;
    private final ServiceRegistry coordinatorsServiceRegistry;
    private final int port;
    private WebServer webServer;

    public OnElectionAction(ServiceRegistry serviceRegistry, ServiceRegistry coordinatorsServiceRegistry, int port) {
        this.serviceRegistry = serviceRegistry;
        this.coordinatorsServiceRegistry = coordinatorsServiceRegistry;
        this.port = port;
    }
    @Override
    public void onElectedToBeLeader() {
        try {
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.registerForUpdates();;
            if (webServer != null) {
                webServer.stop();
            }
            // sending searchCoordinator action handler to webserver
            SearchCoordinator searchCoordinator = new SearchCoordinator(serviceRegistry, new WebClient());
            webServer = new WebServer(port, searchCoordinator);
            webServer.startServer();
            String currentServerAddress =
                    String.format("http://%s:%d%s", InetAddress.getLocalHost().getCanonicalHostName(), port, searchCoordinator.getEndpoint());
            coordinatorsServiceRegistry.registerToCluster(currentServerAddress);
        } catch (InterruptedException | KeeperException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onWorker() {
        try {
            SearchWorker searchWorker = new SearchWorker();
            // if the node is a worker node, then it will start a server and listen on that for incoming tasks
            // sending searchWorker action handler to webserver
            webServer = new WebServer(port,searchWorker);
            String currentAddress = String.format("http://%s:%d%s", InetAddress.getLocalHost().getCanonicalHostName(), port,searchWorker.getEndpoint());
            webServer.startServer();
            serviceRegistry.registerToCluster(currentAddress);
            serviceRegistry.registerForUpdates();
        } catch (UnknownHostException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
