package com.temporaldb.model;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class PrimaryKey {

    @XmlElement(name = "KeyAttribute")
    private List<KeyAttribute> keyAttributes;

    public List<KeyAttribute> getKeyAttributes() { return keyAttributes; }
}
