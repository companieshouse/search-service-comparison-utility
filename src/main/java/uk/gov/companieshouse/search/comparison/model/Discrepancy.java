package uk.gov.companieshouse.search.comparison.model;

public class Discrepancy {

    private int position;
    private HitDoc blue;
    private HitDoc green;

    public Discrepancy() {
    }

    public Discrepancy(int position,HitDoc blue, HitDoc green) {
        this.position = position;
        this.blue = blue;
        this.green = green;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
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