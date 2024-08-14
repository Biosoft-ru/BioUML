package biouml.plugins.gtrd.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ChIPTFExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.HistonesExperiment;

//Organization of files in GTRD /bigBeds/ folder
public class BigBedFiles {
	
	public static String getBigBedNameForPeaks(Experiment exp, String peakCaller)
	{
		if(exp instanceof ChIPTFExperiment)
		{
			ChIPTFExperiment cexp = (ChIPTFExperiment) exp;
			String target = cexp.getTfTitle();
			if(target == null || target.trim().isEmpty())
				target = cexp.getTfUniprotId();
			target = target.replace('/', '-');
			return exp.getPeakId() + "_" + target +"_"+cexp.getTfUniprotId()+"_"+peakCaller.toUpperCase() + "_" + exp.getCell().getName() + ".bb";
		}
		else if(exp instanceof HistonesExperiment)
		{
			HistonesExperiment cexp = (HistonesExperiment) exp;
			String target = cexp.getTarget();
			target = target.replace('/', '-');
			return exp.getPeakId() + "_" + target + "_"+peakCaller.toUpperCase() + "_" + exp.getCell().getName() + ".bb";
		}
		else
		{
			return exp.getPeakId() + "_" + peakCaller.toUpperCase() + "_" + exp.getCell().getName() + ".bb";
		}
	}
	
	public static String getBigBedNameForPeaks(DNaseExperiment exp, String peakCaller, String repNum)
	{
		return exp.getPeakId() + "_rep" + repNum + "_" + peakCaller.toUpperCase() + "_" + exp.getCell().getName() + ".bb";
	}
	
	public static String getBigBedPathForPeaks(Experiment exp, String peakCaller)
	{
		String fileName = getBigBedNameForPeaks(exp, peakCaller);
		return getBBPeaksFolder(exp, peakCaller) + "/" + fileName;
	}
	
	public static String getBigBedPathForPeaks(DNaseExperiment exp, String peakCaller, String repNum)
	{
		String fileName = getBigBedNameForPeaks(exp, peakCaller, repNum);
		return getBBPeaksFolder(exp, peakCaller) + "/" + fileName;
	}
	
	public static String getBBPeaksFolder(Experiment exp, String peakCaller)
	{
	    String genomeBuild = getUCSCGenomeBuild( exp.getSpecie().getLatinName() );
		return  "/bigBeds/" + genomeBuild + "/" + exp.getDesign() + "/Peaks/" + peakCaller.toUpperCase();
	}

	public static String getClustersByCellPath(String expDesign, CellLine cell, String peakCaller, int version, String genomeBuild)
	{
		String fileName = getClustersByCellFileName(expDesign, cell, peakCaller, version);
		return  getClustersByCellFolder(expDesign, genomeBuild, peakCaller) + fileName;
	}
	public static String getClustersByCellFolder(String expDesign, String genomeBuild, String peakCaller)
	{
		return "/bigBeds/" + genomeBuild + "/" + expDesign + "/Clusters_by_Cell_Type/" + peakCaller.toUpperCase() + "/";
	}
	
	public static String getClustersByCellFileName(String expDesign, CellLine cell, String peakCaller, int version)
	{
		return expDesign + "_from_cell_id_" + cell.getName() + "_" + peakCaller.toUpperCase() + ".v" + version + ".bb";
	}
	
	
	public static String getClustersByCellAndTargetPath(String expDesign, CellLine cell, String target, String peakCaller, int version, String genomeBuild)
	{
		String fileName = getClustersByCellAndTargetFileName(expDesign, cell, target, peakCaller, version);
		return getClustersByCellAndTargetFolder(expDesign, genomeBuild, peakCaller) + fileName;
	}

	public static String getClustersByCellAndTargetFileName(String expDesign, CellLine cell, String target, String peakCaller, int version) {
		return target + "_" + expDesign + "_from_cell_id_" + cell.getName() + "_" + peakCaller.toUpperCase() + ".v" + version + ".bb";
	}
	
	public static String getClustersByCellAndTargetFolder(String expDesign, String genomeBuild, String peakCaller)
	{
		return "/bigBeds/" + genomeBuild + "/" + expDesign + "/Clusters_by_Target_and_Cell_Type/" + peakCaller.toUpperCase() + "/";
	}
	
    public static String getBigBedPathForMetaclusters(String tfUniprotId, String organism)
    {
        String genomeBuild = getUCSCGenomeBuild( organism );
        return "/bigBeds/"+genomeBuild+"/ChIP-seq/Meta-clusters_by_TF/"+tfUniprotId+"_ChIP-seq_Meta-clusters.bb";
    }

	
	private static Map<String, String> genomeBuildByLatinName = new HashMap<>();
	static {
	    genomeBuildByLatinName.put( "Arabidopsis thaliana", "tair10" );
        genomeBuildByLatinName.put( "Caenorhabditis elegans", "dm6" );
        genomeBuildByLatinName.put( "Mus musculus", "mm10" );
        genomeBuildByLatinName.put( "Danio rerio", "danRer11" );
        genomeBuildByLatinName.put( "Rattus norvegicus", "rn6" );
        genomeBuildByLatinName.put( "Schizosaccharomyces pombe", "spo2" );
        genomeBuildByLatinName.put( "Homo sapiens", "hg38" );
        genomeBuildByLatinName.put( "Drosophila melanogaster", "dm6" );
        genomeBuildByLatinName.put( "Saccharomyces cerevisiae", "sacCer3" );
	}
    public static String getUCSCGenomeBuild(String organism)
    {
        return genomeBuildByLatinName.get( organism );
    }
    
    public static void setUCSCGenomeBuild(String organism, String ucscGenomeBuild)
    {
        genomeBuildByLatinName.put(organism, ucscGenomeBuild);
    }
    
    public static class VersionParseResult
    {
        public String name;
        public String base;
        public int version;
    }

    //The name should be base.v123.ext, other names will be skipped
    public static List<VersionParseResult> parseVersions(Collection<String> names)
    {
        List<VersionParseResult> result = new ArrayList<>();
        for(String name : names)
        {
            int dotExtIdx = name.lastIndexOf( '.' );
            int dotVerIdx = name.lastIndexOf( '.', dotExtIdx - 1 );
            if(dotExtIdx == -1 || dotVerIdx == -1 || dotExtIdx - dotVerIdx < 3 || name.charAt( dotVerIdx + 1 ) != 'v')
                continue;
            
            
            VersionParseResult r = new VersionParseResult();
            r.name = name;
            r.base = name.substring( 0, dotVerIdx );
            String verStr = name.substring( dotVerIdx+2, dotExtIdx );
            try {
                r.version = Integer.parseInt( verStr );
            }
            catch(NumberFormatException e)
            {
                continue;
            }
            result.add( r );
        }
        return result;
    }
    

    public static Map<String, VersionParseResult> findLatestVersions(Collection<String> names)
    {
        Map<String, VersionParseResult> baseToLatest = new HashMap<>();
        
        for(VersionParseResult r : parseVersions( names ))
        {
            VersionParseResult cur = baseToLatest.get( r.base );
            if( cur == null || cur.version < r.version)
                baseToLatest.put(r.base, r);
        }
        return baseToLatest;
    }

    
}
