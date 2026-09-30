package uk.gov.companieshouse.search.comparison.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MatchQueryResult {

    @JsonProperty("blue_total")
    private int blueTotal;

    @JsonProperty("green_total")
    private int greenTotal;

    @JsonProperty("total_discrepancies")
    private int totalDiscrepancies;

    private List<Discrepancy> discrepancies = new ArrayList<>();

    public int getBlueTotal() {
        return blueTotal;
    }

    public void setBlueTotal(int blueTotal) {
        this.blueTotal = blueTotal;
    }

    public int getGreenTotal() {
        return greenTotal;
    }

    public void setGreenTotal(int greenTotal) {
        this.greenTotal = greenTotal;
    }

    public int getTotalDiscrepancies() {
        return totalDiscrepancies;
    }

    public void setTotalDiscrepancies(int totalDiscrepancies) {
        this.totalDiscrepancies = totalDiscrepancies;
    }

    public List<Discrepancy> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<Discrepancy> discrepancies) {
        this.discrepancies = discrepancies;
    }
}