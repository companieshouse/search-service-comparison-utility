
package uk.gov.companieshouse.search.comparison.handler;

import java.util.List;

public class Event {
    // AWS wraps event's data in a "detail" field
    // which contains the actual event data
    // Until we don't need a complex data structure
    // we can just use a simple class to represent the event
    // and avoid a custom deserializer
    private List<Detail> details;

    public List<Detail> getDetails() {
        return details;
    }

    public void setDetails(List<Detail> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "Event{" +
                "details=" + details +
                '}';
    }

    public static class Detail {
        private String action;
        private String operation;
        private String query;
        private int size;
        private IndexName indexName;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public IndexName getIndexName() {
            return indexName;
        }

        public void setIndexName(IndexName indexName) {
            this.indexName = indexName;
        }


        @Override
        public String toString() {
            return "Detail{" +
                    "action='" + action + '\'' +
                    ", operation='" + operation + '\'' +
                    ", query='" + query + '\'' +
                    ", size=" + size +
                    ", indexName=" + indexName +
                    '}';
        }
    }

    public static class IndexName {
        private String blueIndexName;

        private String greenIndexName;

        public String getBlueIndexName() {
            return blueIndexName;
        }

        public void setBlueIndexName(String blueIndexName) {
            this.blueIndexName = blueIndexName;
        }

        public String getGreenIndexName() {
            return greenIndexName;
        }

        public void setGreenIndexName(String greenIndexName) {
            this.greenIndexName = greenIndexName;
        }

        @Override
        public String toString() {
            return "IndexName{" +
                    "blueIndexName='" + blueIndexName + '\'' +
                    ", greenIndexName='" + greenIndexName + '\'' +
                    '}';
        }
    }
}

