package uk.gov.companieshouse.search.comparison.handler;

public class Event {
    // AWS wraps event's data in a "detail" field
    // which contains the actual event data
    // Until we don't need a complex data structure
    // we can just use a simple class to represent the event
    // and avoid a custom deserializer
    private Detail detail;

    public Detail getDetail() {
        return detail;
    }

    public void setDetail(Detail detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "Event{" +
                "detail=" + detail +
                '}';
    }

    public static class Detail {
        private String action;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return "Detail{" +
                    "action='" + action + '\'' +
                    '}';
        }
    }
}
