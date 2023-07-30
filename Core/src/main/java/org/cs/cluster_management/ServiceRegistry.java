package org.cs.cluster_management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher{
    public static final String REGISTRY_NODE = "/service_registry";
    private final ZooKeeper zooKeeper;
    private String currentZnode = null;

    public List<String> getAllServiceAddresses() {
        return allServiceAddresses;
    }

    private List<String> allServiceAddresses = null;

    public ServiceRegistry(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        createServiceRegistryZNode();
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        this.currentZnode = zooKeeper.create(REGISTRY_NODE + "/n_",metadata.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to the Service Registry");
    }

    public void registerForUpdates() {
        try {
            updateAddress();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }

    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        if(currentZnode != null && zooKeeper.exists(currentZnode,false) != null) {
            zooKeeper.delete(currentZnode, -1);
        }
    }

    private void createServiceRegistryZNode() {
        try {
            if(zooKeeper.exists(REGISTRY_NODE,false) == null) {
                zooKeeper.create(REGISTRY_NODE, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void updateAddress() throws InterruptedException, KeeperException {
        List<String> workerZnodes = zooKeeper.getChildren(REGISTRY_NODE,  this);
        List<String> address = new ArrayList<>(workerZnodes.size());

        for(String znode : workerZnodes) {
            String workerZnodeFullPath = REGISTRY_NODE + "/" + znode;
            Stat stat = zooKeeper.exists(workerZnodeFullPath,false);
            if(stat== null ) {
                continue;
            }
            byte data[] = zooKeeper.getData(workerZnodeFullPath,false,stat);
            String addressString = new String(data);
            address.add(addressString);
        }
        this.allServiceAddresses = Collections.unmodifiableList(address);
        System.out.println("The Cluster Address are: "+ this.allServiceAddresses);
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            updateAddress();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }
}
