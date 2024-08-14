package biouml.plugins.pharm.analysis;

import java.util.stream.Stream;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.simulation.SimulationEngineWrapper;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;

public class PopulationSamplingParameters extends AbstractAnalysisParameters
{
    private DataElementPath path;
    private int populationSize = 200;
    private int acceptanceRate = 1;
    private Diagram diagram;
    private TableDataCollection experimentalData;
    private TableDataCollection initialData;
    private DataElementPath diagramPath;
    private DataElementPath experimentalDataPath;
    private DataElementPath initialDataPath;
    private VariableSet[] estimatedVariables = new VariableSet[0];
    private VariableSet[] observedVariables = new VariableSet[0];
    private int preliminarySteps = 20;
    private int chains = 1;
    private SimulationEngineWrapper engineWrapper;
    private int seed = 0;
    
    private int validationSize = 10;
    private double startSearchTime = 100;
    private double atol = 1E-3;
    
    public PopulationSamplingParameters()
    {
        setEngineWrapper(new SimulationEngineWrapper());
    }
    
    public Stream<String> getAvailableVariables()
    {
        return diagram == null ? StreamEx.of( new String[0] )
                : diagram.getRole( EModel.class ).getVariables().stream().map( v -> v.getName() ).filter( n -> ! ( n.equals( "time" ) ) );
    }

    @PropertyName ( "Result path" )
    @PropertyDescription ( "Result Path." )
    public DataElementPath getResultPath()
    {
        return path;
    }
    public void setResultPath(DataElementPath path)
    {
        this.path = path;
    }
    
    @PropertyName ( "Population size per chain" )
    @PropertyDescription ( "Population size per chain." )
    public int getPopulationSize()
    {
        return populationSize;
    }
    public void setPopulationSize(int sampleSize)
    {
        this.populationSize = sampleSize;
    }

    @PropertyName ( "Diagram" )
    @PropertyDescription ( "Diagram." )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }
    public void setDiagramPath(DataElementPath diagramPath)
    {
        diagram = diagramPath.optDataElement( Diagram.class );
        if( diagram != null )
        {
            this.diagramPath = diagramPath;
            this.engineWrapper.setDiagram(diagram);
        }
    }
    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
        this.engineWrapper.setDiagram(diagram);
        for (VariableSet var: estimatedVariables)
            var.setDiagram(diagram);
        for (VariableSet var: observedVariables)
            var.setDiagram(diagram);
    }
    public Diagram getDiagram()
    {
        return diagram;
    }

    @PropertyName("Experimental data")
    public DataElementPath getExperimentalDataPath()
    {
        return experimentalDataPath;
    }
    public void setExperimentalDataPath(DataElementPath experimentalDataPath)
    {
        this.experimentalDataPath = experimentalDataPath;
        this.experimentalData = experimentalDataPath.getDataElement(TableDataCollection.class);
    }
    public TableDataCollection getExperimentalData()
    {
        return experimentalData;
    }
    public void setExperimentalData(TableDataCollection table)
    {
        this.experimentalData = table;
    }

    @PropertyName("Observed variables")
    public VariableSet[] getObservedVariables()
    {
        return observedVariables;
    }

    public void setObservedVariables(VariableSet... observedVariables)
    {
        this.observedVariables = observedVariables;
        for (VariableSet var: observedVariables)
        {
            var.setEngine(getEngineWrapper().getEngine());
            var.setDiagram(this.getDiagram());
        }
    }

    @PropertyName("Variables to estimate")
    public VariableSet[] getEstimatedVariables()
    {
        return estimatedVariables;
    }

    public void setEstimatedVariables(VariableSet... estimatedVariables)
    {
        this.estimatedVariables = estimatedVariables;
        for (VariableSet var: observedVariables)
        {
            var.setEngine(getEngineWrapper().getEngine());
            var.setDiagram(this.getDiagram());
        }
    }
    public boolean isDiagramNotSet()
    {
        return diagram == null;
    }

    @PropertyName("Initial values")
    @PropertyDescription("Mean and variance values for estimated variables.")
    public DataElementPath getInitialDataPath()
    {
        return initialDataPath;
    }
    public void setInitialDataPath(DataElementPath initialDataPath)
    {
        this.initialData = initialDataPath.getDataElement(TableDataCollection.class);
        this.initialDataPath = initialDataPath;
    }
    public TableDataCollection getInitialData()
    {
        return initialData;
    }
    public void setInitialData(TableDataCollection table)
    {
        this.initialData = table;
    }

    @PropertyName("Preliminary steps")
    public int getPreliminarySteps()
    {
        return preliminarySteps;
    }

    public void setPreliminarySteps(int preliminarySteps)
    {
        this.preliminarySteps = preliminarySteps;
    }
    
    @PropertyName("Acceptance rate")
    public int getAcceptanceRate()
    {
        return acceptanceRate;
    }
    public void setAcceptanceRate(int acceptanceRate)
    {
        this.acceptanceRate = acceptanceRate;
    }

    @PropertyName("Number of chains")
    public int getChains()
    {
        return chains;
    }
    public void setChains(int chains)
    {
        this.chains = chains;
    }

    @PropertyName("Simulation options")
    public SimulationEngineWrapper getEngineWrapper()
    {
        return engineWrapper;
    }
    public void setEngineWrapper(SimulationEngineWrapper engineWrapper)
    {
        Object oldValue = this.engineWrapper;
        this.engineWrapper = engineWrapper;
        this.engineWrapper.setParent(this, "engineWrapper");
        firePropertyChange("engineWrapper", oldValue, this.engineWrapper);
    }

    public int getValidationSize()
    {
        return validationSize;
    }
    public void setValidationSize(int validationSize)
    {
        this.validationSize = validationSize;
    }

    public double getStartSearchTime()
    {
        return startSearchTime;
    }
    public void setStartSearchTime(double startSearchTime)
    {
        this.startSearchTime = startSearchTime;
    }

    public double getAtol()
    {
        return atol;
    }
    public void setAtol(double atol)
    {
        this.atol = atol;
    }

    @PropertyName("Random seed")
    @PropertyDescription("Seed for random algorithms. 0 means seed defined by time (recommended)")
    public int getSeed()
    {
        return seed;
    }
    public void setSeed(int seed)
    {
        this.seed = seed;
    }
}
