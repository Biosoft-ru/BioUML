/* $Id$ */

package biouml.plugins.gtrd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.TextUtil;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrix.DataMatrixConstructor;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample.DensityEstimation;

import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * @author yura
 *
 */
public class SiteUtils
{
    public static String SITE_LENGTH = "Length";
    
    public static String[] getAvailablePropertiesNames(Track track)
    {
        DataCollection<Site> sites = track.getAllSites();
        if( sites.getSize() <= 0 ) return null;
        for( Site site: sites )
        {
            DynamicPropertySet properties = site.getProperties();
            if( properties.isEmpty() ) return null;
            return properties.asMap().keySet().toArray(new String[0]);
        }
        return null;
    }
    
    public static double[] getProperties(Site site, String[] propertiesNames)
    {
        double[] properties = new double[propertiesNames.length];
        DynamicPropertySet dps = site.getProperties();
        for( int i = 0; i < propertiesNames.length; i++ )
        {
            if( propertiesNames[i].equals(SITE_LENGTH) )
                properties[i] = site.getLength();
            else
            {
                String string = dps.getValueAsString(propertiesNames[i]);
                properties[i] = string != null ? Double.parseDouble(string) : Double.NaN;
            }
        }
        return properties;
    }
    
    public static DataMatrix getProperties(Track track, String[] propertiesNames)
    {
        DataMatrixConstructor dmc = new DataMatrixConstructor(propertiesNames);
        int i = 0;
        for( Site site : track.getAllSites() )
        	dmc.addRow("Site_" + Integer.toString(i++), getProperties(site, propertiesNames));
        return dmc.getDataMatrix();
    }
    
    public static Interval changeInterval(Interval interval, int minimalLengthOfInterval, int maximalLengthOfInterval)
    {
        int length = interval.getLength();
        if( minimalLengthOfInterval > 0 && length < minimalLengthOfInterval )
        {
            int left = Math.max(1, interval.getCenter() - minimalLengthOfInterval / 2);
            return new Interval(left, left + minimalLengthOfInterval - 1);
        }
        if( maximalLengthOfInterval > 0 && length > maximalLengthOfInterval )
        {
            int left = Math.max(1, interval.getCenter() - maximalLengthOfInterval / 2);
            return new Interval(left, left + maximalLengthOfInterval - 1);
        }
        return interval;
    }
    
    /***************** TransfacSites: start *********************/
    public static class TransfacSites
    {
    	TransfacSite[] transfacSites;
    	
    	private TransfacSites(TransfacSite[] transfacSites)
    	{
    		this.transfacSites = transfacSites;
    	}
    	
    	/////////////// Description of 3 input files //////////////
    	////////////////////// 1. /////////////////////////////////
    	// pathToFileWithTranspathIdAndUniprotId is path to tab-delimited file; this file has to contains 3 columns:
    	// 1-st column (rowNames) of file must contain transpath_IDs
    	// Column with column name 'uniprot_id' must contain uniprot-IDs
    	// Column with column name 'species' must contain species
    	// Example of file:
    	//transpath_id	uniprot_id	species	type	hub
    	//MO000000001	P00517	Bos taurus	0	0
    	//MO000000001	P17612	Homo sapiens	0	0
    	//MO000000001	P22694	Homo sapiens	0	0
    	//MO000000002	M0R5T4	Rattus norvegicus	0	0
    	//MO000000002	O14807	Homo sapiens	0	0
    	/////////////////////// 2. ////////////////////////////////
    	// pathToFileWithTranspathIdAndUniprotId is path to standard TRANSFAC file "factor.dat"
    	///////////////////\/// 3. /////////////////////////////////
    	// pathToFileWithTransfacSites is path to standard TRANSFAC file "site.dat"
    	public TransfacSites(DataElementPath pathToFileWithTranspathIdAndUniprotId, DataElementPath pathToFileWithTransfacFactors, DataElementPath pathToFileWithTransfacSites, String uniprotId)
    	{
    		this(getTransfacSitesForGivenUniprotId(pathToFileWithTranspathIdAndUniprotId, pathToFileWithTransfacFactors, pathToFileWithTransfacSites, uniprotId));
    	}
    	
