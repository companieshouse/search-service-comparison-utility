package uk.gov.companieshouse.search.comparison.model;

public class Discrepancy {

    private HitDoc blue;
    private HitDoc green;

    public Discrepancy() {
    }

    public Discrepancy(HitDoc blue, HitDoc green) {
        this.blue = blue;
        this.green = green;
    }

    public HitDoc getBlue() {
        return blue;
    }

    public void setBlue(HitDoc blue) {
        this.blue = blue;
    }

    public HitDoc getGreen() {
        return green;
    }

    public void setGreen(HitDoc green) {
        this.green = green;
    }
}