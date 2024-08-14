package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Entry;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.Repository;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TextUtil;

public class TranscriptionFactorTransformer extends AbstractTransformer<Entry, TranscriptionFactor>
{

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<TranscriptionFactor> getOutputType()
    {
        return TranscriptionFactor.class;
    }

    @Override
    public TranscriptionFactor transformInput(Entry input) throws Exception
    {
        Map<String, String> records = new HashMap<>();
        Reader reader = input.getReader();
        BufferedReader bReader = reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader( reader );
        String line;
        while( ( line = bReader.readLine() ) != null )
        {
            String[] fields = TextUtil.splitPos( line, 2 );
            String tag = fields[0];
            if( tag.equals( "//" ) )
                break;
            records.put( tag, fields[1] );
        }

        String name = records.get( "ID" );
        String displayName = records.get( "DN" );
        ReferenceType referenceType = ReferenceTypeRegistry.getReferenceType( records.get( "RT" ) );
        String species = records.get( "SP" );

        return new TranscriptionFactor( name, getTransformedCollection(), displayName, referenceType, species );
    }

    @Override
    public Entry transformOutput(TranscriptionFactor output) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "ID  " ).append( output.getName() ).append( '\n' );
        if( output.getDisplayName() != null && !output.getDisplayName().isEmpty() )
            sb.append( "DN  " ).append( output.getDisplayName() ).append( '\n' );
        sb.append( "RT  " ).append( output.getType().getDisplayName() ).append( '\n' );
        sb.append( "SP  " ).append( output.getSpeciesName() ).append( '\n' );
        sb.append( "//" ).append( '\n' );
        return new Entry( getPrimaryCollection(), output.getName(), sb.toString(), Entry.TEXT_FORMAT );
    }

    public static DataCollection<TranscriptionFactor> createCollection(DataElementPath path) throws Exception
    {
        Properties primary = new ExProperties();
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        primary.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, path.getName() + ".dat");
        primary.setProperty(FileEntryCollection2.ENTRY_DELIMITERS_PROPERTY, " \t");
        primary.setProperty(FileEntryCollection2.ENTRY_START_PROPERTY, "ID");
        primary.setProperty(FileEntryCollection2.ENTRY_ID_PROPERTY, "ID");
        primary.setProperty(FileEntryCollection2.ENTRY_END_PROPERTY, "//");

        Properties transformed = new ExProperties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, TransformedDataCollection.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.TRANSFORMER_CLASS, TranscriptionFactorTransformer.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, TranscriptionFactor.class.getName());

        Repository parentRepository = (Repository)DataCollectionUtils.getTypeSpecificCollection(path.optParentCollection(),
                TransformedDataCollection.class);
        DataCollection<?> result = CollectionFactoryUtils.createDerivedCollection(parentRepository, path.getName(), primary, transformed, null);
        CollectionFactoryUtils.save(result);
        return (DataCollection<TranscriptionFactor>)result;
    }
}
