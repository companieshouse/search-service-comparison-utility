package uk.gov.companieshouse.search.comparison.handler;

public class Detail {
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
