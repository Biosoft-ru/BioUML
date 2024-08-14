
package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.bsa.Track;
import biouml.model.Module;
import biouml.plugins.gtrd.legacy.Experiment;
import biouml.plugins.gtrd.TrackSqlTransformer;
import biouml.standard.type.Species;

/**
 * @author yura
 *
 */

public class TrackInfo
{
    // it is copied
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
    private static Map<ru.biosoft.access.core.DataElementPath, Map<String, DataElementPath>> experimentMap = new ConcurrentHashMap<>();
    private static Map<ru.biosoft.access.core.DataElementPath, Map<String, DataElementPath>> newExperimentMap = new ConcurrentHashMap<>();
    
    public TrackInfo(DataElementPath pathToGtrdChipSeqTrack)
    {
        this(pathToGtrdChipSeqTrack.getDataElement(Track.class));
    }
    
    // it is copied
    // obtain information about general track or about GTRD ChIP-Seq track from GTRD Experiment
    public TrackInfo(Track track)
    {
        trackName = track.getName();
        DataCollectionInfo info = ((DataCollection<?>)track).getInfo();
        tfClass = info.getProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY);
        cellLine = info.getProperty(TrackSqlTransformer.CELL_LINE_PROPERTY);
        specie = info.getProperty(TrackSqlTransformer.SPECIE_PROPERTY);
        pathToSequenceCollection = info.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
        numberOfSites = track.getAllSites().getSize();
        Experiment exp = getExperiment(track);
        if( exp != null )
        {
            tfClass = exp.getTfClassId();
            tfName = exp.getTfTitle();
            cellLine = exp.getCell().getTitle();
            specie = exp.getSpecie().getLatinName();
            antibody = exp.getAntibody();
            treatment = exp.getTreatment();
            controlId = exp.getControlId();
        }
        
