package biouml.plugins.physicell._test;

import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Model;

public class JPhysicellTest
{

    public static void main(String ... args)
    {

        Model model = new Model();
        Microenvironment m = model.getMicroenvironment();
        m.options.initial_condition_vector = new double[1];
        m.addDensity( "oxygen2", "", 0, 0 );

        m.resizeSpace( 0, 100, 0, 100, 0, 100, 20, 20, 20 );
        System.out.println( model.display() );
    }
}