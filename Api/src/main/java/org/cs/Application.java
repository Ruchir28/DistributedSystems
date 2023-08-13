package org.cs;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.cs.cluster_management.ServiceRegistry;
import org.cs.networking.WebClient;
import org.cs.networking.WebServer;

import java.io.IOException;

public class Application implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;
    public static void main(String args[]) throws IOException {

        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZooKeeper();
        ServiceRegistry coordinatorsServiceRegistry = new ServiceRegistry(zooKeeper,"/coordinators_service_registry");
        WebClient webClient = new WebClient();
        ApiHandlerCallback apiHandlerCallback = new ApiHandlerCallback(coordinatorsServiceRegistry,webClient);
        WebServer webServer = new WebServer(8080,apiHandlerCallback);
        webServer.startServer();

        coordinatorsServiceRegistry.registerForUpdates();
        try {
            application.run();
            application.close();
            webServer.stop();
            System.out.println("Disconnected From ZooKeeper !!, Webserver Stopped !! Exiting !!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    public ZooKeeper connectToZooKeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS,SESSION_TIMEOUT,this);
        return zooKeeper;
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if(event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to ZooKeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from ZooKeeper");
                        zooKeeper.notifyAll();
                    }
                }
        }
    }
}
