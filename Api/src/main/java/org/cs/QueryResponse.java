package org.cs;

import java.util.Collections;
import java.util.List;

public class QueryResponse {
    private List<SearchResultInfo> searchResults = Collections.emptyList();

    public QueryResponse(List<SearchResultInfo> searchResults) {
        this.searchResults = searchResults;
    }

    public List<SearchResultInfo> getSearchResults() {
        return searchResults;
    }

    public static class SearchResultInfo {
        private String title;
        private double score;

        public SearchResultInfo(String title, double score) {
            this.title = title;
            this.score = score;
        }

        public String getTitle() {
            return title;
        }

        public double getScore() {
            return score;
        }
    }
}
