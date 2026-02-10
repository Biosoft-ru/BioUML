import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Model.Event;
import ru.biosoft.physicell.core.RandomGenerator;

public class TherapyEvent extends Event
{

    public TherapyEvent(Model model)
    {
        super( model );
        executionTime = model.getParameterDouble( "therapy_activation_time" );
    }

    @Override
    public void execute() throws Exception
    {
        //        System.out.println( "Therapy started!" );
        model.setSaveFullInterval( model.getParameterDouble( "save_interval_after_therapy_start" ) ); // 3.0; 
        introduceBiorobots( model );
    }

    public void introduceBiorobots(Model model) throws Exception
    {
        Microenvironment m = model.getMicroenvironment();
        // idea: we'll "inject" them in a little column
        double workerFraction = model.getParameterDouble( "worker_fraction" ); // 0.10; /* param */
        int numberInjectedCells = model.getParameterInt( "number_of_injected_cells" ); // 500; /* param */

        // make these vary with domain size
        double left = m.options.X_range[1] - 150.0; // 600.0;
        double right = m.options.X_range[1] - 50.0; // 700.0;
        double bottom = m.options.Y_range[0] + 50.0; // -700;
        double top = m.options.Y_range[1] - 50.0; // 700;

        CellDefinition workerCD = model.getCellDefinition( "worker cell" );
        CellDefinition cargoCD = model.getCellDefinition( "cargo cell" );
        RandomGenerator rng = model.getRNG();
        for( int i = 0; i < numberInjectedCells; i++ )
        {
            double[] position = {0, 0, 0};
            position[0] = rng.UniformRandom( left, right );
            position[1] = rng.UniformRandom( bottom, top );

            if( model.getRNG().UniformRandom() <= workerFraction )
            {
                Cell.createCell( workerCD, model, position );
            }
            else
            {
                Cell.createCell( cargoCD, model, position );
            }
        }
    }
}