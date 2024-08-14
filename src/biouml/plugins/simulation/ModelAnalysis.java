package biouml.plugins.simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.TempFiles;

/**
 * @author axec
 * Analysis generates report on the model
 */
@ClassIcon ( "resources/simulation-analysis.gif" )
public class ModelAnalysis extends AnalysisMethodSupport<ModelAnalysisParameters>
{
    private ModelAnalysisJobControl jobControl;

    public ModelAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new ModelAnalysisParameters() );
        log = Logger.getLogger( ModelAnalysis.class.getName() );
        jobControl = new ModelAnalysisJobControl( Logger.getLogger( ModelAnalysis.class.getName() ) );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        ModelAnalysisParameters params = getParameters();
        if( params.getModelPath() != null )
        {
            super.validateParameters();
            Diagram diagram = parameters.getModelPath().getDataElement( Diagram.class );
            if( diagram.getRole() == null || ! ( diagram.getRole() instanceof EModel ) )
                throw new IllegalArgumentException( "Diagram does not contain a model. Please, select valid diagram." );
        }
        SimulationEngine engine = parameters.getSimulationEngine();
        if( engine == null )
            throw new IllegalArgumentException( "Simulation engine was not set." );

        if( ! ( engine instanceof JavaSimulationEngine ) )
            throw new IllegalArgumentException( "Only Java Simulation Engine is supported." );

    }

    @Override
    public ModelAnalysisJobControl getJobControl()
    {
        return jobControl;
    }

    @Override
    public HtmlDataElement justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        ModelAnalysisParameters params = getParameters();

        JavaSimulationEngine engine = (JavaSimulationEngine)params.getSimulationEngine();
        engine.setTerminated( false );
        JavaBaseModel model = (JavaBaseModel)engine.createModel();
        model.init();

        //Step 1. Check rates
        Map<Integer, String> rateMapping = EntryStream.of( engine.varNameRateIndexMapping ).invert().toMap();
        double[] x_values = model.getY();
        double[] dydt = model.dy_dt( 0, x_values );
        double[] abs = DoubleStreamEx.of( dydt ).map( d->Math.abs( d ) ).toArray();
        int[] oldIndices = Util.sort( abs );
        int rateNumber = Math.min( 5, dydt.length );
        String[] rateVariables = new String[rateNumber];
        double[] rates = new double[rateNumber];
        for( int i = 0; i < rateNumber; i++ )
        {
            int oldIndex = oldIndices[dydt.length - 1 - i];
            rateVariables[i] = rateMapping.get( oldIndex );
            rates[i] = dydt[oldIndex];
        }
        log.info("Variables with largest rate:" );
        for( int i = 0; i < rateNumber; i++ )
        {
            log.info( rateVariables[i] + ":\t" + rates[i] );
        }

        ModelReport report = new ModelReport();
        report.setRates(rates);
        report.setRateVariables(rateVariables);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put( "report", report );
        context.put( "name", engine.getDiagram().getName() );
        String name = params.getReportPath().getName();
        DataCollection resultCollection = params.getReportPath().getParentCollection();
        return generateReport( name, resultCollection, getClass().getResourceAsStream( "resources/modelReport.vm" ), context );
    }

    public static class ModelReport
    {
        private String[] rateVariables;
        private double[] rates;
        
        public String[] getRateVariables()
        {
            return rateVariables;
        }
        public void setRateVariables(String[] rateVariables)
        {
            this.rateVariables = rateVariables;
        }
        public double[] getRates()
        {
            return rates;
        }
        public void setRates(double[] rates)
        {
            this.rates = rates;
        }
        
        public String getSt()
        {
            return "sa";
        }
        
        public String toString()
        {
            return "a"+rates[0];
        }
    }

    //TODO: create utility class to manage velocity
    private static HtmlDataElement generateReport(String name, DataCollection parent, InputStream template, Map<String, Object> contextMap)
            throws Exception
    {
        Properties p = new Properties();
        File templateFile = TempFiles.file( "template" );

        try (BufferedWriter bw = ApplicationUtils.utfWriter( templateFile );
                BufferedReader reader = new BufferedReader( new InputStreamReader( template, StandardCharsets.UTF_8 ) ))
        {
            bw.write( StreamEx.of( reader.lines() ).joining( "\n" ) );
        }

        p.setProperty( "file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader" );
        p.setProperty( "file.resource.loader.path", templateFile.getParentFile().getAbsolutePath() );

        // experimental, possble fix for 
        // Runtime : ran out of parsers. Creating a new one.  Please increment the parser.pool.size property. The current value is too small.
        p.setProperty( "parser.pool.size", "50" );

        final VelocityEngine engine = new VelocityEngine( p );
        engine.init();

        Template velocityTemplate = engine.getTemplate( templateFile.getName(), "UTF-8" );
        VelocityContext context = new VelocityContext();
        contextMap.entrySet().forEach( e -> context.put( e.getKey(), e.getValue() ) );

        File resultFile = TempFiles.file( name );
        try (BufferedWriter bw = ApplicationUtils.utfWriter( resultFile ))
        {
            velocityTemplate.merge( context, bw );
        }
        HtmlDataElement html = new HtmlDataElement( name, parent, ApplicationUtils.readAsString( resultFile ) );
        parent.put( html );
        return html;
    }

    public class ModelAnalysisJobControl extends AnalysisJobControl implements ResultListener
    {
        private double percentStep;

        public ModelAnalysisJobControl(Logger l)
        {
            super( ModelAnalysis.this );
        }

        @Override
        public void add(double t, double[] y) throws Exception
        {
            setPreparedness( (int) ( t * percentStep ) );
        }

        @Override
        public void start(Object model)
        {
        }

        public void setPercentStep(double step)
        {
            percentStep = step;
        }

        @Override
        protected void setTerminated(int status)
        {
            ModelAnalysisParameters params = getParameters();
            SimulationEngine engine = params.getSimulationEngine();
            engine.stopSimulation();
            super.setTerminated( status );
        }
    }
}
