import java.util.Set;

import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Model.Event;
import ru.biosoft.physicell.core.RandomGenerator;

public class ImmunityEvent extends Event
{
   
    public ImmunityEvent(Model model)
    {
        super( model );
        executionTime = model.getParameterDouble( "immune_activation_time" );
    }

    @Override
    public void execute() throws Exception
    {
        //        System.out.println( "Therapy started!" );
        model.setSaveFullInterval( model.getParameterDouble( "save_interval_after_therapy_start" ) ); // 3.0; 
        introduceImmuneCells( model );
    }
    
    public void introduceImmuneCells(Model model) throws Exception
    {
        double[] box = model.getMicroenvironment().mesh.boundingBox;
        double[] center = new double[] {(box[0] +box[3])/2, (box[1] + box[4])/2, (box[2] + box[5])/2};
        
        RandomGenerator rng = model.getRNG();
        CellDefinition cd = model.getCellDefinition( "immune cell" );
        Microenvironment m = model.getMicroenvironment();
        Set<Cell> cells = m.getAgents( Cell.class );
        double tumor_radius = -9e9; // 250.0; 
        double temp_radius = 0.0;

        // for the loop, deal with the (faster) norm squared 
        for( Cell cell : cells )//int i=0; i < (all_cells).size() ; i++ )
        {
            temp_radius = VectorUtil.dist( center, cell.position );////VectorUtil.norm_squared( cell.position );
            if( temp_radius > tumor_radius )
            {
                tumor_radius = temp_radius;
            }
        }
        // now square root to get to radius 
//        tumor_radius = Math.sqrt( tumor_radius );

        // if this goes wackadoodle, choose 250 
//        if( tumor_radius < 100.0 )
//        {
//            tumor_radius = 100.0;
//        }
//        tumor_radius = 100;
        System.out.println( "current tumor radius: " + tumor_radius );

        // now seed immune cells 

        int number_of_immune_cells = model.getParameterInt( "number_of_immune_cells" ); // 7500; // 100; // 40; 
        double radius_inner = tumor_radius + model.getParameterDouble( "initial_min_immune_distance_from_tumor" );// 30.0; // 75 // 50; 
        double radius_outer = radius_inner + model.getParameterDouble( "thickness_of_immune_seeding_region" ); // 75.0; // 100; // 1000 - 50.0; 

        double mean_radius = 0.5 * ( radius_inner + radius_outer );
        double std_radius = 0.33 * ( radius_outer - radius_inner ) / 2.0;


        for( int i = 0; i < number_of_immune_cells; i++ )
        {
           double theta = rng.UniformRandom() * 6.283185307179586476925286766559;
           double phi = Math.acos( 2.0 * rng.UniformRandom() - 1.0 );
           double radius = rng.NormalRandom( mean_radius, std_radius );
           double[] position = new double[] {radius * Math.cos( theta ) * Math.sin( phi ) +center[0],
                        radius * Math.sin( theta ) * Math.sin( phi )+center[1], radius * Math.cos( phi )+center[2]};
            Cell.createCell( cd, model, position );
         }
    }
}