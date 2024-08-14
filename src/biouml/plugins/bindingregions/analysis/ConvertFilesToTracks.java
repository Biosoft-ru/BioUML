package biouml.plugins.bindingregions.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

/***
 * @author yura
 * 
 * There exist 1) a folder with tab-delimited files with informations about tracks.
 *                at least, each file has to contains 3 columns with names 'chromosome', 'From', 'To'
 * 
 *             2) additional file '_properties' with tracks properties.
 *                it has to located in the same folder
 * 
 *************    example of file with properties  ******************
 *  common  DynamicProperty score
 *  common  DynamicProperty score2
 *  common  Property    source  ENCODE12
 *  common  Property    site name   DNase_1_HS
 *  file    fileName1   Property    cell    8988T
 *  file    fileName2   Property    cell    AG04449
 *  file    fileName3   Property    cell    AG04450
 *  file    fileName3   Property    treatment  IFNa4h
 *  file    fileName4   Property    cell    AG09309
 *  file    fileName5
 ***********************************************************************
 *
 *  All files with names declared in  file '_properties'
 *  (namely, fileName1, fileName2, fileName3, fileName4, fileName5)
 *  will be converted to tracks
 * 
 ***/

public class ConvertFilesToTracks extends AnalysisMethodSupport<ConvertFilesToTracks.ConvertFilesToTracksParameters>
{

    private static final String FILE = "file";
    private static final String COMMON = "common";
    private static final String DYNAMYC_PROPERTY = "DynamicProperty";
    private static final String PROPERTY = "Property";
    private static final String SITE_NAME = "siteName";
    private static final String CHROMOSOME = "chromosome";
    private static final String FROM = "From";
    private static final String TO = "To";

