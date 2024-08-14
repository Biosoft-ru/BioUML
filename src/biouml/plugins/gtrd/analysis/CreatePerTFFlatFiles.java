package biouml.plugins.gtrd.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.analysis.OpenPerTFView.SpeciesSelector;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CreatePerTFFlatFiles extends AnalysisMethodSupport<CreatePerTFFlatFiles.Parameters>
{
    public CreatePerTFFlatFiles(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath folder = DataElementPath.create( "databases/GTRD/Data/clusters/" + parameters.getOrganism().getLatinName() + "/By TF" );
        
        File outFolder = TempFiles.dir( parameters.getOrganism().getLatinName() );
        log.info( "Output will be written to " + outFolder );
        
        jobControl.forCollection( folder.getChildren(), new TFIteration(outFolder) );
        
        return super.justAnalyzeAndPut();
    }
    
    private static class TFIteration implements Iteration<ru.biosoft.access.core.DataElementPath>
    {
        private File outFolder;
        public TFIteration(File outFolder)
        {
            this.outFolder = outFolder;
        }

        @Override
        public boolean run(DataElementPath tfFolder)
        {
            try
            {
                File outTfFolder = new File( outFolder, tfFolder.getName() );
                outTfFolder.mkdirs();
                for( String peakCaller : new String[] {"MACS2", "GEM", "PICS", "SISSRS"} )
                {
                    DataElementPath peaks = tfFolder.getChildPath( peakCaller + " peaks" );
                    if( peaks.exists() )
                    {
                        File peaksFile = new File( outTfFolder, peakCaller.toLowerCase() + "_peaks.bed.gz" );
                        writeBedFile( peaks.getDataElement( Track.class ), peaksFile );
                    }
                    
                    DataElementPath clusters = tfFolder.getChildPath( peakCaller + " clusters" );
                    if(clusters.exists())
                    {
                        File clustersFile = new File( outTfFolder, peakCaller.toLowerCase() + "_clusters.bed.gz" );
                        writeBedFile( clusters.getDataElement( Track.class ), clustersFile );
                    }
                }
                DataElementPath metaClusters = tfFolder.getChildPath( "meta clusters" );
                if(metaClusters.exists())
                {
                    File metaClustersFile = new File( outTfFolder, "meta_clusters.bed.gz" );
                    writeBedFile( metaClusters.getDataElement( Track.class ), metaClustersFile );
                }
            }
            catch( IOException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return true;
        }

        private void writeBedFile(Track track, File file) throws IOException
        {
            
            try(BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) ) ))
            {
                for(Site s : track.getAllSites())
                {
                    Sequence chr = s.getOriginalSequence();
                    writer
                        .append( chr.getName() )
                        .append( '\t' )
                        .append( String.valueOf( s.getFrom() - chr.getStart() ) )
                        .append( '\t' )
                        .append( String.valueOf( s.getTo() - chr.getStart() ) )
                        .append( '\n' );
                }
            }
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private Species organism = Species.getSpecies( "Homo sapiens" );

        @PropertyName("Organism")
        public Species getOrganism()
        {
            return organism;
        }
        public void setOrganism(Species organism)
        {
            Object oldValue = this.organism;
            this.organism = organism;
            firePropertyChange( "organism", oldValue, organism );
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
            property("organism").editor( SpeciesSelector.class ).hideChildren().add();
        }
    }
}
