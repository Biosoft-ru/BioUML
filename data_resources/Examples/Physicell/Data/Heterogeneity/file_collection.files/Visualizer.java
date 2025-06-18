import java.awt.Color;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.ui.AgentColorer;

public class Visualizer implements AgentColorer
{
    private double pMin;
    private double pMax;
    private SignalBehavior signals;
    private boolean isInit;

    public void init(Model model)
    {
        signals = model.getSignals();
        pMin = model.getParameterDouble( "oncoprotein_min" );
        pMax = model.getParameterDouble( "oncoprotein_max" );
        isInit = true;
    }

    @Override
    public Color[] findColors(Cell pCell)
    {
        if ( !isInit )
            init( pCell.getModel() );

        double p = signals.getSingleSignal( pCell, "custom:oncoprotein" );

        // immune are black
        Color[] output = new Color[] {Color.black, Color.black, Color.black, Color.black};

        if( pCell.type == 1 )
            return output;

        // live cells are green, but shaded by oncoprotein value 
        if( !pCell.phenotype.death.dead )
        {
            int oncoprotein = (int)Math.round( ( 1.0 / ( pMax - pMin ) ) * ( p - pMin ) * 255.0 );
            output[0] = new Color( oncoprotein, oncoprotein, 255 - oncoprotein );
            output[1] = new Color( oncoprotein, oncoprotein, 255 - oncoprotein );
            output[2] = new Color( (int) ( oncoprotein / pMax ), (int) ( oncoprotein / pMax ), (int) ( ( 255 - oncoprotein ) / pMax ) );
        }

        // if not, dead colors 
        if( signals.getSingleSignal( pCell, "apoptotic" ) > 0.5 )
        {
            output[0] = new Color( 255, 0, 0 );
            output[2] = new Color( 125, 0, 0 );
        }

        // Necrotic - Brown
        if( signals.getSingleSignal( pCell, "necrotic" ) > 0.5 )
        {
            output[0] = new Color( 250, 138, 38 );
            output[2] = new Color( 139, 69, 19 );
        }
        return output;
    }
}