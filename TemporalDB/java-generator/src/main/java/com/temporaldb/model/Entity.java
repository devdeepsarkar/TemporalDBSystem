package com.temporaldb.model;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Entity {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "temporal")
    private Boolean temporal;

    @XmlElement(name = "PrimaryKey")
    private java.util.List<PrimaryKey> primaryKeys; // To support multiple or single wrapper

    @XmlElementWrapper(name = "Attributes")
    @XmlElement(name = "Attribute")
    private List<Attribute> attributes;

    public String getName() { return name; }
    public boolean isTemporal() { return temporal != null && temporal; }

    public PrimaryKey getPrimaryKey() {
        return (primaryKeys != null && !primaryKeys.isEmpty()) ? primaryKeys.get(0) : null;
    }

    public List<Attribute> getAttributes() { return attributes; }
}
