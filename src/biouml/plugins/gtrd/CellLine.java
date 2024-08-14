package biouml.plugins.gtrd;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

public class CellLine extends DataElementSupport
{
    private String title;
    private Species species;
    
    //External refs
    private String cellosaurusId;
    private String cellOntologyId;
    private String expFactorOntologyId;
    private String uberonId;
    private String source;
    private String sourceId;
    private String[] cellTypeId;
    private String brendaId;
    
    public CellLine(String id, String title, Species species, DataCollection origin)
    {
        super( id, origin );
        this.title = title;
        this.species = species;
    }
    
    public String getTitle()
    {
        return title;
    }
    
    public Species getSpecies()
    {
        return species;
    }

    public String getCellosaurusId()
    {
        return cellosaurusId;
    }

    public void setCellosaurusId(String cellosaurusId)
    {
        this.cellosaurusId = cellosaurusId;
    }

    public String getCellOntologyId()
    {
        return cellOntologyId;
    }

    public void setCellOntologyId(String cellOntologyId)
    {
        this.cellOntologyId = cellOntologyId;
    }

    public String getExpFactorOntologyId()
    {
        return expFactorOntologyId;
    }

    public void setExpFactorOntologyId(String expFactorOntologyId)
    {
        this.expFactorOntologyId = expFactorOntologyId;
    }

    public String getUberonId()
    {
        return uberonId;
    }

    public void setUberonId(String uberonId)
    {
        this.uberonId = uberonId;
    }
    
    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }
    
    public String getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(String sourceId)
    {
        this.sourceId = sourceId;
    }
    
    public String[] getCellTypeId()
    {
        return cellTypeId;
    }

    public void setCellTypeId(String[] cellTypeId)
    {
        this.cellTypeId = cellTypeId;
    }
    
    public String getBrendaId()
    {
        return brendaId;
    }

    public void setBrendaId(String brendaId)
    {
        this.brendaId = brendaId;
    }
    
    @Override
    public String toString()
    {
        return title;
    }
}
