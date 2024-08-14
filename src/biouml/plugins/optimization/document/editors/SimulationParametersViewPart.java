package biouml.plugins.optimization.document.editors;

import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.simulation.SimulationTaskParameters;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

public class SimulationParametersViewPart extends ViewPartSupport
{
    protected JTabbedPane tabbedPane;

    private final PropertyChangeListener stpListener;

    public SimulationParametersViewPart()
    {
        tabbedPane = new JTabbedPane( SwingConstants.TOP );

        stpListener = evt -> {
            if( evt.getPropertyName().equals( OptimizationParameters.OPTIMIZATION_EXPERIMENTS ) )
            {
                List<OptimizationExperiment> experiments = ( (Optimization)model ).getParameters().getOptimizationExperiments();
                Map<String, SimulationTaskParameters> simulationTaskParameters = ( (Optimization)model ).getParameters()
                        .getSimulationTaskParameters();
                initTabbedPane( experiments, simulationTaskParameters );
            }
        };

        add( BorderLayout.CENTER, tabbedPane );
    }

    private void initTabbedPane(List<OptimizationExperiment> experiments, Map<String, SimulationTaskParameters> simulationTaskParameters)
    {
        for( int i = 0; i < tabbedPane.getTabCount(); ++i )
        {
            SimulationParametersTab tab = (SimulationParametersTab)tabbedPane.getComponentAt( i );
            tab.restoreListeners();
        }

        tabbedPane.removeAll();
        for( int i = 0; i < experiments.size(); ++i )
        {
            String name = experiments.get( i ).getName();
            SimulationParametersTab stpTab = new SimulationParametersTab( simulationTaskParameters.get( name ) );
            tabbedPane.addTab( name, stpTab );
        }
        tabbedPane.setSelectedIndex( 0 );
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( model != null )
        {
            OptimizationParameters params = ( (Optimization)model ).getParameters();
            params.removePropertyChangeListener( stpListener );

            List<OptimizationExperiment> experiments = ( (Optimization)model ).getParameters().getOptimizationExperiments();
            Map<String, SimulationTaskParameters> simulationTaskParameters = ( (Optimization)model ).getParameters()
                    .getSimulationTaskParameters();
            initTabbedPane( experiments, simulationTaskParameters );

            params.addPropertyChangeListener( stpListener );
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        return ( model instanceof Optimization );
    }

}
