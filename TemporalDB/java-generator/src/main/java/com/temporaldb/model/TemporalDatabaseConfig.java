package com.temporaldb.model;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "TemporalDatabaseConfig")
@XmlAccessorType(XmlAccessType.FIELD)
public class TemporalDatabaseConfig {

    // GlobalSettings omitted as it's not used for base DDL

    @XmlElement(name = "TimeDimension")
    private TimeDimension timeDimension;

    @XmlElementWrapper(name = "Entities")
    @XmlElement(name = "Entity")
    private List<Entity> entities;

    // Relationships are optional and omitted for now

    public List<Entity> getEntities() { return entities; }
    public TimeDimension getTimeDimension() { return timeDimension; }
    // getters/setters omitted for brevity
}
