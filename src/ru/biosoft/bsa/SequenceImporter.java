package ru.biosoft.bsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.EntryStream;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.EntryCollection;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.JDBM2Index;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.bsa.transformer.EmblTransformer;
import ru.biosoft.bsa.transformer.FastaSequenceCollection;
import ru.biosoft.bsa.transformer.FastaTransformer;
import ru.biosoft.bsa.transformer.FastqTransformer;
import ru.biosoft.bsa.transformer.GenbankTransformer;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SequenceImporter implements DataElementImporter
{
    public static final String GB_FORMAT = "gb";
    public static final String FASTQ_FORMAT = "fastq";
    public static final String FASTA_FORMAT = "fasta";
    public static final String EMBL_FORMAT = "embl";
    
    private ImporterProperties importerProperties;
    
    private String format;

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, getResultType() )
                || ! ( DataCollectionUtils.getTypeSpecificCollection( parent, getResultType() ) instanceof Repository ) )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;

        String fileName = file.getName();
        int extIdx = fileName.lastIndexOf('.');
        if(extIdx >= 0)
        {
            String ext = fileName.substring(extIdx + 1);
            if(format.equalsIgnoreCase(ext))
                return ACCEPT_HIGH_PRIORITY;
            if(FASTA_FORMAT.equals(format) && ( ext.equals( "fa" ) || ext.equals("fna") ))
                return ACCEPT_HIGH_PRIORITY;
            if("seq".equalsIgnoreCase(ext))
                return ACCEPT_MEDIUM_PRIORITY;
        }
        if( isContentAcceptable(file) )
            return ACCEPT_LOWEST_PRIORITY;
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        if( elementName == null )
            elementName = file.getName().replaceFirst(".+\\.\\w{1,5}$", "");
        parent.getCompletePath().getDataCollection().remove(elementName);

        DataElement result = createElement( parent, elementName, file, format, importerProperties );
        
        CollectionFactoryUtils.save(result);
        //result element become invalid in GenericDataCollection cache after put since file is changed
        //TODO: remove from cache in proper place, when file is copied
        DataCollection<?> primaryParent = (DataCollection<?>)SecurityManager.runPrivileged( () -> {
            return DataCollectionUtils.fetchPrimaryCollectionPrivileged( (DataCollection<?>)parent );
        } );
        if( primaryParent instanceof GenericDataCollection )
        {
            ( (GenericDataCollection)primaryParent ).removeFromCache( result.getName() );
        }

        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return parent.get( elementName );
    }
    
    public static DataElement createElement(DataCollection<?> origin, String name, File input, String format, ImporterProperties importerProperties) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, input.getParent() );
        properties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, input.getName() );
                
        String sequenceTransformerClass = FORMAT_TO_TRANSFORMER.get( format );
        properties.putAll( SequenceImporter.getFormatReaderProperties( format ) );
        String configDir = origin.getInfo().getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
        properties.setProperty( FileEntryCollection2.INDEX_DIR, configDir );
        properties.setProperty( FileEntryCollection2.ORIGINAL_ORDER_PROPERTY, "true" );
        properties.setProperty( FileEntryCollection2.INDEX_TYPE_PROPERTY, JDBM2Index.class.getName() );
        if(format.equals( FASTA_FORMAT ))
        {
        	properties.setProperty( FastaSequenceCollection.DO_GET_SEQUENCEID_ONLY, String.valueOf(importerProperties.getSequenceIdOnly) );
        	if(importerProperties.getSequenceIdOnly)
        		properties.put(EntryCollection.ENTRY_KEY_FULL, "false");
        }
        FileEntryCollection2 fileEntryCollection = new FileEntryCollection2( origin, properties  );
        
        Properties properties2 = new Properties();
        properties2.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties2.setProperty( DataCollectionConfigConstants.TRANSFORMER_CLASS, sequenceTransformerClass );
        properties2.put( DataCollectionConfigConstants.PRIMARY_COLLECTION, fileEntryCollection );
        properties2.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configDir );
        
        if(format.equals( FASTA_FORMAT )) {
        	properties2.setProperty( FastaSequenceCollection.DO_GET_SEQUENCEID_ONLY, String.valueOf(importerProperties.getSequenceIdOnly) );
            return new FastaSequenceCollection(origin, properties2);
        }
        else
            return new GenbankSequenceCollection(origin, properties2 );
    }

    public static DataElement createElement(DataCollection<?> origin, String name, File input, String format) throws Exception
    {
    	return createElement( origin, name, input, format, new ImporterProperties() );
    }
    
    @Override
    public Object getProperties(DataCollection parent, File file, String elementName)
    {
    	this.importerProperties = new ImporterProperties();
        return this.importerProperties;
    }

    @Override
    public boolean init(Properties properties)
    {
        format = properties.getProperty(SUFFIX);
        return EMBL_FORMAT.equalsIgnoreCase( format ) || FASTA_FORMAT.equalsIgnoreCase( format ) || FASTQ_FORMAT.equalsIgnoreCase( format )
                || GB_FORMAT.equalsIgnoreCase( format );
    }

    /**
     * Performs a quick and dirty test on file content.
     * @param file
     * @return true if file content is acceptable for given format
     */
    private boolean isContentAcceptable(File file)
    {
        try (FileInputStream is = new FileInputStream( file ); BufferedReader input = new BufferedReader( new InputStreamReader( is ) ))
        {
            java.util.Map<String, String> properties = getFormatReaderProperties(format);
            if(properties.size() == 0)
                return false;


            int totalLines = 0;
            String startKey = properties.get(EntryCollection.ENTRY_ID_PROPERTY);
            String line;
            while( ( line = input.readLine() ) != null && totalLines < 100 )
            {
                totalLines++;
                if( line.startsWith("#") ) //skip comment lines
                    continue;
                return line.startsWith(startKey);
            }
        }
        catch( Exception e )
        {
        }
        return false;
    }

    public static java.util.Map<String, String> getFormatReaderProperties(String format)
    {
        java.util.Map<String, String> properties = new HashMap<>();
        properties.put(EntryCollection.ENTRY_KEY_ESCAPE_SPECIAL_CHARS, "true");
        if( EMBL_FORMAT.equals( format ) )
        {
            properties.put(EntryCollection.ENTRY_START_PROPERTY, "ID");
            properties.put(EntryCollection.ENTRY_ID_PROPERTY, "ID");
            properties.put(EntryCollection.ENTRY_END_PROPERTY, "//");
            properties.put(EntryCollection.ENTRY_DELIMITERS_PROPERTY, " ");
        }
        else if( FASTA_FORMAT.equals( format ) )
        {
            properties.put(EntryCollection.ENTRY_START_PROPERTY, ">");
            properties.put(EntryCollection.ENTRY_ID_PROPERTY, ">");
            properties.put(EntryCollection.ENTRY_END_PROPERTY, "");
            properties.put(EntryCollection.ENTRY_DELIMITERS_PROPERTY, ";");
            properties.put(EntryCollection.ENTRY_KEY_FULL, "true");
        }
        else if( FASTQ_FORMAT.equals( format ) )
        {
            properties.put(EntryCollection.ENTRY_START_PROPERTY, "@");
            properties.put(EntryCollection.ENTRY_ID_PROPERTY, "@");
            properties.put(EntryCollection.ENTRY_END_PROPERTY, "");
            properties.put(EntryCollection.ENTRY_KEY_FULL, "true");
            properties.put(EntryCollection.ENTRY_DELIMITERS_PROPERTY, " ");
        }
        else if( GB_FORMAT.equals( format ) )
        {
            properties.put(EntryCollection.ENTRY_START_PROPERTY, "LOCUS");
            properties.put(EntryCollection.ENTRY_ID_PROPERTY, "LOCUS");
            properties.put(EntryCollection.ENTRY_END_PROPERTY, "//");
            properties.put(EntryCollection.ENTRY_DELIMITERS_PROPERTY, " ");
        }
        return properties;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return TrackOnSequences.class;
    }

    private static final Map<String, String> TRANSFORMER_TO_FORMAT = EntryStream.of(
            EmblTransformer.class.getName(), EMBL_FORMAT,
            FastaTransformer.class.getName(), FASTA_FORMAT,
            FastqTransformer.class.getName(), FASTQ_FORMAT,
            GenbankTransformer.class.getName(), GB_FORMAT )
        .toMap();

    private static final Map<String, String> FORMAT_TO_TRANSFORMER = EntryStream.of(
            EMBL_FORMAT, EmblTransformer.class.getName(),
            FASTA_FORMAT, FastaTransformer.class.getName(),
            FASTQ_FORMAT, FastqTransformer.class.getName(),
            GB_FORMAT, GenbankTransformer.class.getName() )
        .toMap();

    public static String getFormatForTransformer(String transformerClass, String defaultValue)
    {
        return TRANSFORMER_TO_FORMAT.getOrDefault( transformerClass, defaultValue );
    }
    
    public static class ImporterProperties
    {
        private boolean getSequenceIdOnly = true;

        @PropertyName ( "Get sequenceIds only" )
        @PropertyDescription ( "Ignore additional sequence info after the first space" )
        public boolean isGetSequenceIdOnly()
        {
            return getSequenceIdOnly;
        }

        public void setGetSequenceIdOnly(boolean getSequenceIdOnly)
        {
            this.getSequenceIdOnly = getSequenceIdOnly;
        }
    }

    public static class ImporterPropertiesBeanInfo extends BeanInfoEx2<ImporterProperties>
    {
        public ImporterPropertiesBeanInfo()
        {
        	super(ImporterProperties.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "getSequenceIdOnly" );
        }
    }
}