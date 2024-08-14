package biouml.plugins.biopax.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

public class SequenceSite extends Concept
{
    public SequenceSite(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private String positionStatus;

    public String getPositionStatus()
    {
        return positionStatus;
    }

    public void setPositionStatus(String positionStatus)
    {
        this.positionStatus = positionStatus;
    }
    
    private String sequencePosition;

    public String getSequencePosition()
    {
        return sequencePosition;
    }

    public void setSequencePosition(String sequencePosition)
    {
        this.sequencePosition = sequencePosition;
    }
    
    
}