    public ConvertFilesToTracks(DataCollection<?> origin, String name)
    {
        super(origin, name, new ConvertFilesToTracksParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Convert files to tracks");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToFiles = parameters.getPathToFiles();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        // 1.
        log.info(" Read name of files and information about Dynamic Properties and Properties in file '_properties'");
        log.info(" Read dynamic properties names in file '_properties'");
        log.info(" Read file names and properties in file '_properties'");
        log.info(" Read file names and site names in file '_properties'");
        File propertiesFile = pathToFiles.getChildPath("_properties").getDataElement(FileDataElement.class).getFile();
        Set<String> fileNames = getFileNames(propertiesFile);
        if( fileNames == null )
            throw new IllegalArgumentException("Input  file '_properties' has to contain at least one name of file for convertion");
        List<String> dynamicPropertiesNames = getDynamicPropertiesNames(propertiesFile);
        Map<String, Map<String, String>> fileNamesAndProperties = getFileNamesAndProperties(propertiesFile, fileNames);
        Map<String, String> fileNamesAndSiteNames = getFileNamesAndSiteNames(propertiesFile, fileNames);
        if( fileNamesAndSiteNames == null )
            throw new IllegalArgumentException("Site names have to be determined for all converted files. Check input  file '_properties'");
        jobControl.setPreparedness(5);
        if( jobControl.isStopped() ) return null;

        // 2.
        int iJobControl = 0;
        log.info(" Convert files to tracks");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        for( String fileName : fileNames )
        {
            File file = pathToFiles.getChildPath(fileName).getDataElement(FileDataElement.class).getFile();
            convertFileToTrack(file, fileNamesAndSiteNames.get(fileName), dynamicPropertiesNames, fileNamesAndProperties.get(fileName), pathToSequences, pathToOutputs);
            jobControl.setPreparedness(5 + 95 * ++iJobControl / fileNames.size());
            if( jobControl.isStopped() ) return null;
        }
        return pathToOutputs.getDataCollection();
    }
    
    private Set<String> getFileNames(File propertiesFile) throws IOException
    {
        Set<String> result = new HashSet<>();
        try(BufferedReader reader = ApplicationUtils.asciiReader(propertiesFile))
        {
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line == null ) break;
                String[] fields = TextUtil.split(line, '\t');
                if( ! fields[0].equals(FILE) || fields.length < 2 ) continue;
                result.add(fields[1]);
            }
        }
        if( result.isEmpty() )
            result = null;
        return result;
    }
    
    private List<String> getDynamicPropertiesNames(File propertiesFile) throws IOException
    {
        List<String> result = new ArrayList<>();
        try(BufferedReader reader = ApplicationUtils.asciiReader( propertiesFile ))
        {
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line == null ) break;
                String[] fields = TextUtil.split(line, '\t');
                if( ! fields[0].equals(COMMON) || fields.length < 3 ) continue;
                if( fields[1].equals(DYNAMYC_PROPERTY) )
                    result.add(fields[2]);
            }
            //// new code
            reader.close();
        }
        if( result.isEmpty() )
            result = null;
        return result;
    }
    
    private Map<String, Map<String, String>> getFileNamesAndProperties(File propertiesFile, Set<String> fileNames) throws IOException
    {
        Map<String, Map<String, String>> result = new HashMap<>();
        for(String fileName : fileNames )
            result.put(fileName, new HashMap<String, String>());
        try(BufferedReader reader = ApplicationUtils.asciiReader(propertiesFile))
        {
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line == null ) break;
                String[] fields = TextUtil.split(line, '\t');
                if( fields.length < 4 ) continue;
                switch(fields[0])
                {
                    case COMMON : if( fields[1].equals(PROPERTY) )
                                     for( Map<String, String> map : result.values() )
                                         map.put(fields[2], fields[3]);
                                  break;
                    case FILE : if( fields.length >= 5 && fields[2].equals(PROPERTY) )
                                   result.get(fields[1]).put(fields[3], fields[4]);
                }
            }
        }
        for(Map<String, String> values : result.values() )
            if( values.isEmpty() )
                values = null;
        return result;
    }
    
    private Map<String, String> getFileNamesAndSiteNames(File propertiesFile, Set<String> fileNames) throws IOException
    {
        Map<String, String> result = new HashMap<>();
        try(BufferedReader reader = ApplicationUtils.asciiReader( propertiesFile ))
        {
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line == null ) break;
                String[] fields = TextUtil.split(line, '\t');
                if( fields.length < 4 ) continue;
                switch( fields[0] )
                {
                    case COMMON : if( fields[1].equals(PROPERTY) && fields[2].equals(SITE_NAME) )
                                     for( String name : fileNames )
                                         result.put(name, fields[3]); break;
                    case FILE : if( fields.length >= 5 && fields[2].equals(PROPERTY) && fields[3].equals(SITE_NAME) )
                                   result.put(fields[1], fields[4]);
                }
            }
        }
        for( String name : fileNames )
            if( ! result.containsKey(name) )
                return null;
        return result;
    }

    
    private Track convertFileToTrack(File file, String siteName, List<String> dynamicPropertiesNames, Map<String, String> propertiesNames, DataElementPath pathToSequences, DataElementPath pathToOutputs) throws Exception
    {
        String[] columnNames = getColumnNames(file);
        if( columnNames == null || columnNames.length < 3) return null;
        
        // 1. examination of existence of appropriate columns
        List<String> list = StreamEx.of(CHROMOSOME, FROM, TO).toList();
        if( dynamicPropertiesNames != null )
            list.addAll( dynamicPropertiesNames );
        if( StreamEx.of( list ).anyMatch( s -> !ArrayUtils.contains( columnNames, s ) ) )
            return null;
        
        // 2. Identification of indexes of appropriate columns
        Map<String, Integer> namesAndIndexes = EntryStream.of(columnNames).invert().toMap();
        
        // 3. Create map  chromosomeAndSequence
        Map<String, Sequence> chromosomeAndSequence = EnsemblUtils.sequences( pathToSequences )
                .toMap( Sequence::getName, Function.identity() );

        // 4. Read sites in file and Write them into track
        SqlTrack track = SqlTrack.createTrack(pathToOutputs.getChildPath(file.getName()), null, pathToSequences);
        try(BufferedReader reader = ApplicationUtils.asciiReader( file ))
        {
            int i = 0;
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line == null ) break;
                if( ++i == 1 ) continue;
                String[] fields = TextUtil.split(line, '\t');
                if( fields.length < list.size() )
                {
                    reader.close();
                    return null;
                }
                String chromosome = fields[namesAndIndexes.get(CHROMOSOME)];
                chromosome = chromosome.replaceAll("chr", "");
                chromosome = chromosome.replaceAll("Chr", "");
                chromosome = chromosome.replaceAll("x", "X");
                chromosome = chromosome.replaceAll("y", "Y");
                Sequence sequence = chromosomeAndSequence.get(chromosome);
                if( sequence == null ) continue;
                int start = Integer.parseInt(fields[namesAndIndexes.get(FROM)]);
                int length = Integer.parseInt(fields[namesAndIndexes.get(TO)]) - start + 1;
                if( length < 0 ) continue;
                Site site = new SiteImpl(track, siteName, siteName, Site.BASIS_PREDICTED, start, length, Site.PRECISION_NOT_KNOWN, Site.STRAND_BOTH, sequence, null);
                if( dynamicPropertiesNames != null && ! dynamicPropertiesNames.isEmpty() )
                {
                    DynamicPropertySet dps = site.getProperties();
                    for( String dp : dynamicPropertiesNames )
                    {
                        float value = Float.parseFloat(fields[namesAndIndexes.get(dp)]);
                        dps.add(new DynamicProperty(dp, Float.class, value));
                    }
                }
                track.addSite(site);
            }
        }
        track.finalizeAddition();
        track.getInfo().getProperties().setProperty(SqlTrack.SEQUENCES_COLLECTION_PROPERTY, pathToSequences.toString());
        if( propertiesNames != null )
            for( Entry<String, String> entry : propertiesNames.entrySet() )
                track.getInfo().getProperties().setProperty(entry.getKey(), entry.getValue());
        CollectionFactoryUtils.save(track);
        return track;
    }
    
    private String[] getColumnNames(File file) throws IOException
    {
        String[] result = null;
        try(BufferedReader reader = ApplicationUtils.asciiReader(file))
        {
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line != null )
                    result = TextUtil.split(line, '\t');
                break;
            }
        }
        return result;
    }
    
    // This method is template from Tagir
    private String[][] readTabSeparatedFile(File file) throws Exception
    {
        List<String[]> strings = new ArrayList<>();
        try(BufferedReader reader = ApplicationUtils.asciiReader(file))
        {
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line == null ) break;
                strings.add(TextUtil.split(line, '\t'));
            }
        }
        return strings.toArray(new String[strings.size()][]);
    }

    public static class ConvertFilesToTracksParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath pathToFiles;
        private DataElementPath outputPath;
        
        public ConvertFilesToTracksParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }
        
        @PropertyName(MessageBundle.PN_DB_SELECTOR)
        @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
        public BasicGenomeSelector getDbSelector()
        {
            return dbSelector;
        }
        public void setDbSelector(BasicGenomeSelector dbSelector)
        {
            Object oldValue = this.dbSelector;
            this.dbSelector = dbSelector;
            dbSelector.setParent(this);
            firePropertyChange("dbSelector", oldValue, dbSelector);
        }
        
        @PropertyName(MessageBundle.PN_FILES_FOLDER)
        @PropertyDescription(MessageBundle.PD_FILES_FOLDER)
        public DataElementPath getPathToFiles()
        {
            return pathToFiles;
        }
        public void setPathToFiles(DataElementPath pathToFiles)
        {
            Object oldValue = this.pathToFiles;
            this.pathToFiles = pathToFiles;
            firePropertyChange("pathToFiles", oldValue, pathToFiles);
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }
    }
    
    public static class ConvertFilesToTracksParametersBeanInfo extends BeanInfoEx2<ConvertFilesToTracksParameters>
    {
        public ConvertFilesToTracksParametersBeanInfo()
        {
            super(ConvertFilesToTracksParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInputChild("pathToFiles", beanClass, FolderCollection.class));
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
