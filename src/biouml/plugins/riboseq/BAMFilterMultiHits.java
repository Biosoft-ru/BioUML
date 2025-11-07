package biouml.plugins.riboseq;

import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.importer.SAMBAMTrackImporter;
import ru.biosoft.bsa.importer.SAMBAMTrackImporter.ImporterProperties;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class BAMFilterMultiHits extends AnalysisMethodSupport<BAMFilterMultiHits.Parameters>
{
    public BAMFilterMultiHits(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        BAMTrack inTrack = parameters.getInputBAMTrack().getDataElement( BAMTrack.class );
        Map<String, Boolean> duplicated = new HashMap<>();
        Map<String, Boolean> secondOfPairDuplicated = new HashMap<>();
        try (SamReader bamReader = SamReaderFactory.makeDefault().open( inTrack.getBAMFile() ))
        {
            for(SAMRecord r : bamReader)
            {
                String name = r.getReadName();
                Map<String, Boolean> map = r.getReadPairedFlag() && ! r.getFirstOfPairFlag() ? secondOfPairDuplicated : duplicated; 
                map.put( name, map.containsKey( name ) );
            }
        }
        
        try (TempFile outFile = TempFiles.file( ".bam" ); SamReader bamReader = SamReaderFactory.makeDefault().open( inTrack.getBAMFile() );)
        {
            SAMFileWriterFactory factory = new SAMFileWriterFactory();
            SAMFileHeader header = bamReader.getFileHeader();
            boolean sorted = header.getSortOrder() == SortOrder.coordinate;
            SAMFileWriter bamWriter = factory.makeBAMWriter( header, sorted, outFile );
            try
            {
                for( SAMRecord r : bamReader )
                {
                    Map<String, Boolean> map = r.getReadPairedFlag() && ! r.getFirstOfPairFlag() ? secondOfPairDuplicated : duplicated;
                    if( !map.get( r.getReadName() ) )
                        bamWriter.addAlignment( (SAMRecord)r.clone() );
                }
            }
            finally
            {
                bamWriter.close();
            }
            DataElementPath outPath = parameters.getOutputBAMTrack();
            SAMBAMTrackImporter importer = new SAMBAMTrackImporter();
            ImporterProperties properties = importer.getProperties( outPath.getParentCollection(), outFile, outPath.getName() );
            properties.setCreateIndex( sorted );
            return importer.doImport( outPath.getParentCollection(), outFile, outPath.getName(), null, log );
        }
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputBAMTrack;
        @PropertyName ( "Input BAM track" )
        @PropertyDescription ( "BAM track to filter" )
        public DataElementPath getInputBAMTrack()
        {
            return inputBAMTrack;
        }
        public void setInputBAMTrack(DataElementPath inputBAMTrack)
        {
            Object oldValue = this.inputBAMTrack;
            this.inputBAMTrack = inputBAMTrack;
            firePropertyChange( "inputBAMTrack", oldValue, inputBAMTrack );
        }

        private DataElementPath outputBAMTrack;
        @PropertyName ( "Output BAM track" )
        public DataElementPath getOutputBAMTrack()
        {
            return outputBAMTrack;
        }
        public void setOutputBAMTrack(DataElementPath outputBAMTrack)
        {
            Object oldValue = this.outputBAMTrack;
            this.outputBAMTrack = outputBAMTrack;
            firePropertyChange( "outputBAMTrack", oldValue, outputBAMTrack );
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
            property( "inputBAMTrack" ).inputElement( BAMTrack.class ).add();
            property( "outputBAMTrack" ).outputElement( BAMTrack.class ).add();
        }
    }
}
