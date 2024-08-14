package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

/**
 * SemanticRelation specified semantic relationships between concepts (kernels)
 * associated with two nodes on the diagram.
 */
@ClassIcon( "resources/semanticrelation.gif" )
public class SemanticRelation extends Referrer implements Relation
{
    public SemanticRelation(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    public SemanticRelation(DataCollection<?> origin, String name, String relationType)
    {
        this(origin, name);
        this.relationType = relationType;
    }

    @Override
    public String getType()
    {
        return TYPE_SEMANTIC_RELATION;
    }

    @Override
    public String getTitle()
    {
        return title == null ? relationType : title;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /**
     * Name of input data element (kernel) participation in this relation. The
     * name can be given relative Module.DATA data collection or it can be
     * complete name of corresponding kernel data element.
     */
    private String inputElementName;

    public String getInputElementName()
    {
        return inputElementName;
    }

    public void setInputElementName(String inputElementName)
    {
        String oldValue = this.inputElementName;
        this.inputElementName = inputElementName;
        firePropertyChange("inputElementName", oldValue, inputElementName);
    }

    /**
     * Name of output data element (kernel) participation in this relation. The
     * name can be given relative Module.DATA data collection or it can be
     * complete name of corresponding kernel data element.
     */
    private String outputElementName;

    public String getOutputElementName()
    {
        return outputElementName;
    }

    public void setOutputElementName(String outputElementName)
    {
        String oldValue = this.outputElementName;
        this.outputElementName = outputElementName;
        firePropertyChange("outputElementName", oldValue, outputElementName);
    }

    private String relationType;

    public String getRelationType()
    {
        return relationType;
    }

    public void setRelationType(String relationType)
    {
        String oldValue = this.relationType;
        this.relationType = relationType;
        firePropertyChange("relationType", oldValue, relationType);
        firePropertyChange("title", oldValue, relationType);
    }

    private String participation;

    @Override
    public String getParticipation()
    {
        return participation;
    }

    public void setParticipation(String participation)
    {
        String oldValue = this.participation;
        this.participation = participation;
        firePropertyChange("participation", oldValue, participation);
    }
}
