package biouml.plugins.riboseq.db.model;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class MRNAExperiment extends DataElementSupport
{
    public MRNAExperiment(DataCollection<MRNAExperiment> origin, String name)
    {
        super( name, origin );
    }

    private String title = "";
    @PropertyName("Title")
    @PropertyDescription("Human readable experiment title")
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    private String description = "";
    @PropertyName("Description")
    @PropertyDescription("Experiment description")
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    private Species species;
    @PropertyName("Species")
    @PropertyDescription("Species")
    public Species getSpecies()
    {
        return species;
    }
    public void setSpecies(Species species)
    {
        this.species = species;
    }

    private CellSource cellSource;
    @PropertyName("Cell source")
    @PropertyDescription("The source of cells")
    public CellSource getCellSource()
    {
        return cellSource;
    }
    public void setCellSource(CellSource cellSource)
    {
        this.cellSource = cellSource;
    }

    private Condition[] conditions;
    @PropertyName("Conditions")
    @PropertyDescription("The list of conditions/treatments not related to Ribo-seq protocol")
    public Condition[] getConditions()
    {
        return conditions;
    }
    public void setConditions(Condition[] conditions)
    {
        this.conditions = conditions;
    }

    private SequencingPlatform sequencingPlatform;
    @PropertyName("Sequencing platform")
    @PropertyDescription("Sequencing platform")
    public SequencingPlatform getSequencingPlatform()
    {
        return sequencingPlatform;
    }
    public void setSequencingPlatform(SequencingPlatform sequencingPlatform)
    {
        this.sequencingPlatform = sequencingPlatform;
    }
    
    private SequenceData[] sequenceData = new SequenceData[] { new SequenceData() };
    @PropertyName("Sequence data")
    @PropertyDescription("Sequence data")
    public SequenceData[] getSequenceData()
    {
        return sequenceData;
    }
    public void setSequenceData(SequenceData[] sequenceData)
    {
        this.sequenceData = sequenceData;
    }
    

    private String sraProjectId = "";
    @PropertyName("SRA project id")
    @PropertyDescription("Project identifier in SRA database (http://www.ncbi.nlm.nih.gov/sra), example SRP017942")
    public String getSraProjectId()
    {
        return sraProjectId;
    }
    public void setSraProjectId(String sraProjectId)
    {
        this.sraProjectId = sraProjectId;
    }
    
    private String sraExperimentId = "";
    @PropertyName("SRA experiment id")
    @PropertyDescription("Experiment identifier in SRA database (http://www.ncbi.nlm.nih.gov/sra), example SRX217961")
    public String getSraExperimentId()
    {
        return sraExperimentId;
    }
    public void setSraExperimentId(String sraExperimentId)
    {
        this.sraExperimentId = sraExperimentId;
    }

    private String geoSeriesId = "";
    @PropertyName("GEO series id")
    @PropertyDescription("Series identifier in GEO database (http://www.ncbi.nlm.nih.gov/geo), example GSE41785")
    public String getGeoSeriesId()
    {
        return geoSeriesId;
    }
    public void setGeoSeriesId(String geoSeriesId)
    {
        this.geoSeriesId = geoSeriesId;
    }
    
    private String geoSampleId = "";
    @PropertyName("GEO sample id")
    @PropertyDescription("Sample identifier in GEO database (http://www.ncbi.nlm.nih.gov/geo), example GSM1024311")
    public String getGeoSampleId()
    {
        return geoSampleId;
    }
    public void setGeoSampleId(String geoSampleId)
    {
        this.geoSampleId = geoSampleId;
    }

    private Integer[] pubMedIds = new Integer[0];
    @PropertyName("PubMed identifiers")
    @PropertyDescription("The list of PubMed identifiers, example 23766421")
    public Integer[] getPubMedIds()
    {
        return pubMedIds;
    }
    public void setPubMedIds(Integer[] pubMedIds)
    {
        this.pubMedIds = pubMedIds;
    }
    
    @Override
    public String toString()
    {
        return getName();
    }
}
