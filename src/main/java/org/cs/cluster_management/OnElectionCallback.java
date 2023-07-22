package org.cs.cluster_management;

public interface OnElectionCallback {
    void onElectedToBeLeader();
    void onWorker();
}