    	private static TransfacSite[] getTransfacSitesForGivenUniprotId(DataElementPath pathToFileWithTranspathIdAndUniprotId, DataElementPath pathToFileWithTransfacFactors, DataElementPath pathToFileWithTransfacSites, String uniprotId)
    	{
    		Object[] objects = getTranspathIds(pathToFileWithTranspathIdAndUniprotId, uniprotId);
    		String[] transpathIds = (String[])objects[0];
    		String species = (String) objects[1];
        	
    		log.info("transpathIds.length = " + transpathIds.length);
    		objects = getTransfacSiteIds(pathToFileWithTransfacFactors, transpathIds, species);
    		if( objects == null ) return null;
    		String[] transfacSiteIds = (String[]) objects[0];
    		Integer[] qualities = (Integer[]) objects[1];
    		log.info("transfacSiteIds.length = " + transfacSiteIds.length);
    		return calculateTransfacSites(pathToFileWithTransfacSites, transfacSiteIds, uniprotId, qualities);
    	}
    	
    	private static TransfacSite[] calculateTransfacSites(DataElementPath pathToFileWithTransfacSites, String[] transfacSiteIds, String uniprotId, Integer[] qualities)
    	{
    		TransfacSite[] tfSites = new TransfacSite[transfacSiteIds.length];
    		String[] lines = TableAndFileUtils.readLinesInFile(pathToFileWithTransfacSites);
    		String[] rowNames = getRowNames(lines);
    		
    		log.info("*************** Treatment of site.dat ***************");
    		log.info("lines.length = " + lines.length);
    		
    		//
        	int indexStart = 0, indexEnd = 0;
            for( int i = 1; i < lines.length; i++ )
            {
            	if( rowNames[i].equals("AC") )
            		indexStart = i;
            	if( rowNames[i].equals("//") )
            	{
            		indexEnd = i;
            		String s = lines[indexStart].replace("  ", " ");
                	String[] strings = TextUtil.split(s, ' ');
                	int index = ArrayUtils.indexOf(transfacSiteIds, strings[1]);
                	if( index < 0 ) continue;
                	
                	// Identification of transfac site coordinates
                	for( int j = indexStart; j < indexEnd; j++ )
                    {
                    	if( ! rowNames[j].equals("SC") ) continue;
                    	int strand = lines[j].contains("FORWARD") ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
                		s = lines[j].replace("  ", " ");
                    	s = s.replace(":", " ");
                    	s = s.replace("..", " ");
                    	strings = TextUtil.split(s, ' ');
                    	String chromosomeName = strings[1].substring(3, strings[1].length());
                    	int start = Integer.valueOf(strings[2]), end = Integer.valueOf(strings[3]);
                    	tfSites[index] = new TransfacSite(chromosomeName, new Interval(start, end), strand, uniprotId, qualities[index]);
                		log.info("index = " + index + " chromosomeName = " + chromosomeName + " start = " + start + " end = " + end + " strand = " + strand);
                    	break;
                    }
            	}
            }

            //
    		List<TransfacSite> list = new ArrayList<>();
            for( TransfacSite tf : tfSites )
            	if( tf != null )
            		list.add(tf);
    		return list.toArray(new TransfacSite[0]);
    	}
    	
    	private static Object[] getTranspathIds(DataElementPath pathToFileWithTranspathIdAndUniprotId, String uniprotId)
    	{
    		String speciesResulted = null;
    		DataMatrixString dms = new DataMatrixString(pathToFileWithTranspathIdAndUniprotId, new String[]{"uniprot_id", "species"});
    		String[] uniprotIds = dms.getColumn(0), transpathIds = dms.getRowNames(), species = dms.getColumn(1);
    		List<String> list = new ArrayList<>();
            for( int i = 0; i < uniprotIds.length; i++ )
            	if( uniprotIds[i].equals(uniprotId) )
            	{
            		list.add(transpathIds[i]);
            		speciesResulted = species[i];
            	}
    		return new Object[]{list.toArray(new String[0]), speciesResulted};
    	}
    	
