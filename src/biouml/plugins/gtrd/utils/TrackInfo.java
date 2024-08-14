/* $Id$ */
package biouml.plugins.gtrd.utils;
//03.04.22
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.bsa.Track;
import biouml.model.Module;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.TrackSqlTransformer;
import biouml.plugins.gtrd.legacy.Experiment;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.standard.type.Species;

/**
 * @author yura
 *
 */

public class TrackInfo
{
    public static final String[] UNTREATMENTS = {"untreated", "unstimulated", "no treat", "none", "None", "null", "NULL"};
    public static final String[] UNTREATMENTS_INCLUDED = {"no treatment", "No treatment", "Treament: none", "Treatment: control;", "Treatment: DMSO", "Treatment: Mock", "Treatment: no", "Treatment: no treatment", "Treatment: none", "Treatment: None;", "untreated", "Treatment: vehicle", "Treatment: Vehicle (edited)"};
    
    // 03.04.22
    public static final String CELL_LINE_STATUS_UNTREATED = "Only untreated cell lines are considered";
    public static final String CELL_LINE_STATUS_TREATED = "Only treated cell lines are considered";
    public static final String CELL_LINE_STATUS_ALL = "All (treated and untreated) cell lines are considered";

    String trackName;
    String tfClass;
    String cellLine;
    String specie;
    String pathToSequenceCollection;
    int numberOfSites;
    String tfName;
    String antibody;
    String treatment;
    String controlId;
    String uniprotId;
    
    private static Map<ru.biosoft.access.core.DataElementPath, Map<String, DataElementPath>> experimentMap = new ConcurrentHashMap<>();
    private static Map<ru.biosoft.access.core.DataElementPath, Map<String, DataElementPath>> newExperimentMap = new ConcurrentHashMap<>();
    
    // obtain information about general track or about GTRD ChIP-Seq track from GTRD Experiment
    public TrackInfo(Track track)
    {
        trackName = track.getName();
        DataCollectionInfo info = ((DataCollection<?>)track).getInfo();
        tfClass = info.getProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY);
        cellLine = info.getProperty(TrackSqlTransformer.CELL_LINE_PROPERTY);
        specie = info.getProperty(TrackSqlTransformer.SPECIE_PROPERTY);
        // treatment = info.getProperty
        pathToSequenceCollection = info.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
        numberOfSites = track.getAllSites().getSize();
        
        // It is used only for old versions of GTRD
//        Experiment exp = getExperiment(track);
//        if( exp != null )
//        {
//            tfClass = exp.getTfClassId();
//            tfName = exp.getTfTitle();
//            cellLine = exp.getCell().getTitle();
//            specie = exp.getSpecie().getLatinName();
//            antibody = exp.getAntibody();
//            treatment = exp.getTreatment();
//            controlId = exp.getControlId();
//        }
       
