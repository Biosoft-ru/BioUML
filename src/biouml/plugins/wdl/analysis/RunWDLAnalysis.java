package biouml.plugins.wdl.analysis;

import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.TempFiles;

public class RunWDLAnalysis extends AnalysisMethodSupport<RunWDLAnalysisParameters>
{
    private static final Logger log = Logger.getLogger( RunWDLAnalysis.class.getName() );
    
    public RunWDLAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new RunWDLAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "WDL Analysis started" );
        Diagram diagram =parameters.getWdlPath().getDataElement( Diagram.class );
        DataElementPath outPath = parameters.getOutputPath();

        String nextFlow = new NextFlowGenerator().generate( diagram );
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        WorkflowSettings settings = parameters.getSettings();
        settings.setOutputPath( outPath );
        String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();
        NextFlowRunner.runNextFlowByDiagram( diagram, nextFlow, settings, outputDir, isWindows );

        log.info( "WDL Analysis finished" );
        return new Object[0];
    }

}