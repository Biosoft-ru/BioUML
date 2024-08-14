package biouml.plugins.physicell.ode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.TransitionProperties;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.plugins.simulation.InfiniteSpan;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.SimulationEngine;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;

public class IntracellularProperties extends Option
{
    private DataElementPath diagramPath = null;
    private Diagram diagram;
    private SimulationEngineWrapper engine;// = new SimulationEngineWrapper();
    private PhenotypeVariable[] variables = new PhenotypeVariable[0];
    private String[] densities = new String[0];
    private CellDefinitionProperties cdp;
    private DiagramElement de;
    private String[] phenotypeNames = new String[0];
    private String[] variableNames = new String[0];

    /**
     * Creates dictionary which maps Human readable name to phenotype property code
     * It depends on substrates in microenvironment and cell definition properties
     */
    public static Map<String, String> createDictionary(String[] densities, CellDefinitionProperties cdp)
    {
        Map<String, String> dictionary = new TreeMap<>();
        dictionary.put( "Migration speed", "mms" );
        dictionary.put( "Migration persistence time", "mpt" );
        dictionary.put( "Migration bias", "mmb" );

        dictionary.put( "Rate: Apoptosis", "da" );
        dictionary.put( "Rate: Necrosis", "dn" );

        for( String density : densities )
        {
            dictionary.put( density + " intracellular", density );
            dictionary.put( density + " uptake rate", "sur_" + density );
            dictionary.put( density + " secretion rate", "ssr_" + density );
            dictionary.put( density + " saturation density", "ssd_" + density );
            dictionary.put( density + " export rate", "ser_" + density );
        }

        dictionary.put( "Target Solid Cytoplasmic Volume", "vtsc" );
        dictionary.put( "Target Solid Nuclear Volume", "vtsn" );
        dictionary.put( "Target Fluid Fraction Volume", "vff" );

        if( cdp != null )
        {
            for( TransitionProperties tp : cdp.getCycleProperties().getTransitions() )
            {
                if( tp.getFrom() != null && tp.getTo() != null )
                {
                    int fromIndex = cdp.getCycleProperties().findPhaseIndex( tp.getFrom() );
                    int toIndex = cdp.getCycleProperties().findPhaseIndex( tp.getTo() );
                    dictionary.put( "Rate: " + tp.getFrom() + " -> " + tp.getTo(), "ctr_" + fromIndex + "_" + toIndex );
                }
            }
        }
        return dictionary;
    }

    public IntracellularProperties()
    {
        engine = new SimulationEngineWrapper();
    }

    public IntracellularProperties(IntracellularODEBioUML intracellular, Node node)
    {
        setDiagramElement( node );
        engine = new SimulationEngineWrapper();
        diagram = intracellular.getDiagram();
        diagramPath = diagram.getCompletePath();
        engine.setDiagram( diagram );

        MulticellEModel emodel = Diagram.getDiagram( node ).getRole( MulticellEModel.class );
        String[] substrates = emodel.getSubstrates().stream().map( s -> s.getName() ).toArray( String[]::new );
        Map<String, String> dict = createDictionary( substrates, cdp );
        dict = dict.entrySet().stream().collect( Collectors.toMap( Entry::getValue, Entry::getKey, (oldValue, newValue) -> oldValue ) );
        List<PhenotypeVariable> variableLists = new ArrayList<>();
        for( Entry<String, String> entry : intracellular.getPhenotypeMapping().entrySet() )
        {
            PhenotypeVariable variable = new PhenotypeVariable();
            String phenotypeCode = entry.getKey();
            String phenotypeName = dict.get( phenotypeCode );
            variable.setPhenotypeName(phenotypeName);
            String convertedName = convertVariableName(entry.getValue(), diagram);
            variable.setVarName(convertedName);
            variableLists.add( variable );
        }
        this.variables = variableLists.toArray( new PhenotypeVariable[variableLists.size()] );
    }
    
    private String convertVariableName(String name, Diagram diagram)
    {
        Node node = diagram.findNode(name);
        if (node != null && node.getRole() instanceof VariableRole)
            return node.getRole(VariableRole.class).getName();
        return name;
    }

    public Diagram readSBML(String path, DataCollection dc) throws Exception
    {
        File f = new File( path );
        Diagram diagram = SbmlModelFactory.readDiagram( f, dc, f.getName(), null );
        return SBGNConverterNew.convert( diagram );
    }

