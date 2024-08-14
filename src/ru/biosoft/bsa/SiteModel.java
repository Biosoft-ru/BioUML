package ru.biosoft.bsa;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.ClassIcon;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * SiteModel parent just to make type more specific
 */
@ClassIcon("resources/site_model.gif")
@PropertyName("site model")
public abstract class SiteModel extends DataElementSupport implements CloneableDataElement
{
    public static final String SITE_MODEL_PROPERTY = "siteModel";
    protected static final PropertyDescriptor SITE_MODEL_PD = StaticDescriptor.create(SITE_MODEL_PROPERTY);
    
    protected SortedMap<String, String> thresholdTemplates;
    private double threshold = -Double.MAX_VALUE;

    public SiteModel(String name, DataCollection<?> origin, double threshold)
    {
        this(name, origin, threshold, null);
    }
    
    public SiteModel(String name, DataCollection<?> origin, double threshold, java.util.Map<String, String> thresholdTemplates)
    {
        super(name, origin);
        this.threshold = threshold;
        this.thresholdTemplates = thresholdTemplates == null ? new TreeMap<>() : new TreeMap<>(thresholdTemplates);
    }
    
    public void setThresholdTemplates(java.util.Map<String, String> thresholdTemplates)
    {
        this.thresholdTemplates = thresholdTemplates == null ? new TreeMap<>() : new TreeMap<>(thresholdTemplates);
    }
    
    public Collection<String> getThresholdTemplates()
    {
        return thresholdTemplates.keySet();
    }
    
    public String getTemplateData(String name) throws IllegalArgumentException
    {
        String templateData = thresholdTemplates.get(name);
        if(templateData == null) throw new IllegalArgumentException();
        return templateData;
    }
    
    public void setThresholdTemplate(String name) throws IllegalArgumentException
    {
        String newThreshold = thresholdTemplates.get(name);
        if(newThreshold == null) throw new IllegalArgumentException();
        setThreshold(Double.valueOf(newThreshold));
    }
    
    private PValueCutoff pvalueCutoff;
    public void setPValueCutoff(PValueCutoff pvalueCutoff)
    {
        this.pvalueCutoff = pvalueCutoff;
    }
    public PValueCutoff getPValueCutoff()
    {
        return pvalueCutoff;
    }
    
    @PropertyName("Cutoff")
    @PropertyDescription("Matrix cutoff")
    public double getThreshold()
    {
        return threshold;
    }
    public void setThreshold(double threshold)
    {
        this.threshold = threshold;
    }

    public abstract int getLength();

    public abstract BindingElement getBindingElement();

    public abstract Alphabet getAlphabet();

    public abstract double getScore(Sequence sequence, int position);

    public Site findBestSite(Sequence sequence)
    {
        if( sequence.getLength() < getLength() )
            return null;

        double bestScore = getScore(sequence, sequence.getStart());
        Site bestSite = constructSite(sequence, sequence.getStart(), bestScore);
        // Note that it's necessary to construct the site during the iteration rather than once at the end as
        // some site models (IPSSiteModel) may use some cached information from last getScore to constructSite
        for( int i = sequence.getStart() + 1; i < sequence.getStart() + sequence.getLength() - getLength() + 1; i++ )
        {
            double score = getScore(sequence, i);
            if( score > bestScore )
            {
                bestScore = score;
                bestSite = constructSite(sequence, i, score);
            }
        }

        return bestSite;
    }

    public void findAllSites(Sequence sequence, WritableTrack track) throws Exception
    {
        for( int i = sequence.getStart(); i < sequence.getStart() + sequence.getLength() - getLength() + 1; i++ )
        {
            double score = getScore(sequence, i);
            if( score >= getThreshold() )
                track.addSite(constructSite(sequence, i, score));
        }
    }
    public void addSitesToIntMap(SequenceRegion sequence,ChrIntervalMap<String> additionTable) throws Exception
    {
    	
    	for( int i = sequence.getStart(); i < sequence.getStart() + sequence.getLength() - getLength() + 1; i++ )
        {
            double score = getScore(sequence, i);
            if( score >= getThreshold() )
            {
            	String chrName = sequence.getParentSequence().getName();
            	int motifStart = sequence.translatePosition(i);
            	int motifEnd = sequence.translatePosition(i + getLength());
            	additionTable.add(chrName,motifStart, motifEnd,chrName + "_" + i + "site" + "");
            }
        }
    }
    
    public double countNumberOfAllSites(Sequence sequence) throws Exception
    {
    	for( int i = sequence.getStart(); i < sequence.getStart() + sequence.getLength() - getLength() + 1; i++ )
        {
            double score = getScore(sequence, i);
            if( score >= getThreshold() )
            	return 1;
        }
    	return 0;
    }

    public Site constructSite(Sequence sequence, int position, double score)
    {
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        properties.add(new DynamicProperty(Site.SCORE_PD, Double.class, score));
        properties.add(new DynamicProperty(SITE_MODEL_PD, SiteModel.class, this));
        //the name of site come from sequence name?
        Site newSite = new SiteImpl(null, null, SiteType.TYPE_TRANSCRIPTION_FACTOR, Basis.BASIS_PREDICTED, position, getLength(), Site.PRECISION_EXACTLY,
                Site.STRAND_PLUS, sequence, properties);
        if(sequence instanceof SequenceRegion)
        {
            SubSequence subSequence = new SubSequence(sequence);
            newSite = subSequence.translateSiteBack(newSite);
        }
        return newSite;
    }
    
    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public SiteModel clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        SiteModel result = (SiteModel)super.clone(origin, name);
        result.thresholdTemplates = new TreeMap<>(thresholdTemplates);
        return result;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if( !(obj instanceof SiteModel) )
            return false;
        SiteModel that = (SiteModel)obj;
        DataElementPath thisPath = DataElementPath.create( this );
        DataElementPath thatPath = DataElementPath.create( that );
        return thisPath.equals( thatPath );
    }
    
    @Override
    public int hashCode()
    {
        return DataElementPath.create( this ).hashCode();
    }

    public double getMinScore()
    {
        return 0.0;
    }
}
