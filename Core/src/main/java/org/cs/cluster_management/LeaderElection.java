package org.cs.cluster_management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {

    private static final String ELECTION_NAMESPACE = "/election";
    private final ZooKeeper zooKeeper;
    private String zNodeName;
    private OnElectionCallback onElectionCallback;

    public LeaderElection(ZooKeeper zooKeeper,OnElectionCallback onElectionCallback) {
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
    }

    public void volunteerForLeaderShip() throws InterruptedException, KeeperException {
        String zNodePrefix = ELECTION_NAMESPACE + "/c_";
        String zNodeFullPath = zooKeeper.create(zNodePrefix,new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("zNode name:"+zNodeFullPath);
        this.zNodeName = zNodeFullPath.replace(ELECTION_NAMESPACE + "/","");
    }

    public void electLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorName = "";
        while(predecessorStat == null) {
            List<String> childrenList = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
            Collections.sort(childrenList);
            if (childrenList.get(0).equals(this.zNodeName)) {
                System.out.println("I am the leader");
                onElectionCallback.onElectedToBeLeader();
                return;
            } else {
                System.out.println("I am not the leader, Leader is " + childrenList.get(0));
                int predecessorIndex = Collections.binarySearch(childrenList, zNodeName) - 1;
                predecessorName = childrenList.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorName, this);
            }
        }
        onElectionCallback.onWorker();
        System.out.println("Watching Node :"+predecessorName);
    }

    public void watchTargetZnode() throws InterruptedException, KeeperException {
        Stat stat = zooKeeper.exists(ELECTION_NAMESPACE,this);
        if(stat == null) {
            return;
        }
        byte[] data = zooKeeper.getData(ELECTION_NAMESPACE,this,stat);
        List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE,this);
        String converted_data = data != null ? new String(data) : "";
        System.out.println("Data: "+converted_data+" Children: "+children);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case NodeCreated:
                System.out.println(ELECTION_NAMESPACE+" CREATED");
                break;
            case NodeDeleted:
                // We are watching the node next to us i.e (curr_node - 1) so whenever that
                // node is deleted, we will retrigger an election and will start watching
                // the node which that deleted node was watching.
                System.out.println(ELECTION_NAMESPACE+" DELETED");
                try {
                    electLeader();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (KeeperException e) {
                    throw new RuntimeException(e);
                }
                break;
            case NodeDataChanged:
                System.out.println(ELECTION_NAMESPACE+" DATA CHANGED");
                break;
            case NodeChildrenChanged:
                System.out.println(ELECTION_NAMESPACE+" NODE CHILDREN CHANGES");
                break;
        }

        try {
            System.out.println("[CHECKPOINT]");
            watchTargetZnode();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }

    }
}
