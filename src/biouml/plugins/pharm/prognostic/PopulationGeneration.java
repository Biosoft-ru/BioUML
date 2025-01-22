package biouml.plugins.pharm.prognostic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.util.DiagramXmlConstants;
import biouml.plugins.modelreduction.SteadyStateAnalysis;
import biouml.plugins.modelreduction.SteadyStateAnalysisParameters;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationConstraintCalculator;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationMethodRegistry;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.optimization.SingleExperimentParameterEstimation;
import biouml.plugins.pharm.prognostic.PatientPhysiology.Diagnosis;
import biouml.plugins.pharm.prognostic.PatientPhysiology.NYHA;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod.SRESOptMethodParameters;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class PopulationGeneration extends AnalysisMethodSupport<PopulationGenerationParameters>
{
    private Map<String, Double> knownVariables = new HashMap<String, Double>();
    private List<String> knownParameters = new ArrayList<String>();

    private Optimization slowModelOptimization;
    private Optimization fastModelOptimization;

    private OptimizationExperiment slowModelOptExp;
    private OptimizationExperiment fastModelOptExp;

    private int fixedSlowModelParam;
    private int fixedFastModelParam;

    private Diagram compositeDiagram;

    private AnalysisJobControl nestedJobControl = null;

    private Double[] lastSuccessfulPatient;

    public PopulationGeneration(DataCollection<?> origin, String name)
    {
        super( origin, name, new PopulationGenerationParameters() );
        log = Logger.getLogger( PopulationGeneration.class.getName() );
    }

    @Override
    protected AnalysisJobControl createJobControl()
    {
        return new AnalysisJobControl( this )
        {
            @Override
            protected void setTerminated(int status)
            {
                super.setTerminated( status );
                if( nestedJobControl != null )
                    nestedJobControl.terminate();
            }
        };
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        if( parameters.getOutput() == null )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_OUTPUT" ) );

        if( Double.isNaN( parameters.getPatientPhysiology().getGeneralData().getHeight() ) )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_HEIGHT" ) );

        if( Double.isNaN( parameters.getPatientPhysiology().getGeneralData().getWeight() ) )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_WEIGHT" ) );

        if( Double.isNaN( parameters.getPatientPhysiology().getPressure().getPs() ) )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_PS" ) );

        if( Double.isNaN( parameters.getPatientPhysiology().getPressure().getPd() ) )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_PD" ) );

        if( Double.isNaN( parameters.getPatientPhysiology().getEcg().getHr() ) )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_HR" ) );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
    	DataCollection<?> output = DataCollectionUtils.createSubCollection( parameters.getOutput() );
    	DataElementPath patientsPath = DataElementPath.create( output, "virtual_patients" );
        TableDataCollection patients = TableDataCollectionUtils.createTableDataCollection( patientsPath );
        return justAnalyzeAndPut(1000, 100, 10, patients);
    }

    public Object justAnalyzeAndPut(int attempts, int slowModelAttempts, int fastModelAttempts, TableDataCollection patients) throws Exception
    {
    	DataElementPath patientsPath = DataElementPath.create(patients);
    	DataCollection<?> output = patientsPath.getParentCollection();

        int num = parameters.getPatientsNumber();

        log.info( MessageBundle.getMessage( "INFO_INITIALIZATION" ) );
        init( output );

        String[] slowModelColumns = null;
        String[] fastModelColumns = null;

        int currNum = 1;
        int currAttempt = 0;
        while( currNum <= num && !jobControl.isStopped() && currAttempt < attempts)
        {
            currAttempt++;
            log.info( MessageBundle.format( "INFO_GENERATION", currNum ) );

            log.info( MessageBundle.getMessage( "INFO_SLOW_MODEL_GENERATION" ) );
            SimulationResult slowModelSR = generate( slowModelOptimization, output, null, slowModelAttempts );

            if( jobControl.isStopped() )
                continue;

            if( slowModelSR == null )
            {
                log.info( MessageBundle.getMessage( "INFO_SLOW_MODEL_GENERATION_IS_FAILED" ) );
                break;
            }

            //DataElementPath.create( output, "slowModelSR" ).save( slowModelSR.clone(output, "slowModelSR") );

            log.info( MessageBundle.getMessage( "INFO_FAST_MODEL_GENERATION" ) );
            SimulationResult fastModelSR = generate( fastModelOptimization, output, slowModelSR, fastModelAttempts );

            if( fastModelSR == null || jobControl.isStopped() )
                continue;

            //DataElementPath.create( output, "fastModelSR" ).save( fastModelSR.clone(output, "fastModelSR") );

            if( slowModelColumns == null )
                slowModelColumns = initColumns( patients, slowModelSR, PopulationUtils.SLOW_MODEL );
            if( fastModelColumns == null )
                fastModelColumns = initColumns( patients, fastModelSR, PopulationUtils.FAST_MODEL );

            log.info( MessageBundle.getMessage( "INFO_MERGING" ) );
            Double[] patient = merge( compositeDiagram, slowModelSR, fastModelSR, slowModelColumns, fastModelColumns );

            if( jobControl.isStopped() )
                continue;

            if( patient != null )
            {
                lastSuccessfulPatient = patient;
                writePatient( patientsPath, patients, patient );
                jobControl.setPreparedness( (int) ( ( (long)currNum * 100 ) / num ) );
                currNum++;
            }
            else if( lastSuccessfulPatient != null )
            {
                refreshStartValues( slowModelOptimization, lastSuccessfulPatient, patients, PopulationUtils.SLOW_MODEL );
                refreshStartValues( fastModelOptimization, lastSuccessfulPatient, patients, PopulationUtils.FAST_MODEL );
            }
        }

        return patients;
    }

    private SimulationResult generate(Optimization optimization, DataCollection<?> output, SimulationResult slowModelSR, int attempts)
            throws Exception
    {
        SimulationResult sr = null;
        int currAttempt = 0;

        int ind = fixedSlowModelParam;
        if( slowModelSR != null )
            ind = fixedFastModelParam;

        try
        {
            while( sr == null && currAttempt < attempts && !jobControl.isStopped() )
            {
                OptimizationMethod<?> method = optimization.getOptimizationMethod();
                List<Parameter> params = method.getOptimizationProblem().getParameters();

                if( ind == params.size() )
                    ind = 0;
                else if( slowModelSR != null )
                {
                    String pName = params.get( ind ).getName();
                    if( pName.equals( "V" ) || pName.equals( "AT1_ANGII" ) || pName.equals( "Hct" ) )
                        ind = 0;
                }

                Parameter oldParam = params.get( ind ).copy();

                SingleExperimentParameterEstimation problem = (SingleExperimentParameterEstimation)method.getOptimizationProblem();

                if( slowModelSR != null )
                {
                    fixParameterValue( params, slowModelSR, "V" );
                    fixParameterValue( params, slowModelSR, "AT1_ANGII" );

                    if( !knownParameters.contains( "Hct" ) )
                        fixParameterValue( params, slowModelSR, "Hct" );

                    if( !knownVariables.containsKey( "CO" ) )
                    {
                        int srIndex = slowModelSR.getVariablePathMap().get( "CO" );
                        double initialCO = slowModelSR.getValues()[slowModelSR.getValues().length - 1][srIndex];
                        Iterator<RowDataElement> expIt = problem.getExperiment().getTableSupport().getTable().iterator();
                        expIt.next().setValue( "CO", initialCO );
                        fmExp.getAt( 0 ).setValue( "CO", initialCO );
                        CollectionFactoryUtils.save( fmExp );
                    }
                }

                double fixedValue = random( oldParam.getLowerBound(), oldParam.getUpperBound() );
                fixParameterValue( params.get( ind ), fixedValue );

                nestedJobControl = method.getJobControl();
                nestedJobControl.begin();
                double[] solution = method.getSolution();
                nestedJobControl.end();
                nestedJobControl = null;

                if( jobControl.isStopped() )
                    continue;

                sr = (SimulationResult)problem.getSimulationResult( solution );

                log.info( MessageBundle.format( "INFO_OPTIMIZATION_RESULT", oldParam.getName(), Double.toString( fixedValue ),
                        Double.toString( method.getPenalty() ), Double.toString( method.getDeviation() ) ) );

                params.get( ind ).setValue( oldParam.getValue() );
                params.get( ind ).setLowerBound( oldParam.getLowerBound() );
                params.get( ind ).setUpperBound( oldParam.getUpperBound() );

                ind++;

                if( suitable( sr, method.getPenalty() ) )
                {
                    refreshStartValues( optimization, solution );
                    return sr;
                }

                sr = null;
                log.info( MessageBundle.getMessage( "INFO_INCORRECT_SOLUTION" ) );
                currAttempt++;
            }

            return null;
        }
        finally
        {
            if( slowModelSR != null )
                fixedFastModelParam = ind;
            else
                fixedSlowModelParam = ind;
        }
    }

    private void refreshStartValues(Optimization optimization, double[] values)
    {
        OptimizationMethod<?> method = optimization.getOptimizationMethod();
        List<Parameter> params = method.getOptimizationProblem().getParameters();

        for( int i = 0; i < params.size(); ++i )
        {
            params.get( i ).setValue( values[i] );
        }
    }

    private void refreshStartValues(Optimization optimization, Double[] patient, TableDataCollection patients, String model)
    {
        OptimizationMethod<?> method = optimization.getOptimizationMethod();
        List<Parameter> params = method.getOptimizationProblem().getParameters();

        for( Parameter param : params )
        {
            String name = model + "_" + param.getName();
            int index = patients.getColumnModel().getColumnIndex( name );
            param.setValue( patient[index] );
        }
    }

    private String[] initColumns(TableDataCollection tdc, SimulationResult sr, String model)
    {
        List<String> columns = new ArrayList<String>();

        Iterator<String> it = sr.getVariablePathMap().keySet().iterator();
        while( it.hasNext() )
        {
            String col = it.next();
            columns.add( col );
        }

        Collections.sort( columns );

        if( tdc.getColumnModel().getColumnCount() == 0 )
            for( String col : columns )
                tdc.getColumnModel().addColumn( model + "_" + col, DataType.Float );

        return columns.toArray( new String[columns.size()] );
    }

    private void fixParameterValue(List<Parameter> params, SimulationResult sr, String paramName)
    {
        Parameter param = StreamEx.of( params ).findFirst( p -> p.getName().equals( paramName ) ).get();
        int srIndex = sr.getVariablePathMap().get( paramName );
        double value = sr.getValues()[sr.getValues().length - 1][srIndex];
        fixParameterValue( param, value );
    }

    private void fixParameterValue(Parameter param, double fixedValue)
    {
        param.setValue( fixedValue );
        param.setLowerBound( fixedValue );
        param.setUpperBound( fixedValue );
    }

    private double random(double min, double max)
    {
        return Math.random() * ( max - min ) + min;
    }

    private boolean suitable(SimulationResult sr, double penalty)
    {
        if( penalty == 0.0 )
        {
            int last = sr.getValues().length - 1;

            for( String var : knownVariables.keySet() )
            {
                double controlValue = knownVariables.get( var );

                if( sr.getVariablePathMap().containsKey( var ) )
                {
                    int index = sr.getVariablePathMap().get( var );
                    double value = sr.getValues()[last][index];

                    if( !suitable( var, value, controlValue ) )
                        return false;
                }
            }
        }
        else
            return false;
        return true;
    }

    private boolean suitable(Diagram diagram, Optimization opt) throws Exception
    {
        for( String var : knownVariables.keySet() )
        {
            double controlValue = knownVariables.get( var );
            if( ((EModel)diagram.getRole()).containsVariable( var ))
            {
                double value = ((EModel)diagram.getRole()).getVariable( var ).getInitialValue();
                if( !suitable( var, value, controlValue ) )
                    return false;
            }
        }
        return checkConstraints( diagram, opt );
    }

    private boolean checkConstraints(Diagram diagram, Optimization opt) throws Exception
    {
        OptimizationConstraintCalculator calculator = ( (SingleExperimentParameterEstimation)opt.getOptimizationMethod()
                .getOptimizationProblem() ).getCalculator();
        
        SimulationEngine engine = DiagramUtility.getPreferredEngine( diagram );
        engine.setDiagram( diagram );
        if( diagram.getName().equals( PopulationUtils.FAST_MODEL + "_ss" ) )
            engine.setTimeIncrement( 0.005 );
        SimulationResult sr = new SimulationResult( diagram.getOrigin(), "" );
        Model model = engine.createModel();
        engine.simulate( model, sr );

        int cNum = opt.getParameters().getOptimizationConstraints().size();

        //Ignore auxiliary constraints in the slow model optimization: Fi_sodin-Fi_u_sod>-1.0E-8; Fi_sodin-Fi_u_sod<1.0E-8; Fi_win-Fi_u>-1.0E-8; Fi_win-Fi_u<1.0E-8
        if( diagram.getName().startsWith( PopulationUtils.SLOW_MODEL ) )
            cNum -= 4;

        for( int i = 0; i < cNum; ++i )
        {
            OptimizationConstraint constraint = opt.getParameters().getOptimizationConstraints().get( i );
            double inaccuracy = calculator.getConstraintInaccuracy(i, sr, constraint.getInitialTime(), constraint.getCompletionTime());
            if( inaccuracy > 1E-6 )
            {
                log.info( MessageBundle.format( "INFO_INCORRECT_SOLUTION_AFTER_MERGING", constraint.getFormula(), Double.toString( inaccuracy ) ) );
                return false;
            }
        }
        return true;
    }

    private boolean suitable(String variable, double value, double controlValue)
    {
    	double deviation = parameters.getOptimizationSettings().getPossibleDeviation();
        if( value > ( 1 + deviation ) * controlValue || value < ( 1 - deviation ) * controlValue )
        {
            log.info( MessageBundle.format( "INFO_INCORRECT_VALUE", variable, value ) );
            return false;
        }
        return true;
    }

    private String[] targetVariables = new String[] {"V"};

    private Double[] merge(Diagram diagram, SimulationResult slowModelSR, SimulationResult fastModelSR, String[] slowModelColumns,
            String[] fastModelColumns) throws Exception
    {
        String smName = PopulationUtils.SLOW_MODEL + "_ss";
        String fmName = PopulationUtils.FAST_MODEL + "_ss";

        SteadyStateAnalysis ssAnalysis = new SteadyStateAnalysis( null, "", diagram );
        ssAnalysis.getLogger().setLevel( Level.SEVERE );

        SteadyStateAnalysisParameters ssParameters = ssAnalysis.getParameters();
        ssParameters.setRelativeTolerance( 0.1 );
        ssParameters.setAbsoluteTolerance( 0.1 );
        //  analysisParameters.setValidationSize(1000);
        VariableSet variableNames = new VariableSet( diagram, smName, targetVariables );
        ssParameters.setVariableNames( variableNames );
        ssParameters.getEngineWrapper().getEngine().setCompletionTime( 3000000.0 );
        ssParameters.getEngineWrapper().getEngine().setTimeIncrement( 6000.0 );

        Diagram smDiagram = ( (SubDiagram)diagram.get( smName ) ).getDiagram();
        Diagram fmDiagram = ( (SubDiagram)diagram.get( fmName ) ).getDiagram();
        setValues( smDiagram, slowModelSR );
        setValues( fmDiagram, fastModelSR );

        nestedJobControl = ssAnalysis.getJobControl();
        nestedJobControl.begin();
        Map<String, Double> values = ssAnalysis.findSteadyState( diagram );
        nestedJobControl.end();
        nestedJobControl = null;

        if( values != null )
        {
            log.info( MessageBundle.format( "INFO_MERGING_IS_SUCCESSFUL", values.get( "time" ) ) );

            setValues(smDiagram, values);
            setValues(fmDiagram, values);

            if( suitable( smDiagram, slowModelOptimization ) && suitable( fmDiagram, fastModelOptimization ) )
            {
                if( !parameters.getOptimizationSettings().isCheckSodiumLoadExperiment() || sodiumLoadExperiment(diagram, values) )
                {
                    Double[] row = new Double[slowModelColumns.length + fastModelColumns.length];
                    for( int i = 0; i < slowModelColumns.length; ++i )
                    {
                        row[i] = values.get( smName + "/" + slowModelColumns[i] );
                    }
                    for( int i = 0; i < fastModelColumns.length; ++i )
                    {
                        row[i + slowModelColumns.length] = values.get( fmName + "/" + fastModelColumns[i] );
                    }
                    return row;
                }
            }
        }
        else if( !jobControl.isStopped() )
            log.info( MessageBundle.getMessage( "INFO_MERGING_IS_FAILED" ) );

        return null;
    }

    private boolean sodiumLoadExperiment(Diagram diagram, Map<String, Double> values)
    {
        log.info( MessageBundle.getMessage( "INFO_SODIUM_LOAD_EXPERIMENT" ) );

        Diagram smDiagram = ( (SubDiagram)diagram.get( PopulationUtils.SLOW_MODEL + "_ss" ) ).getDiagram();
        ( (EModel)smDiagram.getRole() ).getVariable( "Fi_sodin" ).setInitialValue( 0.24306 ); //high sodium diet (0.24306 mEq/min ≈ 350 mmol/d)

        try
        {
            SimulationEngine engine = DiagramUtility.getPreferredEngine( diagram );
            engine.setDiagram( diagram );
            engine.setCompletionTime( 2419200.0 ); // 2419200 sec = 4 weeks
            engine.setTimeIncrement( 6000 );

            SimulationResult sr = new SimulationResult( diagram.getOrigin(), "" );
            Model model = engine.createModel();
            String status = engine.simulate( model, sr );

            SimulatorProfile profile = ( (Simulator)engine.getSolver() ).getProfile();
            if( ( status != null && status.length() > 0 ) || profile.isStiff() || profile.isUnstable() )
            {
                log.info( MessageBundle.getMessage( "INFO_FAILED_SODIUM_LOAD_EXPERIMENT" ) );
                return false;
            }

            int ind = engine.getVarPathIndexMapping().get( PopulationUtils.FAST_MODEL + "_ss" + "/P_S" );
            double[] times = sr.getTimes();
            for( int i = 0; i < times.length - 1; ++i )
            {
                if( sr.getValues()[i + 1][ind] < 0.9 * sr.getValues()[i][ind] )
                {
                    log.info( MessageBundle.getMessage( "INFO_FAILED_SODIUM_LOAD_EXPERIMENT" ) );
                    return false;
                }
            }
            if( sr.getValues()[times.length - 1][ind] > sr.getValues()[0][ind] + 25)
            {
                log.info( MessageBundle.getMessage( "INFO_FAILED_SODIUM_LOAD_EXPERIMENT" ) );
                return false;
            }
        }
        catch( Exception e )
        {
            log.info( MessageBundle.getMessage( "INFO_FAILED_SODIUM_LOAD_EXPERIMENT" ) );
            return false;
        }

        return true;
    }

    private void setValues(Diagram diagram, SimulationResult sr)
    {
        EModel model = diagram.getRole( EModel.class );

        int last = sr.getValues().length - 1;

        Map<String, Integer> varMap = sr.getVariablePathMap();
        Iterator<String> it = varMap.keySet().iterator();
        while( it.hasNext() )
        {
            String var = it.next();
            int index = varMap.get( var );
            double value = sr.getValues()[last][index];
            model.getVariable( var ).setInitialValue( value );
        }
    }

    private void setValues(Diagram diagram, Map<String, Double> values)
    {
        String dName = diagram.getName();
        for( String param : values.keySet() )
        {
            double val = values.get( param );
            if( param.startsWith( dName ) )
            {
                String var = param.substring( dName.length() + 1 );
                if( !var.equals( "time" ) )
                    ( (EModel)diagram.getRole() ).getVariable( var ).setInitialValue( val );
            }
        }
    }

    private void writePatient(DataElementPath patientsPath, TableDataCollection patients, @Nonnull Double[] patient)
    {
        TableDataCollectionUtils.addRow( patients, Integer.toString( patients.getSize() + 1 ), patient, false );
        patientsPath.save( patients );
    }

    private TableDataCollection fmExp;
    private void init(DataCollection<?> output) throws Exception
    {
        DataCollection<?> input = parameters.getInput().getDataCollection();
        Logger log = Logger.getLogger( TableDataCollection.class.getName() );
        Level level = log.getLevel();
        log.setLevel( Level.SEVERE );

        Diagram slowModel = (Diagram)input.get( PopulationUtils.SLOW_MODEL );
        Diagram fastModel = (Diagram)input.get( PopulationUtils.FAST_MODEL );

        TableDataCollection smParams = (TableDataCollection)input.get( "SlowModelParameters" );
        TableDataCollection fmParams = (TableDataCollection)input.get( "FastModelParameters" );

        TableDataCollection smConstr = (TableDataCollection)input.get( "SlowModelConstraints" );
        TableDataCollection fmConstr = (TableDataCollection)input.get( "FastModelConstraints" );

        TableDataCollection paramMapping = (TableDataCollection)input.get( "ParameterMapping" );

        Diagram smDiagram = initSlowModelDiagram( slowModel, output );
        Diagram fmDiagram = initFastModelDiagram( fastModel, slowModel, output, paramMapping );

        CollectionFactoryUtils.save( smDiagram );
        CollectionFactoryUtils.save( fmDiagram );

        compositeDiagram = PopulationUtils.initCompositeDiagram( smDiagram, fmDiagram, output );
        CollectionFactoryUtils.save( compositeDiagram );

        TableDataCollection smExp = initSlowModelExperiment( output, smDiagram );
        fmExp = initFastModelExperiment( output, fmDiagram, smDiagram, paramMapping );

        CollectionFactoryUtils.save( smExp );
        CollectionFactoryUtils.save( fmExp );

        slowModelOptimization = initSlowModelOptimization( output, smDiagram, smParams, smConstr, smExp );
        fastModelOptimization = initFastModelOptimization( output, fmDiagram, fmParams, fmConstr, fmExp );

        CollectionFactoryUtils.save( slowModelOptimization );
        CollectionFactoryUtils.save( fastModelOptimization );

        initOptimizationProblem( slowModelOptimization, slowModelOptExp );
        initOptimizationProblem( fastModelOptimization, fastModelOptExp );

        fixedSlowModelParam = 0;
        fixedFastModelParam = 0;

        log.setLevel( level );
    }

    private Diagram initSlowModelDiagram(Diagram slowModel, DataCollection<?> output) throws Exception
    {
        Diagram patientSM = slowModel.clone( output, slowModel.getName() );
        EModel model = patientSM.getRole( EModel.class );

        PatientPhysiology physiology = parameters.getPatientPhysiology();

        double map = physiology.getCalculatedParameters().getMap();
        model.getVariable( "MAP" ).setInitialValue( map );

        double co = physiology.getCalculatedParameters().getCo();
        if( !Double.isNaN( co ) )
        {
            model.getVariable( "CO" ).setInitialValue( co );
            knownVariables.put( "CO", co );
        }

        double potassium = physiology.getBiochemistry().getPotassium();
        if( !Double.isNaN( potassium ) )
        {
            model.getVariable( "C_K" ).setInitialValue( potassium );
            knownParameters.add( "C_K" );
        }

        double glucose = physiology.getBiochemistry().getGlucose();
        if( !Double.isNaN( glucose ) )
        {
            model.getVariable( "glucose" ).setInitialValue( glucose );
            knownParameters.add( "glucose" );
        }

        double urea = physiology.getBiochemistry().getUrea();
        if( !Double.isNaN( urea ) )
        {
            model.getVariable( "urea" ).setInitialValue( urea );
            knownParameters.add( "urea" );
        }

        double tp = physiology.getBiochemistry().getTp();
        if( !Double.isNaN( tp ) )
        {
            model.getVariable( "TP" ).setInitialValue( tp );
            knownParameters.add( "TP" );
        }

        double hct = physiology.getBloodTest().getHct();
        if( !Double.isNaN( hct ) )
        {
            model.getVariable( "Hct" ).setInitialValue( hct );
            knownParameters.add( "Hct" );
        }

        return patientSM;
    }

    private Diagram initFastModelDiagram(Diagram fastModel, Diagram slowModel, DataCollection<?> output, TableDataCollection paramMapping) throws Exception
    {
        Diagram patientFM = fastModel.clone( output, fastModel.getName() );
        EModel model = patientFM.getRole( EModel.class );

        PatientPhysiology physiology = parameters.getPatientPhysiology();

        double weight = physiology.getGeneralData().getWeight();
        model.getVariable( "m" ).setInitialValue( weight );

        double he = physiology.getBloodTest().getHe();
        if( !Double.isNaN( he ) )
        {
        	String heName = (String) paramMapping.get("Hemoglobin").getValue("Designation");
            model.getVariable( heName ).setInitialValue( he );
            knownParameters.add( heName );
        }

        double hct = physiology.getBloodTest().getHct();
        if( Double.isNaN( hct ) )
            hct = slowModel.getRole( EModel.class ).getVariable( "Hct" ).getInitialValue();
        model.getVariable( "Hct" ).setInitialValue( hct );

        return patientFM;
    }

    private TableDataCollection initSlowModelExperiment(DataCollection<?> output, Diagram slowModel) throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( output, "exp_" + slowModel.getName() );
        List<Double> values = new ArrayList<Double>();

        tdc.getColumnModel().addColumn( "time", DataType.Float );
        DynamicProperty dp = slowModel.getAttributes().getProperty( DiagramXmlConstants.SIMULATION_OPTIONS );
        if( dp != null && dp.getValue() instanceof SimulationEngine )
        {
            SimulationEngine se = (SimulationEngine)dp.getValue();
            values.add( se.getCompletionTime() );
        }
        else
            throw new Exception( "ERROR_SLOW_MODEL_EXPERIMENT_GENERATION" );

        double sod = parameters.getPatientPhysiology().getBiochemistry().getSodium();
        if( !Double.isNaN( sod ) )
        {
            tdc.getColumnModel().addColumn( "C_sod", DataType.Float );
            values.add( sod );
            knownVariables.put( "C_sod", sod );
        }

        double gfr = parameters.getPatientPhysiology().getCalculatedParameters().getGfr();
        if( !Double.isNaN( gfr ) )
        {
            tdc.getColumnModel().addColumn( "GFR", DataType.Float );
            values.add( gfr );
            knownVariables.put( "GFR", gfr );
        }

        String arg389Gly = parameters.getPatientPhysiology().getGenetics().getADRB1().getArg389Gly();
        if( !arg389Gly.equals( PatientPhysiology.Arg389Gly.UNKNOWN.toString() ) )
        {
            tdc.getColumnModel().addColumn( "PRC", DataType.Float );
            double prc = parameters.getPatientPhysiology().getGenetics().getADRB1().getPRC();
            values.add( prc );
            knownVariables.put( "PRC", prc );
        }

        TableDataCollectionUtils.addRow( tdc, "1", values.toArray() );
        return tdc;
    }

    private TableDataCollection initFastModelExperiment(DataCollection<?> output, Diagram fastModel, Diagram slowModel, TableDataCollection paramMapping) throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( output, "exp_" + fastModel.getName() );
        List<Double> values = new ArrayList<Double>();

        tdc.getColumnModel().addColumn( "time", DataType.Float );
        DynamicProperty dp = fastModel.getAttributes().getProperty( DiagramXmlConstants.SIMULATION_OPTIONS );
        if( dp != null && dp.getValue() instanceof SimulationEngine )
        {
            SimulationEngine se = (SimulationEngine)dp.getValue();
            values.add( se.getCompletionTime() );
        }
        else
            throw new Exception( "ERROR_FAST_MODEL_EXPERIMENT_GENERATION" );

        double ps = parameters.getPatientPhysiology().getPressure().getPs();
        String psName = (String) paramMapping.get("SBP").getValue("Designation");
        tdc.getColumnModel().addColumn( psName, DataType.Float );
        values.add( ps );
        knownVariables.put( psName, ps );

        double pd = parameters.getPatientPhysiology().getPressure().getPd();
        String pdName = (String) paramMapping.get("DBP").getValue("Designation");
        tdc.getColumnModel().addColumn( pdName, DataType.Float );
        values.add( pd );
        knownVariables.put( pdName, pd );

        double hr = parameters.getPatientPhysiology().getEcg().getHr();
        String hrName = (String) paramMapping.get("Heart rate").getValue("Designation");
        tdc.getColumnModel().addColumn( hrName, DataType.Float );
        values.add( hr );
        knownVariables.put( hrName, hr );

        double co = parameters.getPatientPhysiology().getCalculatedParameters().getCo();
        if( Double.isNaN( co ) )
        {
            co = slowModel.getRole( EModel.class ).getVariable( "CO" ).getInitialValue();
        }
        tdc.getColumnModel().addColumn( "CO", DataType.Float );
        values.add( co );

        double ef = parameters.getPatientPhysiology().getHeartUltrasound().getEf();
        if( !Double.isNaN( ef ) )
        {
            tdc.getColumnModel().addColumn( "EF", DataType.Float );
            values.add( ef );
            knownVariables.put( "EF", ef );
        }

        double lvedv = parameters.getPatientPhysiology().getCalculatedParameters().getLvedv();
        if( !Double.isNaN( lvedv ) )
        {
        	String lvedvName = (String) paramMapping.get("LV EDV").getValue("Designation");
            knownVariables.put( lvedvName, lvedv );
        }

        double lvesv = parameters.getPatientPhysiology().getCalculatedParameters().getLvesv();
        if( !Double.isNaN( lvesv ) )
        {
        	String lvesvName = (String) paramMapping.get("LV ESV").getValue("Designation");
            knownVariables.put( lvesvName, lvesv );
        }

        TableDataCollectionUtils.addRow( tdc, "1", values.toArray() );
        return tdc;
    }

    private Optimization initSlowModelOptimization(DataCollection<?> output, Diagram diagram, TableDataCollection params,
            TableDataCollection constr, TableDataCollection exp) throws Exception
    {
        //Optimization document
        String optName = diagram.getName() + "_optimization";
        if( output.contains( optName ) )
            output.remove( optName );
        Optimization optimization = Optimization.createOptimization( optName, output, diagram );

        //Optimization method
        optimization.setOptimizationMethod( initOptimizationMethod( PopulationUtils.SLOW_MODEL ) );

        //Fitting parameters
        List<Parameter> pList = initParameterList( params, diagram );
        optimization.getParameters().setFittingParameters( pList );

        //Optimization experiments
        slowModelOptExp = initOptimizationExperiment( diagram, exp );
        optimization.getParameters().setOptimizationExperiments( Collections.singletonList( slowModelOptExp ) );

        //Optimization constraints
        DynamicProperty dp = diagram.getAttributes().getProperty( DiagramXmlConstants.SIMULATION_OPTIONS );
        if( dp != null && dp.getValue() instanceof SimulationEngine )
        {
            SimulationEngine se = (SimulationEngine)dp.getValue();
            se.setLogLevel( Level.SEVERE );
            double endTime = se.getCompletionTime();
            List<OptimizationConstraint> cList = initConstraintList( constr, diagram, endTime, endTime );
            optimization.getParameters().setOptimizationConstraints( cList );

            optimization.getParameters().getSimulationTaskParameters().get( slowModelOptExp.getName() ).setSimulationEngine(se);
        }
        return optimization;
    }

    private Optimization initFastModelOptimization(DataCollection<?> output, Diagram diagram, TableDataCollection params,
            TableDataCollection constr, TableDataCollection exp) throws Exception
    {
        //Optimization document
        String optName = diagram.getName() + "_optimization";
        if( output.contains( optName ) )
            output.remove( optName );
        Optimization optimization = Optimization.createOptimization( optName, output, diagram );

        //Optimization method
        optimization.setOptimizationMethod( initOptimizationMethod( PopulationUtils.FAST_MODEL ) );

        //Fitting parameters
        List<Parameter> pList = initParameterList( params, diagram );
        pList.add( initParameter( diagram, "V" ) );
        pList.add( initParameter( diagram, "AT1_ANGII" ) );
        if( !knownParameters.contains( "Hct" ) )
            pList.add( initParameter( diagram, "Hct" ) );
        optimization.getParameters().setFittingParameters( pList );

        //Optimization experiments
        fastModelOptExp = initOptimizationExperiment( diagram, exp );
        optimization.getParameters().setOptimizationExperiments( Collections.singletonList( fastModelOptExp ) );

        //Optimization constraints
        DynamicProperty dp = diagram.getAttributes().getProperty( DiagramXmlConstants.SIMULATION_OPTIONS );
        if( dp != null && dp.getValue() instanceof SimulationEngine )
        {
            SimulationEngine se = (SimulationEngine)dp.getValue();
            se.setLogLevel( Level.SEVERE );
            double endTime = se.getCompletionTime();
            double startTime = endTime - 5; //to include one complete breathing cycle
            List<OptimizationConstraint> cList = initConstraintList( constr, diagram, startTime, endTime );
            optimization.getParameters().setOptimizationConstraints( cList );

            optimization.getParameters().getSimulationTaskParameters().get( fastModelOptExp.getName() ).setSimulationEngine(se);
        }

        return optimization;
    }

    private OptimizationMethod<?> initOptimizationMethod(String model)
    {
        OptimizationMethod<?> method = OptimizationMethodRegistry.getOptimizationMethod( "Evolution strategy (SRES)" );
        initMethodParameters(method, model);
        return method;
    }

    private void initMethodParameters(OptimizationMethod<?> method, String model)
    {
        SRESOptMethodParameters methodParams = (SRESOptMethodParameters)method.getParameters();

        methodParams.setApplyState( false );

        if( model.equals( PopulationUtils.SLOW_MODEL ) || model.equals( PopulationUtils.SLOW_MODEL + "_ss" ) )
        {
            methodParams.setNumOfIterations( parameters.getOptimizationSettings().getSlowModelNumOfIterations() );
            methodParams.setSurvivalSize( parameters.getOptimizationSettings().getSlowModelSurvivalSize() );
        }
        if( model.equals( PopulationUtils.FAST_MODEL ) || model.equals( PopulationUtils.FAST_MODEL + "_ss" ) )
        {
            methodParams.setNumOfIterations( parameters.getOptimizationSettings().getFastModelNumOfIterations() );
            methodParams.setSurvivalSize( parameters.getOptimizationSettings().getFastModelSurvivalSize() );
        }
    }

    private List<Parameter> initParameterList(TableDataCollection params, Diagram diagram) throws Exception
    {
        List<Parameter> pList = new ArrayList<Parameter>();

        Iterator<RowDataElement> it = params.iterator();
        while( it.hasNext() )
        {
            RowDataElement var = it.next();
            if( knownParameters.contains( var.getName() ) || knownVariables.containsKey( var.getName() ) )
                continue;

            Bounds bounds = getBounds( var );
            if( !Double.isNaN( bounds.lower ) && !Double.isNaN( bounds.upper ) )
            {
                double value = (double)var.getValue( "value" );
                if( var.getName().equals( "TBW" ) )
                {
                    double bv = parameters.getPatientPhysiology().getCalculatedParameters().getBv();
                    value = calculateTBW(bv);
                }

                Parameter param = new Parameter( var.getName(), value, bounds.lower, bounds.upper );
                param.setParentDiagramName( diagram.getName() );
                pList.add( param );
            }
        }

        return pList;
    }

    /**
     * Moore F.D. Body composition and its measurement in vivo. British Journal of Surgery. 1967. 54(13):431–435.
     */
    private double calculateTBW(double bloodVolume)
    {
        return ( bloodVolume - 650 ) / 111.5;
    }

    private List<OptimizationConstraint> initConstraintList(TableDataCollection constraints, Diagram diagram, double startTime,
            double endTime) throws IllegalArgumentException, IllegalAccessException
    {
        List<OptimizationConstraint> cList = new ArrayList<OptimizationConstraint>();

        Iterator<RowDataElement> it = constraints.iterator();
        while( it.hasNext() )
        {
            RowDataElement row = it.next();

            if( knownVariables.containsKey( row.getValue( "expression" ) ) )
                continue;

            Bounds bounds = getBounds( row );

            if( !Double.isNaN( bounds.lower ) ) //lower bound constraint
            {
                OptimizationConstraint cmin = new OptimizationConstraint();
                cmin.setFormula( row.getValue( "expression" ) + ">" + bounds.lower );
                cmin.setInitialTime( startTime );
                cmin.setCompletionTime( endTime );
                cmin.setDiagram( diagram );
                cList.add( cmin );
            }

            if( !Double.isNaN( bounds.upper ) ) //upper bound constraint
            {
                OptimizationConstraint cmax = new OptimizationConstraint();
                cmax.setFormula( row.getValue( "expression" ) + "<" + bounds.upper );
                cmax.setInitialTime( startTime );
                cmax.setCompletionTime( endTime );
                cmax.setDiagram( diagram );
                cList.add( cmax );
            }
        }

        return cList;
    }

    private OptimizationExperiment initOptimizationExperiment(Diagram diagram, TableDataCollection exp)
    {
        OptimizationExperiment experiment = new OptimizationExperiment( "experiment" );

        experiment.setExperimentType( "Time course" );
        experiment.setWeightMethod( "Mean square" );
        experiment.setDiagram( diagram );
        experiment.setFilePath( DataElementPath.create( exp ) );
        experiment.setDiagramStateName( "no state" );

        List<ParameterConnection> conn = experiment.getParameterConnections();
        for( int i = 0; i < conn.size(); i++ )
            conn.get( i ).setNameInDiagram( conn.get( i ).getNameInFile() );

        return experiment;
    }

    private Bounds getBounds(RowDataElement var) throws IllegalArgumentException, IllegalAccessException
    {
        Bounds bounds = new Bounds();

        if( var.getValue( "expression" ) != null && var.getValue( "expression" ).equals( "V" ) )
        {
            double bv = parameters.getPatientPhysiology().getCalculatedParameters().getBv();
            bounds.lower = getBVLowerBound( bv );
            bounds.upper = getBVUpperBound( bv );
            return bounds;
        }
        if( var.getValue( "expression" ) != null && var.getValue( "expression" ).equals( "FRC" ) )
        {
        	calculateFRCBounds( bounds );
            return bounds;
        }
        if( var.getValue( "expression" ) != null && var.getValue( "expression" ).equals( "V_DEI-0.267*TV+1291/RR" ) )
        {
        	calculateDeadSpaceVolumeBounds(bounds);
            return bounds;
        }        
        if( var.getName().equals( "TBW" ) )
        {
            double bv = parameters.getPatientPhysiology().getCalculatedParameters().getBv();
            bounds.lower = calculateTBW( getBVLowerBound( bv ) );
            bounds.upper = calculateTBW( getBVUpperBound( bv ) );
            return bounds;
        }

        String sex = parameters.getPatientPhysiology().getGeneralData().sex.name();

        if( sex != null && var.getValue( "min_normal_" + sex ) != null )
            bounds.lower = (double)var.getValue( "min_normal_" + sex );
        else
            bounds.lower = (double)var.getValue( "min_normal" );
        if( sex != null && var.getValue( "max_normal_" + sex ) != null )
            bounds.upper = (double)var.getValue( "max_normal_" + sex );
        else
            bounds.upper = (double)var.getValue( "max_normal" );

        Field[] diseases = PatientPhysiology.Diseases.class.getDeclaredFields();
        for( Field disease : diseases )
        {
            if( disease.getType().equals( PatientPhysiology.Diagnosis.class ) )
            {
                boolean diseaseAccess = disease.isAccessible();
                disease.setAccessible( true );

                PatientPhysiology.Diagnosis diagnosis = (PatientPhysiology.Diagnosis)disease
                        .get( parameters.getPatientPhysiology().getDiseases() );

                Field type = null;
                boolean typeAcess = false;
                PatientPhysiology.Classification classification = null;

                try
                {
                    type = PatientPhysiology.Diseases.class.getDeclaredField( disease.getName() + "Type" );
                    typeAcess = type.isAccessible();
                    type.setAccessible( true );
                    classification = (PatientPhysiology.Classification)type.get( parameters.getPatientPhysiology().getDiseases() );
                }
                catch( NoSuchFieldException e )
                {
                }


                switch( diagnosis )
                {
                    case UNKNOWN:
                    {
                        if( classification != null )
                        {
                            for( String item : classification.getItems() )
                                refreshBounds( var, disease.getName() + "_" + item, bounds, false );
                        }
                        else
                            refreshBounds( var, disease.getName(), bounds, false );
                        break;
                    }
                    case YES:
                    {
                        if( classification != null )
                        {
                            String item = classification.getSelectedItem();
                            refreshBounds( var, disease.getName() + "_" + item, bounds, true );
                        }
                        else
                            refreshBounds( var, disease.getName(), bounds, true );
                        break;
                    }
                    default:
                        break;
                }

                disease.setAccessible( diseaseAccess );
                if( type != null )
                    type.setAccessible( typeAcess );
            }
        }

        return bounds;
    }

    private double getBVLowerBound(double normalBV)
    {
        return 0.9 * normalBV;
    }

    private double getBVUpperBound(double normalBV)
    {
        String chf = parameters.getPatientPhysiology().getDiseases().getChf();
        String type = parameters.getPatientPhysiology().getDiseases().getChfType();
        if( chf.equals( Diagnosis.YES.toString() )
                && ( type.equals( NYHA.CLASS_III.toString() ) || type.equals( NYHA.CLASS_IV.toString() ) ) )
            return 1.4 * normalBV;
        return 1.1 * normalBV;
    }

    /**
     * Quanjer P.H., et al. Lung volumes and forced ventilatory flows. Report Working Party Standardization of Lung Function Tests, European Community for Steel and Coal. Official Statement of the European Respiratory Society. Eur Respir J Suppl. 1993. 16:5-40.
     */
	private void calculateFRCBounds(Bounds bounds)
	{
		double h = parameters.getPatientPhysiology().getGeneralData().getHeight() / 100;
		double age = parameters.getPatientPhysiology().getGeneralData().getAge();

		double mean = Double.NaN;
		double sd = Double.NaN;

		switch (parameters.getPatientPhysiology().getGeneralData().sex) 
		{
		    case MAN:
			    mean = 1000 * (2.34 * h + 0.009 * age - 1.09);
			    sd = 990;
			    break;
		    case WOMAN:
			    mean = 1000 * (2.24 * h + 0.001 * age - 1.0);
			    sd = 820;
			    break;
		}
		bounds.lower = mean - sd;
		bounds.upper = mean + sd;
	}

    /**
     * Harris E.A., et al. Prediction of the physiological dead-space in resting normal subjects. Clin Sci Mol Med. 1973;45(3):375-386.
     */
	private void calculateDeadSpaceVolumeBounds(Bounds bounds)
	{
		double h = parameters.getPatientPhysiology().getGeneralData().getHeight();
		double age = parameters.getPatientPhysiology().getGeneralData().getAge();

		double mean = 0.93 * age + 1.725 * h - 213;
		double deviation = 30;

		bounds.lower = mean - deviation;
		bounds.upper = mean + deviation;
	}

    private Bounds refreshBounds(RowDataElement var, String diseaseName, Bounds bounds, boolean known)
    {
        Double min = (Double)var.getValue( "min_" + diseaseName );
        if( ! ( min == null ) && !min.isNaN())
        {
            if( bounds.minIsNormal )
            {
                bounds.minIsNormal = false;
                bounds.lower = min;

                if( known )
                    bounds.minIsKnown = true;
            }
            else if( known )
            {
                if( !bounds.minIsKnown )
                {
                    bounds.minIsKnown = true;
                    bounds.lower = min;
                }
                else if( min > bounds.lower )
                    bounds.lower = min;
            }
            else if( !bounds.minIsKnown && min < bounds.lower )
                bounds.lower = min;
        }

        Double max = (Double)var.getValue( "max_" + diseaseName );
        if( ! ( max == null ) && !max.isNaN())
        {
            if( bounds.maxIsNormal )
            {
                bounds.maxIsNormal = false;
                bounds.upper = max;
            }
            else if( max > bounds.upper )
                bounds.upper = max;
        }

        return bounds;
    }

    public static class Bounds
    {
        protected double lower;
        protected double upper;
        protected boolean minIsNormal = true;
        protected boolean maxIsNormal = true;
        protected boolean minIsKnown = false;
    }

    private Parameter initParameter(Diagram diagram, String pName) throws Exception
    {
        return initParameter( diagram, pName, null, null );
    }

    private Parameter initParameter(Diagram diagram, String pName, Double lowerBound, Double upperBound) throws Exception
    {
        EModel model = diagram.getRole( EModel.class );
        Variable variable = model.getVariables().get( pName );
        double val = variable.getInitialValue();

        double lower = val;
        if( lowerBound != null )
            lower = lowerBound;

        double upper = val;
        if( upperBound != null )
            upper = upperBound;

        Parameter param = new Parameter( pName, val, lower, upper );
        param.setParentDiagramName( diagram.getName() );
        return param;
    }

    private void initOptimizationProblem(Optimization optimization, OptimizationExperiment exp)
    {
        OptimizationMethod<?> method = optimization.getOptimizationMethod();
        OptimizationParameters optParams = optimization.getParameters();

        Diagram diagram = optParams.getDiagram();
        List<OptimizationConstraint> constraints = optParams.getOptimizationConstraints();
        List<Parameter> params = optParams.getFittingParameters();

        OptimizationConstraintCalculator calculator = new OptimizationConstraintCalculator();
        calculator.parseConstraints( constraints, diagram );

        SingleExperimentParameterEstimation problem = new SingleExperimentParameterEstimation(
                optParams.getSimulationTaskParameters().get( exp.getName() ), exp, params, constraints );

        problem.setCalculator( calculator );
        method.setOptimizationProblem( problem );
    }
}
