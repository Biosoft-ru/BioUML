package biouml.plugins.biopax.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

public class OpenControlledVocabulary extends Concept
{
    public OpenControlledVocabulary(DataCollection origin, String name)
    {
        super(origin, name);
        vocabularyType = "openControlledVocabulary";
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }

    private String term;

    public String getTerm()
    {
        return term;
    }

    public void setTerm(String term)
    {
        this.term = term;
    }
    
    private String vocabularyType;

    public String getVocabularyType()
    {
        return vocabularyType;
    }

    public void setVocabularyType(String vocabularyType)
    {
        this.vocabularyType = vocabularyType;
        
    }
    
}
