package biouml.plugins.physicell;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.ode.PhenotypeVariable;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import one.util.streamex.StreamEx;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;

/**
 * Class to be used in modelSummary.vm
 * Method generateRport should be not static in order to work inside Velocity
 */
public class SimulationEngineHelper
{

    public MulticellEModel getModel(DiagramElement de)
    {
        Diagram diagram = Diagram.getDiagram( de );
        return diagram.getRole( MulticellEModel.class );
    }

    public String generateReport(DiagramElement de)
    {
        try
        {
            Model model = generateModel( de );

            if( de instanceof Diagram )
            {
                return model.display();
            }
            PhysicellRole role = de.getRole( PhysicellRole.class );

            if( role instanceof CellDefinitionProperties )
            {
                CellDefinitionProperties cdProperties = (CellDefinitionProperties)role;
                String name = cdProperties.getName();
                CellDefinition cd = model.getCellDefinition( name );
                return cd.display();
            }
        }
        catch( Exception e )
        {
            return "Could not generate summary: " + e.getMessage();
        }
        return "Select appropriate diagram element";
    }

    public boolean isDiagram(DiagramElement de)
    {
        return de instanceof Diagram;
    }

    public SubstrateProperties getSubtrateProperties()
    {
        return null;
    }

    public boolean isCellDefinition(DiagramElement de)
    {
        return de.getRole() instanceof CellDefinitionProperties;
    }

    public boolean isSubstrate(DiagramElement de)
    {
        return de.getRole() instanceof SubstrateProperties;
    }
    
    public boolean isSecretion(DiagramElement de)
    {
        return de.getRole() instanceof SecretionProperties;
    }
    
    public boolean isChemotaxis(DiagramElement de)
    {
        return de.getRole() instanceof ChemotaxisProperties;
    }
    
    public boolean isInteraction(DiagramElement de)
    {
        return de.getRole() instanceof InteractionProperties;
    }
    
    public boolean isTransformation(DiagramElement de)
    {
        return de.getRole() instanceof TransformationProperties;
    }

    public boolean isDivisionPhase(DiagramElement de, String name)
    {
        CellDefinitionProperties cellDefinitionProperties = de.getRole( CellDefinitionProperties.class );
        for( PhaseProperties phase : cellDefinitionProperties.getCycleProperties().getPhases() )
        {
            if( name.equals( phase.getName() ) )
                return phase.isDivisionAtExit();
        }
        return false;
    }

    public boolean isRemovePhase(DiagramElement de, String deathName, String name)
    {
        CellDefinitionProperties cellDefinitionProperties = de.getRole( CellDefinitionProperties.class );

        DeathModelProperties deathModel = StreamEx.of( cellDefinitionProperties.getDeathProperties().getDeathModels() )
                .findAny( m -> m.getCycleProperties().getCycleName().equals( deathName ) ).orElse( null );

        if (deathModel == null)
            return false;
        
        for( PhaseProperties phase : deathModel.getCycleProperties().getPhases())
        {
            if( name.equals( phase.getName() ) )
                return phase.isRemovalAtExit();
        }
        return false;
    }

    public String display(PhenotypeVariable pv)
    {
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

    public Object getObject(DiagramElement de)
    {
        try
        {
            if( de instanceof Diagram )
            {
                return generateModel( (Diagram)de );
            }
            return de.getRole();

        }
        catch( Exception ex )
        {

        }
        return null;
    }

    public static Model generateModel(DiagramElement de) throws Exception
    {
        Diagram d = Diagram.getDiagram( de );
        SimulationEngine engine = DiagramUtility.getPreferredEngine( d );
        if( ! ( engine instanceof PhysicellSimulationEngine ) )
        {
            engine = new PhysicellSimulationEngine();
        }
        engine.setDiagram( d );
        Model model = ( (PhysicellSimulationEngine)engine ).createCoreModel();

        return model;
    }

    public static PhysicellOptions getOptions(Diagram diagram)
    {
        SimulationEngine engine = DiagramUtility.getEngine( diagram );
        if( engine instanceof PhysicellSimulationEngine )
        {
            return (PhysicellOptions) ( (PhysicellSimulationEngine)engine ).getSimulatorOptions();
        }
        return null;
    }
}
