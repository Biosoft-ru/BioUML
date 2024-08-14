package biouml.plugins.gtrd;

import ru.biosoft.access.core.DataCollection;

public abstract class ChIPTFExperiment extends ChIPExperiment
{
    protected String tfUniprotId;
    protected String tfClassId;
    protected String tfTitle;
    
    public ChIPTFExperiment(DataCollection<?> parent, String id )
    {
        super( parent, id );
    }
    
    public String getTfUniprotId()
    {
        return tfUniprotId;
    }
    public void setTfUniprotId(String tfUniprotId)
    {
        this.tfUniprotId = tfUniprotId;
    }

    public String getTfClassId()
    {
        return tfClassId;
    }
    public void setTfClassId(String tfClassId)
    {
        this.tfClassId = tfClassId;
    }
    
    public String getTfTitle()
    {
        return tfTitle;
    }
    public void setTfTitle(String tfTitle)
    {
        this.tfTitle = tfTitle;
    }
}
