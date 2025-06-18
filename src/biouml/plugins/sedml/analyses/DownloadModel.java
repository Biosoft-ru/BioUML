package biouml.plugins.sedml.analyses;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biouml.model.Diagram;
import biouml.plugins.download.FileDownloader;
import biouml.plugins.sbml.SbmlImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DownloadModel extends AnalysisMethodSupport<DownloadModel.Parameters>
{
    private static final String BIOMODELS_PREFIX =  "https://www.ebi.ac.uk/biomodels/model/download/";
    private static final String BIOMODELS_SUFFIX =  "_urn.xml";
    private static final String BIOMODELS_MIDDLE =  "?filename=";
    private static final String SBML_EXTENSION =  ".sbml";
    private static final Pattern BIOMODELS_PATTERN = Pattern.compile( "urn:miriam:biomodels.db:(BIOMD[0-9]*)" );
    
    public DownloadModel(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath outPath = parameters.getOutputPath();
        Diagram result = downloadModel( parameters.getSource(), outPath.getName(), outPath.getParentCollection(), jobControl, log );
        outPath.save( result );
        return result;
    }


    public static Diagram downloadModel(String source, String name, DataCollection<?> parent, JobControl jobControl, Logger log)
            throws Exception
    {
        Matcher matcher = BIOMODELS_PATTERN.matcher( source );
        if( matcher.matches() )
        {
            String id = matcher.group( 1 );
            //URL is created accordingto https://bitbucket.org/biomodels/jummp-biomodels/wiki/Web%20Services
            //_urn.xml suffix might be not obligatory for each model
            //better to get complete file list with /model/files/model-identifier?format=xml query and select correct file name
            source = BIOMODELS_PREFIX + id + BIOMODELS_MIDDLE + id + BIOMODELS_SUFFIX;
            //source = "http://www.ebi.ac.uk/biomodels-main/download?mid=" + id;
            //source = "http://biomodels.caltech.edu/download?mid=" + id;
        }

        URL url = new URL( TextUtil2.decodeURL( source ) );
        File modelFile = TempFiles.file( SBML_EXTENSION );
        FileDownloader.downloadFile( url, modelFile, new SubFunctionJobControl( jobControl, 0, 50 ) );
        return (Diagram)new SbmlImporter().doImport( parent, modelFile, name, new SubFunctionJobControl( jobControl, 50, 100 ), log );
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private String source;
        private DataElementPath outputPath;
        public String getSource()
        {
            return source;
        }
        public void setSource(String source)
        {
            this.source = source;
        }
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            this.outputPath = outputPath;
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
            add( "source" );
            property( "outputPath" ).outputElement( Diagram.class ).add();
        }
    }
}
