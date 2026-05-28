package com.temporaldb.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class TimeDimension {

    @XmlAttribute(name = "validStartField")
    private String validStartField;

    @XmlAttribute(name = "validEndField")
    private String validEndField;

    // Single transaction timestamp – replaces the old tx_from / tx_to pair
    @XmlAttribute(name = "transactionField")
    private String transactionField;

    public String getValidStartField() { return validStartField != null ? validStartField : "valid_from"; }
    public String getValidEndField()   { return validEndField   != null ? validEndField   : "valid_to"; }
    public String getTransactionField(){ return transactionField != null ? transactionField : "tx_time"; }
}

