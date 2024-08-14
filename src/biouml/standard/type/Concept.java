package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Concept")
@PropertyDescription("Some concept, generally it corresponds to some biological state, function or process.")
@ClassIcon( "resources/concept.gif" )
public class Concept extends Referrer
{
    private String type;
    private String completeName;
    private String synonyms;
    
    public Concept(DataCollection origin, String name)
    {
        super(origin,name);
    }

    @Override
    public String getType()
    {
        if( type == null )
            type = TYPE_CONCEPT;

        return type;
    }
    public void setType(String type)
    {
        String oldValue = getType();
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    @PropertyName("Complete name")
    @PropertyDescription( "The object full name.")
    public String getCompleteName()
    {
        return completeName;
    }
    public void setCompleteName(String completeName)
    {
        String oldValue = this.completeName;
        this.completeName = completeName;
        firePropertyChange("completeName", oldValue, completeName);
    }
    
    @PropertyName("Synonyms")
    @PropertyDescription("The object name synonyms.")
    public String getSynonyms()
    {
        return synonyms;
    }
    public void setSynonyms(String synonyms)
    {
        String oldValue = this.completeName;
        this.synonyms = synonyms;
        firePropertyChange("synonyms", oldValue, synonyms);
    }
}
