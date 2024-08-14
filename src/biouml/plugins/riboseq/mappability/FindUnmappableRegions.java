package biouml.plugins.riboseq.mappability;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FindUnmappableRegions extends AnalysisMethodSupport<FindUnmappableRegions.Parameters>
{
    public FindUnmappableRegions(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkGreater( "minReadLen", 0 );
        checkGreater( "maxReadLen", 0 );
        if( parameters.getMinReadLen() > parameters.getMaxReadLen() )
            throw new IllegalArgumentException( "maxReadLen should be greater or equals to minReadLen" );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        File inputWigFile = parameters.getMinimalUniqueLength().getDataElement( FileDataElement.class ).getFile();
        DataElementPath genomePath = parameters.getGenome();
        SequenceCollection genome = genomePath.getDataElement( SequenceCollection.class );

        SqlTrack[] unmappableTracks = new SqlTrack[parameters.getMaxReadLen() - parameters.getMinReadLen() + 1];
        for( int readLen = parameters.getMinReadLen(); readLen <= parameters.getMaxReadLen(); readLen++ )
        {
            DataElementPath trackPath = parameters.getOutputFolder().getChildPath( String.valueOf( readLen ) );
            DataCollectionUtils.createFoldersForPath( trackPath );
            SqlTrack track = SqlTrack.createTrack( trackPath, null, genomePath );
            unmappableTracks[readLen - parameters.getMinReadLen()] = track;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader( new GZIPInputStream( new FileInputStream( inputWigFile ) ) ) ))
        {
            String line = reader.readLine();
            while( line != null )
            {
                String[] fields = line.split( " " );
                if( fields.length != 4 || !fields[0].equals( "fixedStep" ) || !fields[1].startsWith( "chrom=" )
                        || !fields[2].equals( "start=1" ) || !fields[3].equals( "step=1" ) )
                    throw new Exception( "Unexpected line '" + line + "' in " + parameters.getMinimalUniqueLength() );
                String chrName = fields[1].substring( "chrom=".length() );
                boolean forward = chrName.endsWith( "+" );
                chrName = chrName.substring( 0, chrName.length() - 1 );
                AnnotatedSequence chr = genome.get( chrName );
                int chrLen = chr.getSequence().getLength();

                SiteAccumulator[] accums = new SiteAccumulator[parameters.getMaxReadLen() - parameters.getMinReadLen() + 1];
                for( int i = 0; i < accums.length; i++ )
                    accums[i] = new SiteAccumulator( unmappableTracks[i], chr, forward );

                int pos = 0;
                while( ( line = reader.readLine() ) != null && !line.startsWith( "fixedStep" ) )
                {
                    int mul = Integer.parseInt( line );
                    for( int readLen = parameters.getMinReadLen(); readLen <= parameters.getMaxReadLen(); readLen++ )
                    {
                        boolean mappable = mul != -1 && readLen >= mul && pos + readLen <= chrLen;
                        accums[readLen - parameters.getMinReadLen()].addPos( !mappable );
                    }
                    pos++;
                }
                for( SiteAccumulator acc : accums )
                    acc.finish();
            }
        }

        for( SqlTrack t : unmappableTracks )
        {
            t.finalizeAddition();
            t.getCompletePath().save( t );
        }
        return parameters.getOutputFolder().getDataElement( ru.biosoft.access.core.DataCollection.class );
    }

    private static class SiteAccumulator
    {
        private int siteFrom = -1;
        private int pos = 0;
        private SqlTrack track;
        private AnnotatedSequence chr;
        private boolean forward;
        private int chrLen;

        public SiteAccumulator(SqlTrack track, AnnotatedSequence chr, boolean forward)
        {
            this.track = track;
            this.chr = chr;
            this.forward = forward;
            chrLen = chr.getSequence().getLength();
        }
        public void addPos(boolean isSite)
        {
            if( isSite && siteFrom == -1 )
            {
                siteFrom = pos;
            }
            else if( !isSite && siteFrom != -1 )
            {
                int siteTo = pos - 1;
                Site site = new SiteImpl( null, chr.getName(),
                        chr.getSequence().getStart() + ( forward ? siteFrom : chrLen - siteFrom - 1 ), siteTo - siteFrom + 1,
                        forward ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS, null );
                track.addSite( site );
                siteFrom = -1;
            }
            pos++;
        }

        public void finish()
        {
            addPos( false );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath genome;
        @PropertyName ( "Genome" )
        @PropertyDescription ( "Collection of genomic sequences" )
        public DataElementPath getGenome()
        {
            return genome;
        }
        public void setGenome(DataElementPath genome)
        {
            Object oldValue = this.genome;
            this.genome = genome;
            firePropertyChange( "genome", oldValue, genome );
        }

        private DataElementPath minimalUniqueLength;
        @PropertyName ( "Minimal unique length" )
        @PropertyDescription ( "Minimal unique length file, produced by 'Sequence minimal unique length' analysis" )
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

        private int minReadLen = 30;
        @PropertyName ( "Min read length" )
        public int getMinReadLen()
        {
            return minReadLen;
        }
        public void setMinReadLen(int minReadLen)
        {
            int oldValue = this.minReadLen;
            this.minReadLen = minReadLen;
            firePropertyChange( "minReadLen", oldValue, minReadLen );
        }

        private int maxReadLen = 30;
        @PropertyName ( "Max read length" )
        public int getMaxReadLen()
        {
            return maxReadLen;
        }
        public void setMaxReadLen(int maxReadLen)
        {
            int oldValue = this.maxReadLen;
            this.maxReadLen = maxReadLen;
            firePropertyChange( "maxReadLen", oldValue, maxReadLen );
        }


        private DataElementPath outputFolder;
        @PropertyName ( "Output folder" )
        public DataElementPath getOutputFolder()
        {
            return outputFolder;
        }
        public void setOutputFolder(DataElementPath outputFolder)
        {
            Object oldValue = this.outputFolder;
            this.outputFolder = outputFolder;
            firePropertyChange( "outputFolder", oldValue, outputFolder );
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
            property( "genome" ).inputElement( SequenceCollection.class ).add();
            property( "minimalUniqueLength" ).inputElement( FileDataElement.class ).add();
            add( "minReadLen" );
            add( "maxReadLen" );
            property( "outputFolder" ).outputElement( FolderCollection.class ).add();
        }
    }
}
