package org.cs;

import org.apache.zookeeper.KeeperException;
import org.cs.cluster_management.OnElectionCallback;
import org.cs.cluster_management.ServiceRegistry;
import org.cs.networking.WebServer;
import org.cs.search.SearchWorker;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry serviceRegistry;
    private final int port;
    private WebServer webServer;

    public OnElectionAction(ServiceRegistry serviceRegistry, int port) {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }
    @Override
    public void onElectedToBeLeader() {
        try {
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.registerForUpdates();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onWorker() {
        try {
            SearchWorker searchWorker = new SearchWorker();
            // if the node is a worker node, then it will start a server and listen on that for incoming tasks
            webServer = new WebServer(port,searchWorker);
            String currentAddress = String.format("http://%s:%d%s", InetAddress.getLocalHost().getCanonicalHostName(), port,searchWorker.getEndpoint());
            serviceRegistry.registerToCluster(currentAddress);
            serviceRegistry.registerForUpdates();
        } catch (UnknownHostException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
