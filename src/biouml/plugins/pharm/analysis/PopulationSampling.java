package biouml.plugins.pharm.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import java.util.logging.Logger;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.simulation.SimulationEngine;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class PopulationSampling extends AnalysisMethodSupport<PopulationSamplingParameters>
{
    protected static final Logger log = Logger.getLogger(PopulationSampling.class.getName());
    private static final String[] columnsLong = {"Mean", "Variance", "Min", "Max"};
    private static final String[] columnsShort = {"Mean", "Variance"};

    private double[] exactValues;
    private double[] error;
    
    private boolean useExperimentalTable = true; //by default
    private Normal[] normal;
    private String[] estimatedVariables;
    private String[] observedVariables;
    private double[][] experimentalData;
    private Uniform uniform;
    private List<Patient> acceptedPatients;
    private PatientCalculator calculator = null;
    private boolean debug = false;
    private boolean saveHistory = false;
    private PatientProcessor processor;
    private int seed;
    
    // statistics
    private int patientsTotal;
    private int maxPerChain;
    private int patientsAccepted;
    private int acceptedMax;
    private List<String> history;
    
    double[] patientValues;
    double[] minValues;
    double[] maxValues;
    
    public PopulationSampling(DataCollection<?> origin, String name)
    {
        super(origin, name, new PopulationSamplingParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<Patient> result = justAnalyze();
        TableDataCollection resultTable = TableDataCollectionUtils.createTableDataCollection(parameters.getResultPath());
        int i = 0;
        for( Patient patient : result )
            TableDataCollectionUtils.addRow(resultTable, String.valueOf(i++), patient.getValues());

        resultTable.getOrigin().put(resultTable);
        return resultTable;
    }
   
    private void init() throws Exception
    {
        if( useExperimentalTable )
        {
            TableDataCollection experimentalTable = parameters.getExperimentalData();
            experimentalData = TableDataCollectionUtils.getMatrix(experimentalTable);
        }

        estimatedVariables = VariableSet.getVariablePaths(parameters.getEstimatedVariables());
        observedVariables = VariableSet.getVariablePaths(parameters.getObservedVariables());
        history = new ArrayList<>();
        acceptedPatients = new ArrayList<>();
        patientsAccepted = 0;
        patientsTotal = 0;
        maxPerChain = parameters.getPreliminarySteps()+parameters.getPopulationSize()*parameters.getAcceptanceRate();
        acceptedMax = parameters.getChains() * maxPerChain;
        seed = parameters.getSeed();
        
        if( calculator == null )
        {
            SimulationEngine engine = getParameters().getEngineWrapper().getEngine();
            calculator = new SteadyStateCalculator2(engine, estimatedVariables, observedVariables);
            ( (SteadyStateCalculator2)calculator ).setAtol(parameters.getAtol());
            ( (SteadyStateCalculator2)calculator ).setStartSearchTime(parameters.getStartSearchTime());
            ( (SteadyStateCalculator2)calculator ).setValidationSize(parameters.getValidationSize());
        }
        loadInitialData();
    }
    
    public List<Patient> justAnalyze() throws Exception
    {
        init();
        for( int chain = 0; chain < parameters.getChains(); chain++ ) //TODO: parallelize
        {
            MersenneTwister twister = seed == 0? new MersenneTwister(new Date()): new MersenneTwister(seed/2);
            uniform = new Uniform(0, 1, twister);
            List<Patient> chainPatients = new ArrayList<>();
            Patient currentPatient = createPatient(patientValues);
            while( chainPatients.size() < maxPerChain )
            {
                Patient nextPatient = generateNextPatient(currentPatient, minValues, maxValues);
                if( acceptPatient(nextPatient, currentPatient, chainPatients) )
                    currentPatient = nextPatient;
            }
            acceptedPatients.addAll(chainPatients);
        }
        filterResult();

        if( debug )
            System.out.println("Acceptance rate: " + getAcceptanceRate());
        return acceptedPatients;
    }
    
    private void loadInitialData()
    {
        TableDataCollection initialDataTable = parameters.getInitialData();
        Set<String> columns = StreamEx.of(TableDataCollectionUtils.getColumnNames(initialDataTable)).toSet();
        String[] usedColumns = columns.containsAll(Arrays.asList("Min", "Max")) ? columnsLong : columnsShort;
        int[] indices = TableDataCollectionUtils.getColumnIndexes(initialDataTable, usedColumns);
        MersenneTwister twister = seed == 0? new MersenneTwister(new Date()): new MersenneTwister(seed);
        minValues = new double[estimatedVariables.length];
        maxValues = new double[estimatedVariables.length];
        patientValues = new double[estimatedVariables.length];
        normal = new Normal[estimatedVariables.length];
        
        for( int i = 0; i < estimatedVariables.length; i++ )
        {
            String variable = estimatedVariables[i];
            double[] values = TableDataCollectionUtils.getDoubleRow(initialDataTable, variable);
            patientValues[i] = values[indices[0]];
            double variance = values[indices[1]];
            minValues[i] = indices.length > 2? values[indices[2]]: Double.MIN_VALUE;
            maxValues[i] = indices.length > 3? values[indices[3]]: Double.MAX_VALUE;
            normal[i] = new Normal(0, variance, twister);// new
                                                         // MersenneTwister(new
                                                         // Date()));
        }
    }

    private boolean acceptPatient(Patient newPatient, Patient currentPatient, List<Patient> acceptedPatients)
    {
        if( newPatient.invalid )
            return false;
        boolean accepted = newPatient.likelihood > currentPatient.likelihood * uniform.nextDouble();
        if( accepted )
        {
            acceptedPatients.add(newPatient);
            patientsAccepted++;

            if( processor != null )
                processor.process(newPatient);
        }

        if( saveHistory )
            history.add(generateInfo(newPatient, accepted));
        if( debug )
        {
            System.out.println(generateInfo(newPatient, accepted));
            if( accepted )
                System.out.println(patientsAccepted + " / " + acceptedMax);
            System.out.println(" ");
        }
        return accepted;
    }
    
    private String generateInfo(Patient patient, boolean accepted)
    {
        return "Patient was " + ( accepted ? "ACCEPTED" : "REJECTED" ) + " (" + patient.likelihood + "):" + patient.toString();
    }

    private Patient generateNextPatient(Patient currentPatient, double[] minValues, double[] maxValues) throws Exception
    {
        double[] currentParameters = currentPatient.input;
        double[] nextParameters = new double[currentParameters.length];
        for( int i = 0; i < currentParameters.length; i++ )
        {
            nextParameters[i] = currentParameters[i] + normal[i].nextDouble();
            if( nextParameters[i] < minValues[i] || nextParameters[i] > maxValues[i] )
                return new Patient(nextParameters, new double[0], true);
        }
        return createPatient(nextParameters);
    }

    private Patient createPatient(double[] input) throws Exception
    {
        Patient patient = calculator.calculate(input);
        patientsTotal++;
        
        if( exactValues != null && error != null && !isTolerable( patient, exactValues, error ) )
        {
        	 patient.invalidate();
        	 return patient;
         }

        patient.likelihood = useExperimentalTable
                ? Util.estimateDensityNormal(patient.getObserved(), experimentalData, Util.calculateKDEBandwidth(experimentalData))
                : distribution.density(patient.getObserved());
       
        return patient;
    }

	private boolean isTolerable(Patient patient, double[] exact, double[] tolerance)
	{
		for (int i = 0; i < exact.length; i++)
		{
			if (Math.abs(patient.getObserved()[i] - exact[i]) >= tolerance[i])
			{
				System.out.println("Parameter "+ observedVariables[i]+" out of bounds: "+ patient.getObserved()[i]);
				return false;
			}
		}
		return true;
	}
	
	private boolean isTolerable(Patient patient, int[] indices, double[] min, double[] max)
	{
		double[] values = patient.getAllValues();
		for (int i=0; i<indices.length; i++)
		{
			if ( min[i] > values[indices[i]] || max[i] < values[indices[i]])
				return false;
		}
		return true;
	}

	public void setAcceptedError(double[] exactValues, double[] error)
    {
		this.exactValues = exactValues;
		this.error = error;
    }
	
    public void setObservedDistribution(double[] mean, double[][] sd)
    {
        this.useExperimentalTable = false;
        this.distribution = new MultivariateNormalDistribution(mean, sd);
    }

    MultivariateNormalDistribution distribution = null;

    private void filterResult()
    {
        acceptedPatients = StreamEx.of(acceptedPatients).skip(parameters.getPreliminarySteps()).toList();
        acceptedPatients = IntStreamEx.range(0, acceptedPatients.size(), parameters.getAcceptanceRate()).elements(acceptedPatients)
                .toList();
    }

    public double getAcceptanceRate()
    {
        return this.patientsAccepted / (double)this.patientsTotal;
    }

    public List<String> getHistory()
    {
        return history;
    }

    public void setCalculator(PatientCalculator calculator)
    {
        this.calculator = calculator;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void setSaveHistory(boolean saveHistory)
    {
        this.saveHistory = saveHistory;
    }

    public void setPatientProcessor(PatientProcessor processor)
    {
        this.processor = processor;
    }
    public interface PatientProcessor
    {
        void process(Patient patient);
    }

    public void setUseExperimentalTable(boolean useExperimentalTable)
    {
        this.useExperimentalTable = useExperimentalTable;
    }
}
