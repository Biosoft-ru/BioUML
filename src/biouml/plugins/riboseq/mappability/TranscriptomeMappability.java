package biouml.plugins.riboseq.mappability;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import biouml.plugins.riboseq.transcripts.TranscriptsProvider;
import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TranscriptomeMappability extends AnalysisMethodSupport<TranscriptomeMappability.Parameters>
{

    public TranscriptomeMappability(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        NavigableMap<Integer, Double> readLengthPDF = getReadLengthPDF();
        NavigableMap<Integer, Double> lengthIndex = getFractionReadsGreaterOrEqual( readLengthPDF );

        TableDataCollection mappableLengthTable = TableDataCollectionUtils
                .createTableDataCollection( parameters.getTranscriptMappableLength() );
        mappableLengthTable.getColumnModel().addColumn( "MappableLength", DataType.Float );
        
        TableDataCollection cdsMappableLengthTable = null;
        if(parameters.getCdsMappableLength() != null)
        {
            cdsMappableLengthTable = TableDataCollectionUtils.createTableDataCollection( parameters.getCdsMappableLength() );
            cdsMappableLengthTable.getColumnModel().addColumn( "MappableLength", DataType.Float );
        }
        
        Set<String> subset = null;
        if( parameters.getTranscriptSubset() != null )
            subset = new HashSet<>( parameters.getTranscriptSubset().getDataCollection().getNameList() );

        Map<String, Transcript> transcripts = null;
        if( parameters.getCdsMappableLength() != null )
        {
            transcripts = new HashMap<>();
            TranscriptSet transcriptSet = parameters.getTranscriptSet();
            TranscriptsProvider provider = transcriptSet.createTranscriptsProvider();
            provider.setSubset( subset );
            provider.setOnlyProteinCoding( true );
            provider.setLoadCDS( true );
            for( Transcript t : provider.getTranscripts() )
                transcripts.put( t.getName(), t );
        }

        File inputWigFile = parameters.getMinimalUniqueLength().getDataElement( FileDataElement.class ).getFile();

        try (BufferedReader reader = new BufferedReader( new FileReader( inputWigFile ) );
                TempFile outputWigFile = TempFiles.file( ".wig" );
                BufferedWriter writer = new BufferedWriter( new FileWriter( outputWigFile ) ))
        {
            String line = reader.readLine();
            while( line != null )
            {
                String[] fields = line.split( " " );
                if( fields.length != 4 || !fields[0].equals( "fixedStep" ) || !fields[1].startsWith( "chrom=" )
                        || !fields[2].equals( "start=1" ) || !fields[3].equals( "step=1" ) )
                    throw new Exception( "Unexpected line '" + line + "' in " + parameters.getMinimalUniqueLength() );
                String transcriptName = fields[1].substring( "chrom=".length() );

                boolean skip = subset != null && !subset.contains( transcriptName );

                Interval cds = null;
                if( parameters.getCdsMappableLength() != null )
                {
                    Transcript transcript = transcripts.get( transcriptName );
                    if( transcript == null )
                        skip = true;
                    else
                        cds = transcript.getCDSLocations().get( 0 );
                }

                if( skip )
                {
                    while( ( line = reader.readLine() ) != null && !line.startsWith( "fixedStep" ) )
                        ;
                }
                else
                {
                    writer.append( line ).append( '\n' );
                    double mappableLength = 0;
                    double cdsMappableLength = 0;

                    int pos = 0;
                    while( ( line = reader.readLine() ) != null && !line.startsWith( "fixedStep" ) )
                    {
                        int mul = Integer.parseInt( line );
                        double mappability = 0;
                        if( mul > 0 )
                            mappability = fractionReadsGreaterOrEqual( lengthIndex, mul );
                        writer.append( String.valueOf( mappability ) ).append( '\n' );
                        
                        mappableLength += mappability;
                        
                        if(cds != null)
                            cdsMappableLength += computeCDSMappableLength( readLengthPDF, mul, cds, pos );
                        
                        pos++;
                    }
                    TableDataCollectionUtils.addRow( mappableLengthTable, transcriptName, new Object[] {mappableLength}, true );
                    if(cds != null)
                        TableDataCollectionUtils.addRow( cdsMappableLengthTable, transcriptName, new Object[] {cdsMappableLength}, true );
                        
                }

            }
            writer.close();

            FileImporter importer = new FileImporter();
            DataCollection<DataElement> parent = parameters.getMappabilityProfile().getParentCollection();
            String name = parameters.getMappabilityProfile().getName();
            importer.getProperties( parent, outputWigFile, name ).setPreserveExtension( false );
            importer.doImport( parent, outputWigFile, name, null, log );
        }

        mappableLengthTable.finalizeAddition();
        parameters.getTranscriptMappableLength().save( mappableLengthTable );
        
        if(cdsMappableLengthTable != null)
        {
            cdsMappableLengthTable.finalizeAddition();
            parameters.getCdsMappableLength().save( cdsMappableLengthTable );
        }

        return new Object[] {parameters.getMappabilityProfile().getDataElement(), mappableLengthTable};
    }

    private double computeCDSMappableLength(NavigableMap<Integer, Double> readLengthPDF, int mappableLen, Interval cds, int pos)
    {
        return EntryStream.of( readLengthPDF.tailMap( mappableLen ) )
          .filterKeys( readLen->getBounds5Prime( cds, readLen ).inside( pos ) )
          .values().mapToDouble( Double::doubleValue ).sum();
    }
    
    private Interval getBounds5Prime(Interval cds, int readLen)
    {
        int from = cds.getFrom() - readLen + parameters.getMinCDSOverlap();
        if( from < 0 )
            from = 0;
        int to = cds.getTo() - parameters.getMinCDSOverlap() + 1;
        return new Interval(from, to);
    }

    private double fractionReadsGreaterOrEqual(NavigableMap<Integer, Double> index, int length)
    {
        Entry<Integer, Double> e = index.ceilingEntry( length );
        if( e == null )
            return 0;
        return e.getValue();
    }
    
    private NavigableMap<Integer, Double> getReadLengthPDF() throws Exception
    {
        TreeMap<Integer, Double> lengthToCount = new TreeMap<>();
        TableDataCollection table = parameters.getReadLengthDistribution().getDataElement( TableDataCollection.class );
        for( String id : table.getNameList() )
        {
            Number count = (Number)table.get( id ).getValue( "Count" );
            double dCount = count.doubleValue();
            lengthToCount.put( Integer.parseInt( id ), dCount );
        }
        double sum = lengthToCount.values().stream().mapToDouble( Double::doubleValue ).sum();
        return EntryStream.of( lengthToCount ).mapValues( count->count / sum ).toCustomMap( TreeMap::new );
    }
    
    private NavigableMap<Integer, Double> getFractionReadsGreaterOrEqual(NavigableMap<Integer, Double> pdf)
    {
        NavigableMap<Integer, Double> result = new TreeMap<>();
        double cumsum = 0;
        for( Integer len : pdf.descendingKeySet() )
        {
            cumsum += pdf.get( len );
            result.put( len, cumsum );
        }
        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        {            
            setTranscriptSet( new TranscriptSet() );
        }

        private DataElementPath minimalUniqueLength;
        @PropertyName ( "Minimal unique length" )
        @PropertyDescription ( "Minimal unique length file, produced by 'Transcriptome minimal unique length' analysis" )
        public DataElementPath getMinimalUniqueLength()
        {
            return minimalUniqueLength;
        }
        public void setMinimalUniqueLength(DataElementPath minimalUniqueLength)
        {
            Object oldValue = this.minimalUniqueLength;
            this.minimalUniqueLength = minimalUniqueLength;
            firePropertyChange( "minimalUniqueLength", oldValue, minimalUniqueLength );
        }

        private DataElementPath readLengthDistribution;
        @PropertyName ( "Read length distribution" )
        @PropertyDescription ( "Table with ids corresponding to read length and 'Count' column" )
        public DataElementPath getReadLengthDistribution()
        {
            return readLengthDistribution;
        }
        public void setReadLengthDistribution(DataElementPath readLengthDistribution)
        {
            Object oldValue = this.readLengthDistribution;
            this.readLengthDistribution = readLengthDistribution;
            firePropertyChange( "readLengthDistribution", oldValue, readLengthDistribution );
        }

        private DataElementPath transcriptSubset;
        @PropertyName ( "Transcript subset" )
        @PropertyDescription ( "Use only transcripts present in this table" )
        public DataElementPath getTranscriptSubset()
        {
            return transcriptSubset;
        }
        public void setTranscriptSubset(DataElementPath transcriptSubset)
        {
            Object oldValue = this.transcriptSubset;
            this.transcriptSubset = transcriptSubset;
            firePropertyChange( "transcriptSubset", oldValue, transcriptSubset );
        }

        private DataElementPath mappabilityProfile;
        @PropertyName ( "Mappability profile" )
        @PropertyDescription ( "Resulting mappability profile" )
        public DataElementPath getMappabilityProfile()
        {
            return mappabilityProfile;
        }
        public void setMappabilityProfile(DataElementPath mappabilityProfile)
        {
            Object oldValue = this.mappabilityProfile;
            this.mappabilityProfile = mappabilityProfile;
            firePropertyChange( "mappabilityProfile", oldValue, mappabilityProfile );
        }

        private DataElementPath transcriptMappableLength;
        @PropertyName ( "Transcript mappable length" )
        @PropertyDescription ( "Number of mappable positions for each transcript" )
        public DataElementPath getTranscriptMappableLength()
        {
            return transcriptMappableLength;
        }
        public void setTranscriptMappableLength(DataElementPath transcriptMappableLength)
        {
            Object oldValue = this.transcriptMappableLength;
            this.transcriptMappableLength = transcriptMappableLength;
            firePropertyChange( "transcriptMappableLength", oldValue, transcriptMappableLength );
        }
        
        private DataElementPath cdsMappableLength;
        @PropertyName("CDS mappable length")
        @PropertyDescription("Number of mappable position in coding sequence")
        public DataElementPath getCdsMappableLength()
        {
            return cdsMappableLength;
        }
        public void setCdsMappableLength(DataElementPath cdsMappableLength)
        {
            Object oldValue = this.cdsMappableLength;
            this.cdsMappableLength = cdsMappableLength;
            firePropertyChange( "*", oldValue, cdsMappableLength );
        }
        
        private int minCDSOverlap = 20;
        @PropertyName("Min CDS overlap")
        public int getMinCDSOverlap()
        {
            return minCDSOverlap;
        }
        public void setMinCDSOverlap(int minCDSOverlap)
        {
            int oldValue = this.minCDSOverlap;
            this.minCDSOverlap = minCDSOverlap;
            firePropertyChange( "minCDSOverlap", oldValue, minCDSOverlap );
        }

        private TranscriptSet transcriptSet;
        @PropertyName("Transcript set")
        public TranscriptSet getTranscriptSet()
        {
            return transcriptSet;
        }
        public void setTranscriptSet(TranscriptSet transcriptSet)
        {
            TranscriptSet oldValue = this.transcriptSet;
            this.transcriptSet = withPropagation( oldValue, transcriptSet );
            transcriptSet.setNeedsSequencesCollection( false );
            transcriptSet.disableTranscriptSubset();
            firePropertyChange( "transcriptSet", oldValue, transcriptSet );
        }
        public boolean isTranscriptSetHidden()
        {
            return getCdsMappableLength() == null;
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "minimalUniqueLength" ).inputElement( FileDataElement.class ).add();
            property( "readLengthDistribution" ).inputElement( TableDataCollection.class ).add();
            property( "transcriptSubset" ).inputElement( TableDataCollection.class ).canBeNull().add();
            property( "mappabilityProfile" ).outputElement( FileDataElement.class ).add();
            property( "transcriptMappableLength" ).outputElement( TableDataCollection.class ).add();
            property( "cdsMappableLength" ).outputElement( TableDataCollection.class ).canBeNull().add();
            property( "minCDSOverlap" ).hidden( "isTranscriptSetHidden" ).add();
            property( "transcriptSet" ).hidden( "isTranscriptSetHidden" ).add();
        }
    }
}
