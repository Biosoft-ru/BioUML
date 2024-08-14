package biouml.plugins.gtrd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biouml.standard.type.Species;
import com.developmentontheedge.beans.annot.PropertyName;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.ClassIcon;

@PropertyName("experiment")
@ClassIcon("resources/experiment.gif")
public abstract class Experiment extends DataElementSupport
{
    protected CellLine cell;
    protected Species specie;
    protected String treatment;
    protected String peakId;
    protected String alignmentId;
    protected Set<String> readsIds = new HashSet<>();
    protected DataElementPathSet reads;
    protected List<ExternalReference> externalRefs = new ArrayList<>();
    protected Map<String, Map<String, String>> properties = new HashMap<>();
    protected List<ExperimentalFactor> experimentalFactors = new ArrayList<>();
    
    public Experiment(DataCollection<?> parent, String id)
    {
        super( id, parent );
    }
    
    public abstract String getDesign();
    
    public CellLine getCell()
    {
        return cell;
    }
    
    public void setCell(CellLine cell)
    {
        this.cell = cell;
    }

    public Species getSpecie()
    {
        return specie;
    }
    
    public void setSpecie(Species specie)
    {
        this.specie = specie;
    }

    public String getTreatment()
    {
        return treatment;
    }

    public void setTreatment(String treatment)
    {
        this.treatment = treatment;
    }
    
    public abstract String[] getPeakCallers();

	public String getPeakId() 
	{
		return peakId;
	}

	public void setPeakId(String peakId) 
	{
		this.peakId = peakId;
	}
	
    public DataElementPathSet getReads()
    {
        return reads;
    }

    public void setReads(DataElementPathSet reads)
    {
        this.reads = reads;
    }
    
    public Set<String> getReadsIds()
    {
        return readsIds;
    }

    public void setReadsIds(Set<String> readsIds)
    {
        this.readsIds = readsIds;
    }
    
    public String getAlignmentId()
    {
        return alignmentId;
    }

    public void setAlignmentId(String alignmentId)
    {
        this.alignmentId = alignmentId;
    }
    
    public List<ExternalReference> getExternalRefs()
    {
        return externalRefs;
    }
    
    public String getExternalReferences()
    {
        return StreamEx.of(externalRefs).map( ExternalReference::getId ).joining( ", " );
    }
        
    public Map<String, Map<String, String>> getProperties()
    {
        return properties;
    }
    public Map<String, String> getElementProperties(String elementId)
    {
        Map<String, String> result = properties.get( elementId );
        if( result == null )
            result = Collections.emptyMap();
        return result;
    }
    
    public void setElementProperty(String elementId, String propertyName, String propertyValue)
    {
        properties.computeIfAbsent( elementId, k -> new HashMap<>() ).put( propertyName, propertyValue );
    }
    
    public double getDoubleProperty(String elementId, String propertyName, double defaultValue)
    {
        String strVal = getElementProperties( elementId ).get(propertyName);
        if(strVal == null)
            return defaultValue;
        return Double.parseDouble( strVal );
    }
    
    public double getDoubleProperty(String elementId, String propertyName)
    {
        return getDoubleProperty( elementId, propertyName, Double.NaN );
    }
    
    public List<ExperimentalFactor> getExperimentalFactors() 
    {
		return experimentalFactors;
	}
	
	public void removeElementProperty(String elementId, String propertyName)
    {
        if(!properties.containsKey( elementId ))
            return;
        properties.get( elementId ).remove( propertyName );
    }
    
    public List<String> getExternalIDs(String database)
    {
        List<String> result = new ArrayList<>();
        for(ExternalReference ref : getExternalRefs())
            if(ref.getExternalDB().equals( database ))
                result.add(ref.getId());
        return result;
    }
    
    public List<String> getReadsURLs()
    {
        List<String> result = new ArrayList<>();
        for(DataElementPath path : getReads())
        {
            String url = getElementProperties( path.getName().replaceAll( "[.].*", "" ) ).get( "url" );
            if(url != null)
                result.add( url );
        }
        return result;
    }

    
    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        Experiment other = (Experiment)obj;

//       return Objects.equal( getName(), other.getName() );
	if( getName() != null )
            return getName().equals(other.getName());

        return getName() == other.getName();
    }
}
