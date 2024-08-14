/* $Id$ */

package biouml.plugins.gtrd.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.WithSite;

/**
 * @author yura
 *
 */

//  There are to types of FunSite:
//  1-st type: it contains DataMatrix[] without DataMatrix; it is for merged sites obtained by several peak callers in the same experiment;
//  2-st type: it contains DataMatrix without DataMatrix[]; it is for merged sites obtained by same peak caller in distinct experiments;
//  Each type of Funsite is constructed by it's specific constructor. 

public class FunSite implements Comparable<FunSite> , WithSite
{
    // It is reserve.
//    public static final String SITE_LENGTH = "Length";
//    public static final String CHROMOSOME = "Chromosome";
//    public static final String START = "Start";
//    public static final String END = "End";
//    public static final String STRAND = "Strand";
//    public static final String NUMBER_OF_INITIAL_SITES = "Number_of_initial_sites";

    private String chromosomeName;
    private Interval coordinates;
    private int strand;
    private DataMatrix dataMatrix;
    private DataMatrix[] dataMatrices;
    private Object[] objects;
    private Sequence sequence;
    
    private FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix dataMatrix, DataMatrix[] dataMatrices, Sequence sequence)
    {
        this.chromosomeName = chromosomeName;
        this.coordinates = coordinates;
        this.strand = strand;
        this.dataMatrix = dataMatrix;
        this.dataMatrices = dataMatrices;
        this.sequence = sequence;
    }

    // Constructor for 1-st site type. 
    public FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix[] dataMatrices)
    {
        this(chromosomeName, coordinates, strand, null, dataMatrices, null);
    }
    public FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix[] dataMatrices, Sequence sequence)
    {
        this(chromosomeName, coordinates, strand, null, dataMatrices, sequence);
    }

    // Constructor for 2-nd site type. 
    public FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix dataMatrix)
    {
        this(chromosomeName, coordinates, strand, dataMatrix, null, null);
    }
    public FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix dataMatrix, Sequence sequence)
    {
        this(chromosomeName, coordinates, strand, dataMatrix, null, sequence);
    }
    
    @Override
    public int compareTo(FunSite o)
    {
        return coordinates.compareTo(o.coordinates);
    }
    
    public String getChromosomeName()
    {
        return chromosomeName;
    }
    
    public int getStrand()
    {
        return strand;
    }
    
    public int getStartPosition()
    {
        return coordinates.getFrom();
    }
    
    public int getFinishPosition()
    {
        return coordinates.getTo();
    }
    
    public int getLength()
    {
        return coordinates.getLength();
    }
    
    public Interval getCoordinates()
    {
        return coordinates;
    }
    
    public DataMatrix getDataMatrix()
    {
        return dataMatrix;
    }
    
    public DataMatrix[] getDataMatrices()
    {
        return dataMatrices;
    }
    
    public Object[] getObjects()
    {
        return objects;
    }
    
    public void setObjects(Object[] objects)
    {
        this.objects = objects;
    }
    
    public void changeCoordinates(int minimalLengthOfSite, int maximalLengthOfSite)
    {
        coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    }
    
    public void fromDataMatricesToDataMatrix(DataMatrix dataMatrix)
    {
        this.dataMatrix = dataMatrix;
        dataMatrices = null;
    }
    
    public void fromDataMatrixToDataMatrices()
    {
        this.dataMatrices = new DataMatrix[]{dataMatrix};
        this.dataMatrix = null;
    }

    public void calculateAveragesOfMatricesThatHaveSameRowAndColumnNames()
    {
        dataMatrices = DataMatrix.calculateAveragesOfMatricesThatHaveSameRowAndColumnNames(dataMatrices);
    }
    
    public String[] getDistinctRowNames()
    {
        if( dataMatrices == null ) return UtilsGeneral.getDistinctValues(dataMatrix.getRowNames());
        Set<String> set = new HashSet<>();
        for( DataMatrix dm : dataMatrices )
            for( String s : dm.getRowNames() )
                set.add(s);
        return set.toArray(new String[0]);
    }
    
    public String[] getDistinctColumnNames()
    {
        if( dataMatrices == null ) return UtilsGeneral.getDistinctValues(dataMatrix.getColumnNames());
        Set<String> set = new HashSet<>();
        for( DataMatrix dm : dataMatrices )
            for( String s : dm.getColumnNames() )
                set.add(s);
        return set.toArray(new String[0]);
    }
    
    public Site toSite(String scoreName, double score)
    {
        Site site = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, getStartPosition(), getLength(), Site.PRECISION_NOT_KNOWN, strand, sequence, null);
        DynamicPropertySet dps = site.getProperties();
        dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(getDistinctRowNames().length)));
        if( scoreName != null )
            dps.add(new DynamicProperty(scoreName, String.class, Double.toString(score)));
        if( objects != null )
        {
            String[] array = (String[])objects[0];
            dps.add(new DynamicProperty("Additional_property", String.class, UtilsForArray.toString(array, ",")));
        }
        return site;
    }
    
    public Site getSite()
    {
    	String name = chromosomeName + ":" + getStartPosition() + "-" + getStartPosition() + getLength();
        Site site = new SiteImpl(null, name, null, Site.BASIS_USER_ANNOTATED, getStartPosition(), getLength(), Site.PRECISION_NOT_KNOWN, strand, sequence, null);
        return site;
    }
    
    
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(); 
        builder.append("chr\t").append(chromosomeName).append("\tcoord\t").append(String.valueOf(coordinates.getFrom())).append("\t").append(String.valueOf(coordinates.getTo())).append("\t").append(String.valueOf(strand));
        DataMatrix[] dms = dataMatrices != null ? dataMatrices : new DataMatrix[]{dataMatrix};
        builder.append("\nnumber\t").append(String.valueOf(dms.length));
        for( DataMatrix dm : dms )
        {
            builder.append("\ndim\t").append(String.valueOf(dm.getRowNames().length)).append("\t").append(String.valueOf(dm.getColumnNames().length));
            builder.append("\n").append(dm.toString());
        }
        return builder.toString();
    }
    
    public boolean doContainGivenRowName(String givenRowName)
    {
        if( dataMatrices == null ) return ArrayUtils.contains(dataMatrix.getRowNames(), givenRowName);
        for( DataMatrix dm : dataMatrices )
            if( ArrayUtils.contains(dm.getRowNames(), givenRowName) ) return true;
        return false;
    }

    public boolean isOrphan()
    {
        if( dataMatrix != null )
        {
            if( dataMatrix.getRowNames().length == 1 ) return true;
            else return false;
        }
        if( dataMatrices.length == 1 ) return true;
        return false;
    }
    
    public Sequence getSequence() 
    {
    	return this.sequence;
    }
    
    public Sequence getSequenceRegionWithGivenLength(Sequence fullChromosome, int lengthOfSequenceRegion)
    {
        int center = coordinates.getCenter(), leftPosition = center - lengthOfSequenceRegion / 2;
        if(leftPosition < 1 )
            leftPosition = 1;
        int rightPosition = leftPosition + lengthOfSequenceRegion - 1;
        if( rightPosition >= fullChromosome.getLength() )
        {
            rightPosition = fullChromosome.getLength() - 1;
            leftPosition = rightPosition - lengthOfSequenceRegion + 1;
        }
        return new SequenceRegion(fullChromosome, new Interval(leftPosition, rightPosition), false, false);
    }
    
    public Sequence getSequenceRegion(Sequence fullChromosome)
    {
        return new SequenceRegion(fullChromosome, coordinates, false, false);
    }
}
