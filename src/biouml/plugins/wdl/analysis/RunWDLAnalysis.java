package biouml.plugins.wdl.analysis;

import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;

public class RunWDLAnalysis extends AnalysisMethodSupport<RunWDLAnalysisParameters>
{
    private static final Logger log = Logger.getLogger( RunWDLAnalysis.class.getName() );
    private RunWDLAnalysisParameters parameters = new RunWDLAnalysisParameters();
    
    public RunWDLAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new RunWDLAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "WDL Analysis started" );
        Diagram diagram =parameters.getWdlPath().getDataElement( Diagram.class );

        String nextFlow = new NextFlowGenerator().generate( diagram );
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        NextFlowRunner.runNextFlow( diagram, null, parameters.getSettings(), nextFlow, isWindows );

        log.info( "WDL Analysis finished" );
        return new Object[0];
    }

}