        String trackName = track.getName(), s = trackName.substring(0, 5);
        //dc = s.equals("PEAKS") ? path.getChildPath("Data", "experiments").getDataCollection(biouml.plugins.gtrd.Experiment.class) : path.getChildPath("Data", "DNase experiments").getDataCollection(biouml.plugins.gtrd.Experiment.class);
        if( s.equals("PEAKS"))
        {
        	ChIPseqExperiment newExperiment = getNewChIPseqExperiment(track);
            if( newExperiment != null )
            {
                tfClass = newExperiment.getTfClassId();//can be null for species without TFClass DB
                tfName = newExperiment.getTfTitle();
                cellLine = newExperiment.getCell().getTitle();
                specie = newExperiment.getSpecie().getLatinName();
                antibody = newExperiment.getAntibody();
                treatment = newExperiment.getTreatment();
                controlId = newExperiment.getControlId();
                uniprotId = newExperiment.getTfUniprotId(); 
            }
        }
        else
        {
            DNaseExperiment newExperiment = getNewDNaseExperiment(track);
            if( newExperiment != null )
            {
                cellLine = newExperiment.getCell().getTitle();
                specie = newExperiment.getSpecie().getLatinName();
                treatment = newExperiment.getTreatment();
            }
        }
    }
    
    // It is used only for old versions of GTRD
    private static Experiment getExperiment(Track track)
    {
        DataElementPath path = Module.optModulePath(track);
        if( path == null ) return null;
        Map<String, DataElementPath> map = experimentMap.get(path);
        if( map != null ) return map.get(track.getName()).getDataElement(Experiment.class);
        DataCollection<Experiment> dc;
        try
        {
            dc = path.getChildPath("Data", "experiments").getDataCollection(Experiment.class);
        }
        catch( RepositoryException e )
        {
            return null;
        }
        synchronized( TrackInfo.class )
        {
            map = experimentMap.get(path);
            if( map == null )
            {
                map = new HashMap<>();
                for( Experiment exp : dc )
                    if( exp.getPeak() != null )
                        map.put(exp.getPeak().getName(), DataElementPath.create(exp));
                experimentMap.put(path, map);
            }
        }
        return map.get(track.getName()).getDataElement(Experiment.class);
    }
    
    private static ChIPseqExperiment getNewChIPseqExperiment(Track track)
    {
        return getNewExperiment(track, "experiments", ChIPseqExperiment.class);
    }
    
    private static DNaseExperiment getNewDNaseExperiment(Track track)
    {
        return getNewExperiment(track, "DNase experiments", DNaseExperiment.class);
    }
    
    private static <T extends biouml.plugins.gtrd.Experiment> T getNewExperiment(Track track, String expDir, Class<T> expDesign)
    {
        DataElementPath path = Module.optModulePath(track);
        if( path == null ) return null;
        Map<String, DataElementPath> map = newExperimentMap.get(path);
        String trackPeakId = track.getName().split("_")[0]; // DNase-seq exps have suffix for rep number (DEXP000001_1)
        
        //log.info("test :::: trackPeakId = " + trackPeakId);

        if( map != null )
        {
            DataElementPath expPath = map.get(trackPeakId);
            
            //log.info("test :::: expPath = " + expPath);

            if( expPath == null ) return null;
            return expPath.getDataElement(expDesign);
        }
        DataCollection<T> dc;
        try
        {
            //log.info("::: test :::: path.getChildPath('Data', expDir) = " + path.getChildPath("Data", expDir));
            dc = path.getChildPath("Data", expDir).getDataCollection(expDesign);
        }
        catch( RepositoryException e )
        {
            return null;
        }
        synchronized( TrackInfo.class )
        {
            map = newExperimentMap.get(path);
            if( map == null )
            {
                map = new HashMap<>();
                for( T exp : dc )
                {
                    String peakId = exp.getPeakId();
                    
                 //log.info("TrackInfo: peakId = " + peakId);

                 
                    if( peakId != null )
                        map.put(peakId, DataElementPath.create(exp));
                }
                newExperimentMap.put(path, map);
            }
        }
        return map.get(trackPeakId).getDataElement(expDesign);
    }
    
    public static String getControlId(Track track)
    {
        biouml.plugins.gtrd.ChIPseqExperiment newExperiment = getNewChIPseqExperiment(track);
        if( newExperiment != null ) return newExperiment.getControlId();
        Experiment exp = getExperiment(track);
        if( exp == null ) return null;
        return exp.getControlId();
    }
    
    public String getTrackName()
    {
        return trackName;
    }

    public String getTfClass()
    {
        return tfClass;
    }

    public String getCellLine()
    {
        return cellLine;
    }

    public String getSpecie()
    {
        return specie;
    }

    public String getPathToSequenceCollection()
    {
        return pathToSequenceCollection;
    }
    
    public int getNumberOfSites()
    {
        return numberOfSites;
    }

    public String getTfName()
    {
        return tfName;
    }

    public String getAntibody()
    {
        return antibody;
    }

    public String getTreatment()
    {
        return treatment;
    }

    public String getControlId()
    {
        return controlId;
    }
    
    public String getUniprotId()
    {
        return uniprotId;
    }
    
    public void replaceTreatment()
    {
        if( treatment == null || ArrayUtils.contains(UNTREATMENTS, treatment) )
        {
            treatment = "";
            return;
        }
        if( treatment.equals("") ) return;
        for( String s : UNTREATMENTS_INCLUDED )
            if( treatment.contains(s) )
            {
                treatment = "";
                return;
            }
    }
    
    // 03.04.22 novel method
    public boolean isCellLineTreated()
    {
    	if( treatment == null ) return false;
    	replaceTreatment();
        if( treatment.equals("") ) return false;
        return true;
    }

    // 05.04.22 novel method
    public String getTfClassOrUniprotId(String tfClassificationType)
    {
    	return tfClassificationType.equals(EnsemblUtils.TF_CLASSIFICATION_TYPE_TF_CLASS) ? tfClass : uniprotId;
    }

    /*****************************************************/
    /******************** static methods *****************/
    /*****************************************************/
    
    public static String[] getAvailableCellLineStatus()
    {
        return new String[]{CELL_LINE_STATUS_UNTREATED, CELL_LINE_STATUS_TREATED, CELL_LINE_STATUS_ALL};
    }

    public static TrackInfo[] getTracksInfo(DataElementPath pathToFolderWithTracks, Species givenSpecie, String givenTfClass, DataElementPath pathToSequences)
    {
        DataCollection<DataElement> tracks = pathToFolderWithTracks.getDataCollection(DataElement.class);
        List<TrackInfo> result = new ArrayList<>();
        for( ru.biosoft.access.core.DataElement de: tracks )
        {
            if( ! (de instanceof Track) ) continue;
            Track track = (Track)de;
            TrackInfo ti = getTrackInfor(track, givenSpecie, givenTfClass, null, pathToSequences);
            if( ti != null )
                result.add(ti);
        }
        if( result.isEmpty() ) return null;
        return result.toArray(new TrackInfo[0]);
    }
    
    private static TrackInfo getTrackInfor(Track track, Species givenSpecie, String givenTfClass, String givenCellLine, DataElementPath pathToSequences)
    {
        if( track.getAllSites().getSize() <= 0 ) return null;
        TrackInfo trackInfo = new TrackInfo(track);
        if( trackInfo.getSpecie() == null ) return null;
        String pathToSequenceCollection = trackInfo.getPathToSequenceCollection();
        if( pathToSequences != null && pathToSequenceCollection != null && ! pathToSequenceCollection.equals(pathToSequences.toString()) ) return null;
        if( ! trackInfo.getSpecie().equals(givenSpecie.getLatinName()) ) return null;
        if( givenTfClass != null )
        {
            String tfClass = trackInfo.getTfClass();
            if( tfClass == null || ! tfClass.equals(givenTfClass) ) return null;
        }
        if( givenCellLine != null && ! trackInfo.getCellLine().equals(givenCellLine) ) return null;
        return trackInfo;
    }
    
    // 29.03.22. To avoid difference between GTRD-folders "GTRD" and "GTRD_2006" Semen changed 4 classes: ExperimentSQLTransformer, ChipseqexperimentSQLTransformer, HistonesexperimentSQLTransformer, ChIpexoExperimentSQLTransformer
    public static TrackInfo[] getTracksInfo(DataElementPath pathToFolderWithFolders, String[] foldersNames, Species givenSpecie, String givenTfClass, String givenCellLine, DataElementPath pathToSequences)
    {
        List<TrackInfo> result = new ArrayList<>();

        // 1. To identify distinctTrackNames
        Set<String> set = new HashSet<>();
        for(String s : foldersNames )
            for( String name : pathToFolderWithFolders.getChildPath(s).getDataCollection(DataElement.class).getNameList() )
                set.add(name);
        String[] distinctTrackNames = set.toArray(new String[0]);
        
        // 27.03.22
        log.info("distinctTrackNames.length = " + distinctTrackNames.length);

        
        // 2. Create TrackInfo[]
        // 29.03.22
        //boolean[] areTreated = new boolean[distinctTrackNames.length];
        boolean[] areTreated = UtilsForArray.getConstantArray(distinctTrackNames.length, false);
        
        for( String folderName : foldersNames )
        {
            DataElementPath path = pathToFolderWithFolders.getChildPath(folderName);
            for( int i = 0; i < distinctTrackNames.length; i++ )
            {
            	// 29.03.22
                log.info("******** folderName = " + folderName + " i = " + i + " distinctTrackNames[i] = " + distinctTrackNames[i]);

                if( areTreated[i] ) continue;
                DataElementPath pathToTrack = path.getChildPath(distinctTrackNames[i]);
                if( ! pathToTrack.exists() ) continue;
                Track track = pathToTrack.getDataElement(Track.class);
                TrackInfo ti = getTrackInfor(track, givenSpecie, givenTfClass, givenCellLine, pathToSequences);
                if( ti != null )
                {
                    result.add(ti);
                    areTreated[i] = true;
                }
            }
        }
        
    	// 29.03.22
        log.info("******** result.size() = " + result.size());
        return result.toArray(new TrackInfo[0]);
    }

    public static String[] getDistinctTracksNamesInSeveralFolders(DataElementPath pathToFolderWithFolders, Species givenSpecie, String[] foldersNames)
    {
        Set<String> result = new HashSet<>();
        for( String folderName : foldersNames )
        {
            TrackInfo[] trackInfos = getTracksInfo(pathToFolderWithFolders.getChildPath(folderName), givenSpecie, null, null);
            for( TrackInfo ti : trackInfos )
                result.add(ti.getTrackName());
        }
        return result.toArray(new String[0]);
    }
    
    public static TrackInfo[] removeTrackInfosWithoutTfClasses(TrackInfo[] trackInfos)
    {
        List<TrackInfo> list = new ArrayList<>();
        for( TrackInfo ti : trackInfos )
            if( ti.getTfClass() != null )
                list.add(ti);
        return list.toArray(new TrackInfo[0]);
    }
    
    // 03.04.22. It is replaced 