    public void createIntracellular(CellDefinition cd, Model model) throws Exception
    {
        if( diagram == null )
            return;
        SimulationEngine eng = engine.getEngine();
        eng.setDiagram( diagram );
        biouml.plugins.simulation.Model odeModel = eng.createModel();
        odeModel.init();
        Map<String, String> dictionary = createDictionary( densities, cdp );
        IntracellularODEBioUML intracellular = new IntracellularODEBioUML( model, cd );
        if( eng instanceof OdeSimulationEngine )
            intracellular.setVarIndexes( ( (OdeSimulationEngine)eng ).getVarIndexMapping());//Va.getShortNameMapping() );
        else
            throw new Exception( "Only ODE Simualtion engine is supported for Physicell model" );

        List<String> inputs = new ArrayList<>();
        List<String> outputs = new ArrayList<>();
        for( PhenotypeVariable variable : variables )
        {
            String code = dictionary.get( variable.getPhenotypeName() );
            if( variable.getType().equals( PhenotypeVariable.INPUT_TYPE ) )
                inputs.add( code );
            else if( variable.getType().equals( PhenotypeVariable.INPUT_TYPE ) )
                outputs.add( code );
            else
            {
                inputs.add( code );
                outputs.add( code );
            }

            intracellular.addPhenotypeSpecies( code, variable.getVarName() );
        }
        intracellular.setInputs( inputs.toArray( new String[inputs.size()] ) );
        intracellular.setOutputs( outputs.toArray( new String[outputs.size()] ) );
        intracellular.setDT( engine.getEngine().getTimeIncrement() );
        intracellular.setModel( odeModel );
        InfiniteSpan span = new InfiniteSpan( engine.getEngine().getTimeIncrement() );
        eng.getSimulator().init( odeModel, odeModel.getInitialValues(), span, null, null );
        intracellular.setSolver( eng.getSimulator() );
        cd.phenotype.intracellular = intracellular;
    }

    @PropertyName ( "Diagram" )
    public DataElementPath getDiagram()
    {
        return diagramPath;
    }

    public void setDiagram(DataElementPath diagramPath)
    {
        Object oldValue = this.diagramPath;
        this.diagramPath = diagramPath;
        if( diagramPath == null || !diagramPath.exists() )
        {
            diagram = null;
            return;
        }
        this.diagram = diagramPath.getDataElement( Diagram.class );
        updateVariableNames();
        this.engine.setDiagram( diagram );
        firePropertyChange( "diagram", oldValue, diagram );
        firePropertyChange( "*", null, null );
    }

    public void setEngine(SimulationEngineWrapper engine)
    {
        this.engine = engine;
    }
    public SimulationEngineWrapper getEngine()
    {
        return engine;
    }

    @PropertyName ( "Variables" )
    public PhenotypeVariable[] getVariables()
    {
        return variables;
    }
    public void setVariables(PhenotypeVariable[] variables)
    {
        this.variables = variables;
        update();
    }

    public void update()
    {
        updateVariableNames();
        updatePhenotypeNames();
    }

    public void setDiagramElement(DiagramElement de)
    {
        if( de == null )
            return;
        this.de = de;
        if( de.getRole() instanceof CellDefinitionProperties )
            cdp = de.getRole( CellDefinitionProperties.class );
        updatePhenotypeNames();
    }

    private void updateVariableNames()
    {
        if( diagram == null )
        {
            variableNames = new String[0];
            return;
        }
        List<String> list = diagram.getRole( EModel.class ).getVariables().getNameList();
        variableNames = list.toArray( new String[list.size()] );
        for( PhenotypeVariable v : variables )
            v.setVariableNames( variableNames );
    }

    private void updatePhenotypeNames()
    {
        if( de != null )
        {
            Diagram d = Diagram.getDiagram( de );
            densities = d.getRole( MulticellEModel.class ).getSubstrates().stream().map( s -> s.getName() ).toArray( String[]::new );
        }
        Set<String> set = createDictionary( densities, cdp ).keySet();
        this.phenotypeNames = set.toArray( new String[set.size()] );
        for( PhenotypeVariable v : variables )
            v.setPhenotypeNames( phenotypeNames );
    }

    public boolean isDiagramNull()
    {
        return diagram == null;
    }

    public String getPhenotypeVariableTitle(Integer i, Object obj)
    {
        PhenotypeVariable pv = ( (PhenotypeVariable)obj );
        String varName = pv.getVarName();
        if( varName == null )
            varName = "";
        String phenotypeVar = pv.getPhenotypeName();
        if( phenotypeVar == null )
            phenotypeVar = "";
        String type = pv.getType();
        if( type.equals( PhenotypeVariable.INPUT_TYPE ) )
        {
            return "ODE [" + varName + "] <-- [" + phenotypeVar + "]";
        }
        else if( type.equals( PhenotypeVariable.OUTPUT_TYPE ) )
        {
            return "ODE [" + varName + "] --> [" + phenotypeVar + "]";
        }
        else
        {
            return "ODE [" + varName + "] <--> [" + phenotypeVar + "]";
        }
    }

    public IntracellularProperties clone(DiagramElement de)
    {
        IntracellularProperties result = new IntracellularProperties();
        result.densities = densities.clone();
        result.diagramPath = diagramPath;
        result.diagram = diagram;
        result.engine = engine;
        result.variables = variables.clone();
        result.cdp = cdp;
        result.phenotypeNames = phenotypeNames.clone();
        result.variableNames = variableNames.clone();
        result.de = de;
        return result;
    }
}