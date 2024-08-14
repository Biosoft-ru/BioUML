package biouml.plugins.optimization.diagram;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.document.OptimizationDocument;

public class OptimizationChangeListener extends OptimizationDiagramManager implements PropertyChangeListener
{
    protected OptimizationDocument optimizationDocument;

    public OptimizationChangeListener(Optimization optimization, OptimizationDocument optimizationDocument)
    {
        super(optimization);
        this.optimizationDocument = optimizationDocument;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        try
        {
            String propertyName = evt.getPropertyName();
            if( OptimizationParameters.OPTIMIZATION_EXPERIMENTS.equals(propertyName) )
            {
                refreshNodes(optimization.getOptimizationDiagram());
            }
            else if( Optimization.OPTIMIZATION_METHOD.equals(propertyName) )
            {
                refreshNodes(optimization.getOptimizationDiagram());
            }
            else if( Optimization.OPTIMIZATION_DIAGRAM_PATH.equals(propertyName) )
            {
                initDiagram();
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not process change event", e);
        }
    }

    @Override
    public void refreshNodes(Diagram diagram) throws Exception
    {
        if( diagram != null )
        {
            super.refreshNodes(diagram);
            optimizationDocument.getDiagramDocument(diagram).update();
        }
    }

}
