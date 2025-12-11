import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.biofvm.Microenvironment;

public class Initial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
        Microenvironment m = model.getMicroenvironment();
        double Xmin = m.mesh.boundingBox[0];
        double Ymin = m.mesh.boundingBox[1];
        double Xmax = m.mesh.boundingBox[3];
        double Ymax = m.mesh.boundingBox[4];

        double Xrange = Xmax - Xmin;
        double Yrange = Ymax - Ymin;

        int directorsNumber = model.getParameterInt( "number_of_directors" ); // 15;  
        int cargoClustersNumber = model.getParameterInt( "number_of_cargo_clusters" ); // 100;  
        int workersNumber = model.getParameterInt( "number_of_workers" ); // 50;  
        CellDefinition pCargoDef = model.getCellDefinition( "cargo cell" );
        CellDefinition pDirectorDef = model.getCellDefinition( "director cell" );
        CellDefinition pWorkerDef = model.getCellDefinition( "worker cell" );

        // randomly place seed cells 
        double[] position = new double[3];
        double relative_margin = 0.2;
        double relative_outer_margin = 0.02;

        for( int i = 0; i < directorsNumber; i++ )
        {
            position[0] = m.options.X_range[0]
                    + Xrange * ( relative_margin + ( 1.0 - 2 * relative_margin ) * model.getRNG().UniformRandom() );

            position[1] = m.options.Y_range[0]
                    + Yrange * ( relative_outer_margin + ( 1.0 - 2 * relative_outer_margin ) * model.getRNG().UniformRandom() );
            Cell cell = Cell.createCell( pDirectorDef, model, position );
            model.getSignals().setSingleBehavior( cell, "movable", 0 );
        }

        // place cargo clusters on the fringes 
        for( int i = 0; i < cargoClustersNumber; i++ )
        {
            position[0] = m.options.X_range[0]
                    + Xrange * ( relative_outer_margin + ( 1 - 2.0 * relative_outer_margin ) * model.getRNG().UniformRandom() );

            position[1] = m.options.Y_range[0]
                    + Yrange * ( relative_outer_margin + ( 1 - 2.0 * relative_outer_margin ) * model.getRNG().UniformRandom() );

            if( model.getRNG().UniformRandom() < 0.5 )
                Cell.createCell( pCargoDef, model, position );
            else
                createCargoCluster7( position, model );
        }

        // place workers
        for( int i = 0; i < workersNumber; i++ )
        {
            position[0] = m.options.X_range[0]
                    + Xrange * ( relative_margin + ( 1.0 - 2 * relative_margin ) *model.getRNG().UniformRandom() );

            position[1] = m.options.Y_range[0]
                    + Yrange * ( relative_outer_margin + ( 1.0 - 2 * relative_outer_margin ) * model.getRNG().UniformRandom() );
            Cell.createCell( pWorkerDef, model, position );
        }
    }

    /** 
     * Create a hollow cluster at position, with random orientation 
     */
    void createCargoCluster6(double[] center, Model model) throws Exception
    {
       
        CellDefinition pCargoDef = model.getCellDefinition( "cargo cell" );
        double spacing = 0.95 * pCargoDef.phenotype.geometry.radius * 2.0;
        double dTheta = 1.047197551196598; // 2*pi / 6.0 

        double theta = 6.283185307179586 * model.getRNG().UniformRandom();
        double[] position = new double[3];
        for( int i = 0; i < 6; i++ )
        {
            position[0] = center[0] + spacing * Math.cos( theta );
            position[1] = center[1] + spacing * Math.sin( theta );
            Cell.createCell( pCargoDef, model, position );
            theta += dTheta;
        }
    }

    /**
     * Creates a filled cluster at position, with random orientation 
     */
    void createCargoCluster7(double[] center, Model model) throws Exception
    {
        CellDefinition pCargoDef =  model.getCellDefinition( "cargo cell" );
        createCargoCluster6( center, model );
        Cell.createCell( pCargoDef, model, center );
    }

    /**
     *  Creates a small cluster at position, with random orientation 
     */
    void createCargoCluster3(double[] center, Model model) throws Exception
    {
        CellDefinition pCargoDef =  model.getCellDefinition( "cargo cell" );
        double spacing = 0.95 * pCargoDef.phenotype.geometry.radius * 1.0;
        double d_Theta = 2.094395102393195; // 2*pi / 3.0 
        double theta = 6.283185307179586 * model.getRNG().UniformRandom();
        double[] position = new double[3];
        for( int i = 0; i < 3; i++ )
        {
            position[0] = center[0] + spacing * Math.cos( theta );
            position[1] = center[1] + spacing * Math.sin( theta );
            Cell.createCell( pCargoDef, model, position );
            theta += d_Theta;
        }
    }
}