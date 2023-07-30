package org.cs;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.cs.cluster_management.LeaderElection;
import org.cs.cluster_management.OnElectionCallback;
import org.cs.cluster_management.ServiceRegistry;

import java.io.IOException;

public class Application implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;
    public static void main(String args[]) throws IOException {

        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZooKeeper();
        ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);
        ServiceRegistry coordinatorsServiceRegistry = new ServiceRegistry(zooKeeper);
        OnElectionCallback onElectionCallback = new OnElectionAction(serviceRegistry,coordinatorsServiceRegistry, Integer.parseInt(args[0]));
        LeaderElection leaderElection = new LeaderElection(zooKeeper,onElectionCallback);
        try {
            leaderElection.volunteerForLeaderShip();
            leaderElection.electLeader();
            leaderElection.watchTargetZnode();

            application.run();
            application.close();
            System.out.println("Disconnected From ZooKeeper !!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
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
