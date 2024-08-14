package biouml.plugins.ensembl.homology;

import java.io.FileInputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.zip.GZIPInputStream;

import biouml.plugins.download.FileDownloader;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

public class ImportProteinTreeAnalysis extends AnalysisMethodSupport<ImportProteinTreeAnalysis.ImportProteinTreeAnalysisParameters>
{
    public ImportProteinTreeAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new ImportProteinTreeAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Querying homology hub" );
        HomologyHub bioHub = (HomologyHub)BioHubRegistry.getBioHub( "HomologyHub" );
        jobControl.setPreparedness( 1 );
        log.info( "Downloading from "+parameters.getUrl()+"..." );
        jobControl.pushProgress( 1, 40 );
        try (TempFile destinationFile = TempFiles.file( "tree.gz" ))
        {
            FileDownloader.downloadFile( new URL( parameters.getUrl() ), destinationFile, jobControl );
            jobControl.popProgress();
            if( jobControl.isStopped() )
                return null;
            log.info( "Unpacking..." );
            jobControl.pushProgress( 40, 50 );
            try (TempFile unpackedFile = TempFiles.file( "tree.txt", new GZIPInputStream( new FileInputStream( destinationFile ) ) ))
            {
                jobControl.popProgress();
                if( jobControl.isStopped() )
                    return null;
                Connection conn = bioHub.getConnection();
                log.info( "Using connection "+conn );
                log.info( "Creating schema..." );
                jobControl.pushProgress( 50, 55 );
                ImportProteinTree.createSchema( conn );
                jobControl.popProgress();
                log.info( "Importing..." );
                jobControl.pushProgress( 55, 100 );
                ImportProteinTree.insert( unpackedFile, conn, jobControl );
                jobControl.popProgress();
                log.info( "Done" );
            }
        }
        return null;
    }

    public static class ImportProteinTreeAnalysisParameters extends AbstractAnalysisParameters
    {
        private String url = "ftp://ftp.ensembl.org/pub/current_emf/ensembl-compara/homologies/";

        @PropertyName("URL to the *.nh.emf.gz file")
        @PropertyDescription("Full url to the file like Compara.84.protein.nh.emf.gz")
        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            Object oldValue = this.url;
            this.url = url;
            firePropertyChange( "url", oldValue, this.url );
        }
    }

    public static class ImportProteinTreeAnalysisParametersBeanInfo extends BeanInfoEx
    {
        public ImportProteinTreeAnalysisParametersBeanInfo()
        {
            super( ImportProteinTreeAnalysisParameters.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "url" );
        }
    }
}
