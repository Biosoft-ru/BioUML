package biouml.plugins.optimization.document.editors;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.swing.PropertyInspector;

import biouml.plugins.simulation.SimulationTaskParameters;
import ru.biosoft.gui.ViewPartSupport;

public class SimulationParametersTab extends ViewPartSupport
{
    protected PropertyInspector inspector;

    private SimulationTaskParameters stp;

    public SimulationParametersTab(SimulationTaskParameters stp)
    {
        this.stp = stp;

        inspector = new PropertyInspector();
        inspector.setDefaultNumberFormat( null );
        add( BorderLayout.CENTER, inspector );

        initListeners();

        inspector.explore( stp.getParametersBean() );
        inspector.setComponentModel( ComponentFactory.getModel( stp.getParametersBean(), Policy.UI, true ) );
    }

    private PropertyChangeListener simulationEngineListener;
    private void initListeners()
    {
        simulationEngineListener = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if( evt.getPropertyName().equals( "simulator" ) || evt.getPropertyName().equals( "engine" ) )
                {
                    inspector.setComponentModel( ComponentFactory.getModel( stp.getParametersBean(), Policy.UI, true ) );
                }
            }
        };

        stp.addPropertyChangeListener( simulationEngineListener );
    }

    public void restoreListeners()
    {
        stp.removePropertyChangeListener( simulationEngineListener );
    }
}
