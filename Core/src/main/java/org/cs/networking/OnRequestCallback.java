package org.cs.networking;

public interface OnRequestCallback {
    public byte[] handleRequest(byte[] requestPayload);
    public String getEndpoint();
}