//    public static TrackInfo[] removeTrackInfosWithCellTreatments(TrackInfo[] trackInfos)
//    {
//        List<TrackInfo> list = new ArrayList<>();
//        for( TrackInfo ti : trackInfos )
//        {
//            String treatment = ti.getTreatment();
//            if( treatment != null && treatment.equals("") )
//               list.add(ti);
//        }
//        return list.toArray(new TrackInfo[0]);
//    }
    public static TrackInfo[] removeTrackInfosWithCellTreatments(TrackInfo[] trackInfos)
    {
        List<TrackInfo> list = new ArrayList<>();
        for( TrackInfo ti : trackInfos )
        {
        	ti.replaceTreatment();
            String treatment = ti.getTreatment();
            if( treatment == null || treatment.equals("") )
               list.add(ti);
        }
        return list.toArray(new TrackInfo[0]);
    }
    
    // 03.04.22
    public static TrackInfo[] selectTrackInfosWithGivenCellTreatmentStatus(TrackInfo[] trackInfos, String cellLineStatus)
    {
        if( cellLineStatus.equals(CELL_LINE_STATUS_ALL) ) return trackInfos;
    	List<TrackInfo> list = new ArrayList<>();
        for( TrackInfo ti : trackInfos )
        {
        	ti.replaceTreatment();
            boolean isTreated = ti.isCellLineTreated();
            if( (isTreated && cellLineStatus.equals(CELL_LINE_STATUS_UNTREATED)) || ( ! isTreated && cellLineStatus.equals(CELL_LINE_STATUS_TREATED))) continue;
            list.add(ti);
        }
        return list.toArray(new TrackInfo[0]);
    }
    
    public static String[] getDistinctTfClasses(TrackInfo[] trackInfos)
    {
        Set<String> set = new HashSet<>();
        for( TrackInfo ti : trackInfos )
        {
            String tfClass = ti.getTfClass();
            if( tfClass != null )
                set.add(tfClass);
        }
        return set.toArray(new String[0]);
    }
    
    // 04.04.22 new
    public static String[] getDistinctUniprotIds(TrackInfo[] trackInfos)
    {
        Set<String> set = new HashSet<>();
        for( TrackInfo ti : trackInfos )
        {
            String uniprotId = ti.getUniprotId();
            if( uniprotId != null )
                set.add(uniprotId);
        }
        return set.toArray(new String[0]);
    }
    
    // 04.04.22
    public static String[] getDistinctTfClassesOrUniprotIds(TrackInfo[] trackInfos, String tfClassificationType)
    {
    	return tfClassificationType.equals(EnsemblUtils.TF_CLASSIFICATION_TYPE_TF_CLASS) ? getDistinctTfClasses(trackInfos) : getDistinctUniprotIds(trackInfos);
    }
    
    public static String[] getTfNames(TrackInfo[] trackInfos, String[] tfClasses)
    {
        String[] result = new String[tfClasses.length];
        for( int i = 0; i < tfClasses.length; i++ )
            for ( TrackInfo ti : trackInfos )
            {
                String tfClass = ti.getTfClass();
                if( tfClass != null && tfClass.equals(tfClasses[i]) )
                {
                    result[i] = ti.getTfName();
                    break;
                }
            }
        return result;
    }
    
    public static String[] getDistinctCellLines(TrackInfo[] trackInfos)
    {
        Set<String> set = new HashSet<>();
        for( TrackInfo ti : trackInfos )
        {
            String cellLine = ti.getCellLine();
            if( cellLine != null )
                set.add(cellLine);
        }
        return set.toArray(new String[0]);
    }
    
    public static String[] getDistinctTreatments(TrackInfo[] trackInfos)
    {
        Set<String> set = new HashSet<>();
        for( TrackInfo ti : trackInfos )
        {
            String treatment = ti.getTreatment();
            if( treatment != null )
                set.add(treatment);
        }
        return set.toArray(new String[0]);
    }
    
    public static String[] getTrackNames(TrackInfo[] trackInfos, String givenTfClass, String givenTreament, String givenCellLine)
    {
        List<String> list = new ArrayList<>();
        for( TrackInfo ti : trackInfos )
        {
            String tfClass = ti.getTfClass(), treatment = ti.getTreatment(), cellLine = ti.getCellLine();
            if( givenTfClass != null && tfClass != null && ! tfClass.equals(givenTfClass) ) continue;
            if( givenTreament != null && treatment != null && ! treatment.equals(givenTreament) ) continue;
            if( givenCellLine != null && cellLine != null && ! cellLine.equals(givenCellLine) ) continue;
            list.add(ti.getTrackName());
        }
        return list.toArray(new String[0]);
    }
    
    // 05.04.22 novel; it is extension of method getTrackNames(TrackInfo[] trackInfos, String givenTfClass, String givenTreament, String givenCellLine);
    public static String[] getTrackNames(TrackInfo[] trackInfos, String tfClassificationType, String givenTfClassOrUniprotId, String givenTreament, String givenCellLine)
    {
        List<String> list = new ArrayList<>();
        for( TrackInfo ti : trackInfos )
        {
            String tfClassOrUniprotId = ti.getTfClassOrUniprotId(tfClassificationType), treatment = ti.getTreatment(), cellLine = ti.getCellLine();
            if( givenTfClassOrUniprotId != null && tfClassOrUniprotId != null && ! tfClassOrUniprotId.equals(givenTfClassOrUniprotId) ) continue;
            if( givenTreament != null && treatment != null && ! treatment.equals(givenTreament) ) continue;
            if( givenCellLine != null && cellLine != null && ! cellLine.equals(givenCellLine) ) continue;
            list.add(ti.getTrackName());
        }
        return list.toArray(new String[0]);
    }
    
    public static TrackInfo[] getSubsetOfTrackInfos(TrackInfo[] trackInfos, String[] trackNames)
    {
        List<TrackInfo> list = new ArrayList<>();
        for( TrackInfo ti : trackInfos )
        {
            String trackName = ti.getTrackName();
            if( ArrayUtils.contains(trackNames, trackName) )
                list.add(ti);
        }
        return list.toArray(new TrackInfo[0]);
    }
    
//  String trackName = track.getName(), s = trackName.substring(0, 5);
//  dc = s.equals("PEAKS") ? path.getChildPath("Data", "experiments").getDataCollection(biouml.plugins.gtrd.Experiment.class) : path.getChildPath("Data", "DNase experiments").getDataCollection(biouml.plugins.gtrd.Experiment.class);
//  // dc = getDataCollection(biouml.plugins.gtrd.DNaseExperiment.class)

    
    protected static Logger log = Logger.getLogger(TrackInfo.class.getName());
}
