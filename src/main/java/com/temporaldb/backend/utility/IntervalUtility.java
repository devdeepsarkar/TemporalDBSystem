package com.temporaldb.backend.utility;

public class IntervalUtility {

    /**
     * Checks if two intervals overlap.
     * Two intervals [start1, end1] and [start2, end2] overlap if:
     * start1 <= end2 AND start2 <= end1
     * 
     * @param i1 The first interval
     * @param i2 The second interval
     * @return true if the intervals overlap, false otherwise
     */
    public static <T extends Comparable<T>> boolean overlap(Interval<T> i1, Interval<T> i2) {
        if (i1 == null || i2 == null) {
            return false;
        }

        T start1 = i1.getValid_from();
        T end1 = i1.getValid_to();
        T start2 = i2.getValid_from();
        T end2 = i2.getValid_to();

        // valid_from can never be null based on business rules
        if (start1 == null || start2 == null) {
            return false;
        }

        // valid_to = null indicates current time / indefinitely into the future
        // Two intervals overlap if: start1 < end2 AND start2 < end1
        // If an end date is null, the respective start date is always considered < to it.
        boolean start1BeforeEnd2 = (end2 == null) || (start1.compareTo(end2) < 0);
        boolean start2BeforeEnd1 = (end1 == null) || (start2.compareTo(end1) < 0);

        return start1BeforeEnd2 && start2BeforeEnd1;
    }
}

class Interval<T extends Comparable<T>> {
    private T valid_from;
    private T valid_to;

    public Interval() {
    }

    public Interval(T valid_from, T valid_to) {
        this.valid_from = valid_from;
        this.valid_to = valid_to;
    }

    public T getValid_from() {
        return valid_from;
    }

    public void setValid_from(T valid_from) {
        this.valid_from = valid_from;
    }

    public T getValid_to() {
        return valid_to;
    }

    public void setValid_to(T valid_to) {
        this.valid_to = valid_to;
    }

    @Override
    public String toString() {
        return "Interval{" +
                "valid_from=" + valid_from +
                ", valid_to=" + valid_to +
                '}';
    }
}
