package ru.biosoft.bsa;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.biohub.ReferenceType;

public class TranscriptionFactor extends DataElementSupport
{
    private ReferenceType type;
    private String displayName;
    private String speciesName;
    private DynamicPropertySet attributes;
    
    public TranscriptionFactor(String name, DataCollection origin, String displayName, ReferenceType type, String speciesName)
    {
        super(name, origin);
        this.type = type;
        this.displayName = displayName;
        this.speciesName = speciesName;
    }

    public String getSpeciesName()
    {
        return speciesName;
    }

    public ReferenceType getType()
    {
        return type;
    }

    public String getDisplayName()
    {
        return displayName;
    }
    
    public DynamicPropertySet getAttributes()
    {
        if(attributes == null) attributes = new DynamicPropertySetAsMap();
        return attributes;
    }
}
