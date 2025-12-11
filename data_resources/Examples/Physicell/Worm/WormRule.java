import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.CustomCellRule;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.standard.Chemotaxis;
import ru.biosoft.physicell.core.CellFunctions.UpdateMigrationBias;
import ru.biosoft.physicell.biofvm.VectorUtil;

public class WormRule extends CustomCellRule
{
    private Model model;
    private boolean isInit = false;

    public void init(Model model)
    {
        this.model = model;
        isInit = true;
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        if (!isInit)
            init(pCell.getModel());

        // bookkeeping 
        Microenvironment m = pCell.getMicroenvironment();
        int nSignal = m.findDensityIndex( "signal" );

        // look for cells to form attachments, if 0 attachments
        int number_of_attachments = pCell.state.numberAttachedCells();
        if( number_of_attachments == 0 )
        {
            for( Cell neighbor : pCell.nearby_interacting_cells() )
            {
                if( neighbor.state.numberAttachedCells() < neighbor.customData.get( "max_attachments" ) )
                {
                    Cell.attachcCells( neighbor, pCell );
                    number_of_attachments++;
                }
                if( number_of_attachments > pCell.customData.get( "max_attachments" ) )
                    break;
            }
        }

        if( number_of_attachments == 0 )
        {
            pCell.functions.updateMigration = new Chemotaxis();
        }
        else if( number_of_attachments == 1 ) // if 1 attachment, do some logic  
        {
            // constant expression in end cells 
            pCell.customData.set( "head", pCell.customData.get( "head_initial" ) );

            // am I the head? 
            boolean head = false;
            if( pCell.customData.get( "head" ) > pCell.state.attachedCells.iterator().next().customData.get( "head" ) )
                head = true;

            //            if( head )
            pCell.functions.updateMigration = head ? new HeadMigration( model ) : new TailMigration( model );
            //            else
            //                pCell.functions.updateMigration = new TailMigration( model );

            phenotype.secretion.secretionRates[nSignal] = 100;
        }
        else if( number_of_attachments > 1 ) // if 2 or more attachments, use middle 
        {
            pCell.functions.updateMigration = new MiddleMigration( model );
            phenotype.secretion.secretionRates[nSignal] = 1;
        }
    }

public class HeadMigration extends Chemotaxis
{
    private int direction;
    private double speed;
    private double bias;
    private double persistenceTime;

    public HeadMigration(Model model)
    {
        direction = model.getParameterInt( "head_migration_direction" );
        speed = model.getParameterDouble( "head_migration_speed" );
        bias = model.getParameterDouble( "head_migration_bias" );
        persistenceTime = model.getParameterDouble( "head_migration_persistence" );
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        phenotype.motility.chemotaxisDirection = direction;
        phenotype.motility.migrationSpeed = speed;
        phenotype.motility.migrationBias = bias;
        phenotype.motility.persistenceTime = persistenceTime;
        // use this for fun rotational paths 
        /*
        double r = norm( pCell.position ) + 1e-16; 
        phenotype.motility.migration_bias_direction[0] = - pCell.position[1] / r; 
        phenotype.motility.migration_bias_direction[1] = pCell.position[0] / r; 
        
        normalize( &(phenotype.motility.migration_bias_direction) ); 
        return; 
        */
        super.execute( pCell, phenotype, dt );
    }
}

public class MiddleMigration extends UpdateMigrationBias
{
    double speed;

    public MiddleMigration(Model model)
    {
        speed = model.getParameterDouble( "middle_migration_speed" );
    }

    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        // get velocity from "Upstream" 
        Cell headCell = null;
        for( Cell cell : pCell.state.attachedCells )
        {
            if( headCell == null || cell.customData.get( "head" ) > headCell.customData.get( "head" ) )
                headCell = cell;
        }
        phenotype.motility.migrationSpeed = speed;
        phenotype.motility.migrationBiasDirection = headCell.phenotype.motility.migrationBiasDirection;
        VectorUtil.normalize( phenotype.motility.migrationBiasDirection );
    }
}

public class TailMigration extends Chemotaxis
{
    private int direction;
    private double speed;
    private double bias;
    private double persistenceTime;

    public TailMigration(Model model)
    {
        direction = model.getParameterInt( "tail_migration_direction" );
        speed = model.getParameterDouble( "tail_migration_speed" );
        bias = model.getParameterDouble( "tail_migration_bias" );
        persistenceTime = model.getParameterDouble( "tail_migration_persistence" );
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        phenotype.motility.chemotaxisDirection = direction;
        phenotype.motility.migrationSpeed = speed;
        phenotype.motility.migrationBias = bias;
        phenotype.motility.persistenceTime = persistenceTime;
        super.execute( pCell, phenotype, dt );
    }
}

}