    	private static Object[] getTransfacSiteIds(DataElementPath pathToFileWithTransfacFactors, String[] transpathIds, String species)
    	{
    		//
    		String[] lines = TableAndFileUtils.readLinesInFile(pathToFileWithTransfacFactors);
    		String[] rowNames = getRowNames(lines);
    		List<String> list = new ArrayList<>();
    		List<Integer> listQuality = new ArrayList<>();
    		log.info("************** Treatment of factor.dat ************************");

    		//
        	int indexStart = 0, indexEnd = 0;
            for( int i = 1; i < lines.length; i++ )
            {
            	if( rowNames[i].equals("AC") )
            		indexStart = i;
            	if( rowNames[i].equals("//") )
            	{
            		indexEnd = i;
            		if( ! doContainRequiredTranspathIds(lines, rowNames, transpathIds, indexStart, indexEnd) ) continue;
            		Object[] objects = getTransfacSiteIdsAndQualities(lines, rowNames, indexStart, indexEnd, species);
            		if( objects[0] == null ) continue;
            		String[] transfacSiteIds = (String[])objects[0];
            		Integer[] qualities = (Integer[]) objects[1];
                    for( int j = 0; j < transfacSiteIds.length; j++ )
                    {
            			list.add(transfacSiteIds[j]);
            			listQuality.add(qualities[j]);
                    }
            	}
            }
    		return new Object[]{list.isEmpty() ? null : list.toArray(new String[0]), listQuality.isEmpty() ? null : listQuality.toArray(new Integer[0])};
    	}
    	
    	private static boolean doContainRequiredTranspathIds(String[] lines, String[] rowNames, String[] transpathIds, int indexStart, int indexEnd)
    	{
            for( int i = indexStart; i < indexEnd; i++ )
            {
            	if( ! rowNames[i].equals("DR") ) continue;
            	String[] strings = TextUtil.split(lines[i], ' ');
            	String s = strings[strings.length - 1];
            	s = s.replace(".", "");
            	if( ArrayUtils.contains(transpathIds, s) ) return true;
            }
    		return false;
    	}
    	
    	private static Object[] getTransfacSiteIdsAndQualities(String[] lines, String[] rowNames, int indexStart, int indexEnd, String species)
    	{
    		List<String> list = new ArrayList<>();
    		List<Integer> listQuality = new ArrayList<>();
            for( int i = indexStart; i < indexEnd; i++ )
            {
            	if( ! rowNames[i].equals("BS") ) continue;
            	if( ! lines[i].contains(species) ) continue;
            	String s = lines[i].replace(";", "");
            	s = s.replace(":", " ");
            	s = s.replace("  ", " ");
            	String[] strings = TextUtil.split(s, ' ');
        		list.add(strings[1]);
        		Integer quality = null;
        		if( strings[3].equals("Quality") && strings[4].length() == 1 )
        			quality = Integer.decode(strings[4]);
        		listQuality.add(quality);
            }
            return new Object[]{list.isEmpty() ? null : list.toArray(new String[0]), listQuality.isEmpty() ? null : listQuality.toArray(new Integer[0])};
    	}
    	
    	private static String[] getRowNames(String[] lines)
    	{
    		String[] result = new String[lines.length];
            for( int i = 0; i < lines.length; i++ )
            	result[i] = lines[i].substring(0, 2);
            return result;
    	}
    	
        public String toString()
        {
        	String result = "ID\tchromosome\tstart\tend\tstrand\tquality\tuniprot_id";
            for( int i = 0; i < transfacSites.length; i++ )
            {
            	Integer quality = transfacSites[i].getQuality();
            	String qualityAsString = quality == null ? "null" : Integer.toString(quality); 
            	result += "\nS_" + Integer.toString(i) + "\t" + transfacSites[i].getChromosomeName() + "\t" +
            			Integer.toString(transfacSites[i].getStartPosition()) + "\t" +
            			Integer.toString(transfacSites[i].getFinishPosition()) + "\t" +
            			Integer.toString(transfacSites[i].getStrand()) + "\t" +
            			qualityAsString + "\t" + transfacSites[i].getUniprotId();
            }
        	return result;
        }
        
