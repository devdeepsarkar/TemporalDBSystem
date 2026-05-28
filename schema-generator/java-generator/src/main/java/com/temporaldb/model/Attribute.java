package com.temporaldb.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Attribute {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "type")
    private String type;

    @XmlAttribute(name = "temporal")
    private Boolean temporal;

    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isTemporal() { return temporal != null && temporal; }
}
