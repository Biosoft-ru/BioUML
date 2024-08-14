package biouml.plugins.gtrd.legacy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ExternalReference;
import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.ClassIcon;

@PropertyName("experiment")
@ClassIcon("resources/experiment.gif")
public class Experiment extends DataElementSupport
{
    private String antibody;
    private String tfClassId;
    private String tfTitle;
    private CellLine cell;
    private Species specie;
    private String treatment;
    private String controlId;
    private DataElementPathSet reads;
    private DataElementPath alignment;
    private DataElementPath peak;
    private List<ExternalReference> externalRefs = new ArrayList<>();
    private Map<String, Map<String, String>> properties = new HashMap<>();
    
    public Experiment(DataCollection parent, String id, String antibody, String tfClassId, CellLine cell, Species specie,
            String treatment, String controlId) throws Exception
    {
        super(id, parent);
        this.antibody = antibody;
        this.tfClassId = tfClassId;
        this.cell = cell;
        this.specie = specie;
        this.treatment = treatment;
        this.controlId = controlId;
    }

    public String getAntibody()
    {
        return antibody;
    }
    
    public void setAntibody(String antibody)
    {
        this.antibody = antibody;
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

    public String getControlId()
    {
        return controlId;
    }
    
    public void setControlId(String controlId)
    {
        this.controlId = controlId;
    }
    
    
    public DataElementPath getControl()
    {
        if(controlId == null)
            return null;
        return DataElementPath.create(this).getSiblingPath(controlId);
    }
    
    public boolean isControlExperiment()
    {
        return tfClassId == null;
    }

    public DataElementPathSet getReads()
    {
        return reads;
    }

    public void setReads(DataElementPathSet reads)
    {
        this.reads = reads;
    }

    public DataElementPath getAlignment()
    {
        return alignment;
    }

    public void setAlignment(DataElementPath alignment)
    {
        this.alignment = alignment;
    }

    public DataElementPath getPeak()
    {
        return peak;
    }

    public void setPeak(DataElementPath peak)
    {
        this.peak = peak;
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
    
    public void removeElementProperty(String elementId, String propertyName)
    {
        if(!properties.containsKey( elementId ))
            return;
        properties.get( elementId ).remove( propertyName );
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
    
    public List<String> getExternalIDs(String database)
    {
        List<String> result = new ArrayList<>();
        for(ExternalReference ref : getExternalRefs())
            if(ref.getExternalDB().equals( database ))
                result.add(ref.getId());
        return result;
    }

    public static Experiment getExperimentByEncodeID(Collection<Experiment> experiments, String encodeId, Collection<String> urls) throws Exception
    {
        List<Experiment> matches = getExperimentsByExternalRef( experiments, new ExternalReference( "ENCODE", encodeId ) );
        
        Experiment result = null;
        for( Experiment gtrdExp : matches)
        {
            if( new HashSet<>( gtrdExp.getReadsURLs() ).equals( new HashSet<>( urls ) ) )
            {
                if( result != null )
                    throw new Exception( "Duplicates in GTRD " + result.getName() + ", " + gtrdExp.getName() );
                result = gtrdExp;
            }
        }
        
        return result;
    }
    
    public static List<Experiment> getExperimentsByExternalRef(Collection<Experiment> experiments, ExternalReference externalRef)
    {
        List<Experiment> result = new ArrayList<>();
        for(Experiment e : experiments)
            if( e.getExternalRefs().contains( externalRef ) )
                result.add( e );
        return result;
    }
    
    public static Experiment getExperimentByEncodeID(Collection<Experiment> experiments, String encodeId, String encodeIdOfControl)
    {
        List<Experiment> matches = getExperimentsByExternalRef( experiments, new ExternalReference( "ENCODE", encodeId ) );
        if( matches.isEmpty() )
            return null;

        Experiment result = null;
        for( Experiment e : matches )
        {
            if( encodeIdOfControl == null )
            {
                if( e.getControl() == null )
                {
                    if( result != null )
                        throw new IllegalStateException( "EncodeID (" + encodeId + ") matches to " + e.getName() + " and "
                                + result.getName() );
                    result = e;
                }
            }
            else
            {
                if( e.getControl() != null )
                {
                    Experiment ctrlExp = e.getControl().getDataElement(Experiment.class);
                    if( ctrlExp.getExternalIDs( "ENCODE" ).contains( encodeIdOfControl ) )
                    {
                        if( result != null )
                            throw new IllegalStateException( "EncodeID (" + encodeId + ") with control (" + encodeIdOfControl
                                    + ") matches to " + e.getName() + " and " + result.getName() );
                        result = e;
                    }
                }
            }
        }

        return result;
    }

}