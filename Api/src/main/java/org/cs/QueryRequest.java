package org.cs;

public class QueryRequest {
    public String query;
    public int limit;

    @Override
    public String toString() {
        return "QueryRequest{" +
                "query='" + query + '\'' +
                ", limit=" + limit +
                '}';
    }
}
