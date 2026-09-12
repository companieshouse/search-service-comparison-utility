package uk.gov.companieshouse.search.comparison.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class HitDoc {

    @JsonProperty("doc_id")
    private String docId;

    private Map<String, Object> source;

    public HitDoc() {
    }

    public HitDoc(String docId, Map<String, Object> source) {
        this.docId = docId;
        this.source = source;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public Map<String, Object> getSource() {
        return source;
    }

    public void setSource(Map<String, Object> source) {
        this.source = source;
    }
}