        biouml.plugins.gtrd.ChIPseqExperiment newExperiment = getNewExperiment( track );
        if( newExperiment != null )
        {
            tfClass = newExperiment.getTfClassId();//can be null for species without TFClass DB
            tfName = newExperiment.getTfTitle();
            cellLine = newExperiment.getCell().getTitle();
            specie = newExperiment.getSpecie().getLatinName();
            antibody = newExperiment.getAntibody();
            treatment = newExperiment.getTreatment();
            controlId = newExperiment.getControlId();
        }
    }
    
    // it is copied
    private static Experiment getExperiment(Track track)
    {
        DataElementPath path = Module.optModulePath(track);
        if( path == null ) return null;
        Map<String, DataElementPath> map = experimentMap.get(path);
        if(map != null) return map.get(track.getName()).getDataElement(Experiment.class);
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
    
    private static biouml.plugins.gtrd.ChIPseqExperiment getNewExperiment(Track track)
    {
        DataElementPath path = Module.optModulePath(track);
        if( path == null ) return null;
        Map<String, DataElementPath> map = newExperimentMap.get(path);
        if(map != null) {
            DataElementPath expPath = map.get(track.getName());
            if(expPath == null)
                return null;
            return expPath.getDataElement(biouml.plugins.gtrd.ChIPseqExperiment.class);
        }
        DataCollection<biouml.plugins.gtrd.ChIPseqExperiment> dc;
        try
        {
            dc = path.getChildPath("Data", "experiments").getDataCollection(biouml.plugins.gtrd.ChIPseqExperiment.class);
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
                for( biouml.plugins.gtrd.ChIPseqExperiment exp : dc )
                    if( exp.getPeak() != null )
                        map.put(exp.getPeak().getName(), DataElementPath.create(exp));
                newExperimentMap.put(path, map);
            }
        }
        return map.get(track.getName()).getDataElement(biouml.plugins.gtrd.ChIPseqExperiment.class);
    }

    // it is copied
    public static String getControlId(Track track)
    {
        biouml.plugins.gtrd.ChIPseqExperiment newExperiment = getNewExperiment( track );
        if(newExperiment != null)
            return newExperiment.getControlId();
        Experiment exp = getExperiment(track);
        if( exp == null ) return null;
        return exp.getControlId();
    }
    
    // it is copied
    public String getTrackName()
    {
        return trackName;
    }

    // it is copied
    public String getTfClass()
    {
        return tfClass;
    }

    // it is copied
    public String getCellLine()
    {
        return cellLine;
    }

    // it is copied
    public String getSpecie()
    {
        return specie;
    }

    // it is copied
    public String getPathToSequenceCollection()
    {
        return pathToSequenceCollection;
    }
    
    // it is copied
    public int getNumberOfSites()
    {
        return numberOfSites;
    }

    // it is copied
    public String getTfName()
    {
        return tfName;
    }

    // it is copied
    public String getAntibody()
    {
        return antibody;
    }

    // it is copied
    public String getTreatment()
    {
        return treatment;
    }

    // it is copied
    public String getControlId()
    {
        return controlId;
    }

    public void changeCellLine(String cellLine)
    {
        this.cellLine = cellLine;
    }

    // it is copied
    public static List<TrackInfo> getTracksInfo(DataElementPath pathToTracks, Species givenSpecie, String givenTfClass, DataElementPath pathToSequences)
    {
        DataCollection<DataElement> tracks = pathToTracks.getDataCollection(DataElement.class);
        List<TrackInfo> result = new ArrayList<>();
        for( ru.biosoft.access.core.DataElement de: tracks )
        {
            if( ! (de instanceof Track) ) continue;
            Track track = (Track)de;
            if( track.getAllSites().getSize() <= 0 ) continue;
            TrackInfo trackInfo = new TrackInfo(track);
            String pathToSequenceCollection = trackInfo.getPathToSequenceCollection();
            if( pathToSequences != null && pathToSequenceCollection != null && ! pathToSequenceCollection.equals(pathToSequences.toString()) ) continue;
            if( ! trackInfo.getSpecie().equals(givenSpecie.getLatinName()) ) continue;
            if( givenTfClass != null && ! trackInfo.getTfClass().equals(givenTfClass) ) continue;
            result.add(trackInfo);
        }
        if( result.isEmpty() ) return null;
        return result;
    }
    
    public static List<TrackInfo> getTracksInfo(DataElementPath pathToTracks, Species givenSpecie, DataElementPath pathToSequences)
    {
        return getTracksInfo(pathToTracks, givenSpecie, null, pathToSequences);
    }
    
    public static List<TrackInfo> getTracksInfo(DataElementPath pathToTracks, Species givenSpecie)
    {
        return getTracksInfo(pathToTracks, givenSpecie, null, null);
    }

    public static List<TrackInfo> getTrackInfosWithGivenCellLine(List<TrackInfo> trackInfos, String cellLine)
    {
        return StreamEx.of(trackInfos).filter(ti -> ti.getCellLine().equals(cellLine)).toList();
    }
    
    public static List<TrackInfo> getTrackInfosWithGivenTfClass(List<TrackInfo> trackInfos, String tfClass)
    {
        return StreamEx.of(trackInfos).filter(ti -> ti.getTfClass().equals(tfClass)).toList();
    }

    public static List<TrackInfo> getTracksInfo(DataElementPath pathToTracks, String[] trackNames)
    {
        List<TrackInfo> result = new ArrayList<>();
        for( String trackName : trackNames )
            result.add(new TrackInfo(pathToTracks.getChildPath(trackName).getDataElement(Track.class)));
        return result;
    }

    public static List<String> getDistinctCellLines(List<TrackInfo> trackInfos)
    {
        return StreamEx.of(trackInfos).map(TrackInfo::getCellLine).distinct().toList();
    }

    public static List<String> getDistinctTfClasses(List<TrackInfo> trackInfos)
    {
        return StreamEx.of(trackInfos).map(TrackInfo::getTfClass).distinct().toList();
    }

    public static Map<String, String> getDistinctTfClassAndTfName(List<TrackInfo> trackInfos)
    {
        return StreamEx.of(trackInfos).toMap(TrackInfo::getTfClass, TrackInfo::getTfName, (a, b) -> a);
    }
    
    public static void changeSynonymCellLines(List<TrackInfo> trackInfos, DataElementPath pathToTableWithSynonyms, String nameOfColumnWithOldCellNames, String nameOfColumnWithNewCellNames)
    {
        if( pathToTableWithSynonyms == null ) return;
        Map<String, String> cellLineAndSynonymCellLine = new HashMap<>();
        String[][] cellLineAndItsSynonym = (String[][])TableUtils.readStringDataSubMatrix(pathToTableWithSynonyms, new String[]{nameOfColumnWithOldCellNames, nameOfColumnWithNewCellNames})[1];
        for( int i = 0; i < cellLineAndItsSynonym.length; i++ )
            cellLineAndSynonymCellLine.put(cellLineAndItsSynonym[i][0], cellLineAndItsSynonym[i][1]);
        if( cellLineAndSynonymCellLine != null )
            TrackInfo.changeSynonymCellLines(trackInfos, cellLineAndSynonymCellLine);
    }
    public static void changeSynonymCellLines(List<TrackInfo> trackInfos, Map<String, String> cellLineAndSynonymCellLine)
    {
        for( TrackInfo trackInfo : trackInfos )
        {
            String fromCellLine = trackInfo.getCellLine();
            trackInfo.changeCellLine(cellLineAndSynonymCellLine.getOrDefault(fromCellLine, fromCellLine));
        }
    }
    
    public static List<TrackInfo> removeSmallTracks(List<TrackInfo> trackInfos, int trackSizeThreshold)
    {
        List<TrackInfo> result = new ArrayList<>();
        for( TrackInfo ti : trackInfos )
            if( ti.getNumberOfSites() >= trackSizeThreshold )
                result.add(ti);
        return result;
    }
    
    public static DataElementPath getPathToSequences(DataElementPath pathToTrackWithBindingRegions)
    {
        Track track = pathToTrackWithBindingRegions.getDataElement(Track.class);
        DataElementPath pathToSequences = DataElementPath.create(new TrackInfo(track).getPathToSequenceCollection());
        return pathToSequences;
    }
    
    public static List<Track> getTracksWithSameTfClass(DataElementPath pathToChipSeqTrack)
    {
        Track track = pathToChipSeqTrack.getDataElement(Track.class);
        TrackInfo trackInfo = new TrackInfo(track);
        DataElementPath pathToTracks = pathToChipSeqTrack.getParentPath();
        List<TrackInfo> trackInfos = getTracksInfo(pathToTracks, Species.createInstance(trackInfo.getSpecie()), trackInfo.getTfClass(), null);
        String trackName = trackInfo.getTrackName();
        List<Track> result = StreamEx.of(trackInfos).map(TrackInfo::getTrackName).without(trackName)
            .map(name -> pathToTracks.getChildPath(name).getDataElement(Track.class)).toList();
        if( result.isEmpty() ) return null;
        return result;
    }
}