        public Object[] calculateDistanceBetweenTransfacSitesAndMetaClusters(int w, DataElementPath pathToFolderWithMetaClusterTracks, String trackName)
        {
        	int ww = w / 2;
        	ru.biosoft.access.core.DataElementPath pathToTrack = pathToFolderWithMetaClusterTracks.getChildPath(trackName);
            Track track = pathToTrack.getDataElement(Track.class);
        	double[] distances = new double[transfacSites.length];
            for( int i = 0; i < transfacSites.length; i++ )
            {
                int center = (transfacSites[i].getFinishPosition() + transfacSites[i].getFinishPosition()) / 2, start = center - ww, end = center + ww;  
                DataCollection<Site> sites = track.getSites(transfacSites[i].getChromosomeName(), start, end);
                int size = sites.getSize();
                if( size < 1 )
                	distances[i] = ww;
                else
                {
                	int index = 0;
                	double[] array = new double[size];
                	for( Site site : sites )
                		array[index++] = (double)Math.abs(center - site.getInterval().getCenter());
                	Object[] objects = PrimitiveOperations.getMin(array);
                	distances[i] = (double)objects[1];
                }
            }
        	double[] meanAndSigma = UnivariateSample.getMeanAndSigma(distances), minAndMax = PrimitiveOperations.getMinAndMax(distances);
            Chart chart = DensityEstimation.createChartWithSmoothedDensities(new double[][]{distances}, null, "Distance", true, null, DensityEstimation.WINDOW_WIDTH_01, null);
        	return new Object[]{meanAndSigma, minAndMax, chart};
        }
        
        public double[] calculateFirstTypeErrorByTransfacSites(int w, DataElementPath pathToFolderWithMetaClusterTracks, String trackName)
        {
        	int ww = w / 2, coveredNumber = 0;
            Track track = pathToFolderWithMetaClusterTracks.getChildPath(trackName).getDataElement(Track.class);
            for( int i = 0; i < transfacSites.length; i++ )
            {
            	int center = (transfacSites[i].getFinishPosition() + transfacSites[i].getFinishPosition()) / 2, start = center - ww, end = center + ww;  
            	DataCollection<Site> dc = track.getSites(transfacSites[i].getChromosomeName(), start, end);
                if( dc.getSize() > 0 )
                	coveredNumber++;
            }
        	return new double[]{(double)transfacSites.length, (double)(transfacSites.length - coveredNumber) / (double)transfacSites.length};
        }
    	
        /***************** TransfacSite: start *********************/
        public static class TransfacSite
        {
        	private String chromosomeName;
            private Interval coordinates;
            private int strand;
            private String uniprotId;
            private Integer quality;
            
            public TransfacSite(String chromosomeName, Interval coordinates, int strand, String uniprotId, Integer quality)
            {
            	this.chromosomeName = chromosomeName;
            	this.coordinates = coordinates;
            	this.strand = strand;
            	this.uniprotId = uniprotId;
            	this.quality = quality;
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
            
            public String getUniprotId()
            {
            	return uniprotId;
            }
            
            public Integer getQuality()
            {
            	return quality;
            }
            
            public String toString(String siteName)
            {
            	return siteName + "\t" + chromosomeName + "\t" + Integer.toString(strand) + "\t" + Integer.toString(coordinates.getFrom()) + "\t" + Integer.toString(coordinates.getTo()) + "\t" + Integer.toString(quality) + "\t" + uniprotId;
            }
        }
        /***************** TransfacSite: end *********************/
    }
    /***************** TransfacSites: end *********************/
    
    private static Logger log = Logger.getLogger(SiteUtils.class.getName());


}
