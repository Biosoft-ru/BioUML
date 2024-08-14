package biouml.plugins.riboseq.transcripts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.biosoft.bsa.DiscontinuousCoordinateSystem;
import ru.biosoft.bsa.DiscontinuousSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;

/**
 * All locations are zero based 
 */
public class Transcript
{
    private String name;
    private String chromosome;
    private Interval location;
    private boolean isOnPositiveStrand;
    private List<Interval> exonLocations;
    private List<Interval> cdsLocations;
    private int length;
    
    public Transcript(String name, String chromosome, Interval location, boolean isOnPositiveStrand,
            List<Interval> exonLocations, List<Interval> cdsLocations)
    {
        this.name = name;
        this.chromosome = chromosome;
        this.location = location;
        this.isOnPositiveStrand = isOnPositiveStrand;
        this.exonLocations = exonLocations;
        this.cdsLocations = cdsLocations != null?
                cdsLocations : Collections.<Interval>emptyList();
        for(Interval exon : exonLocations)
            length += exon.getLength();
    }

    public String getName()
    {
        return name;
    }
    
    public String getChromosome()
    {
        return chromosome;
    }
    
    /**
     * @return genomic interval occupied by transcript
     */
    public Interval getLocation()
    {
        return location;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public boolean isOnPositiveStrand()
    {
        return isOnPositiveStrand;
    }

    /**
     * @return genomic intervals of exons in ascending order
     * */
    public List<Interval> getExonLocations()
    {
        return exonLocations;
    }
    
    /**
     * @return interval of coding part in transcript coordinate system, empty list for non coding transcripts
     */
    public List<Interval> getCDSLocations()
    {
        return cdsLocations;
    }
    
    public boolean isCoding()
    {
        return !cdsLocations.isEmpty();
    }

    
    public Sequence getSequence(Sequence chrSequence) throws Exception
    {
        List<Interval> exonLocationsOnChr = new ArrayList<>();
        for( Interval i : getExonLocations() )
            exonLocationsOnChr.add( i.shift( chrSequence.getStart() ) );
        DiscontinuousCoordinateSystem coordSystem = new DiscontinuousCoordinateSystem( exonLocationsOnChr, !isOnPositiveStrand() );
        Sequence processedSequence = new DiscontinuousSequence( getName(), chrSequence, coordSystem );
        return processedSequence;
    }

    /**
     * @return starts of coding part in transcript coordinate system, empty list for non coding transcripts
     */
    public List<Integer> getStartInitializations()
    {
        final List<Integer> startInitializationList = new ArrayList<>();
        final List<Interval> cdsLocationList = getCDSLocations();

        for( Interval cdsLocation : cdsLocationList )
        {
            startInitializationList.add( cdsLocation.getFrom() );
        }

        return startInitializationList;
    }
}
