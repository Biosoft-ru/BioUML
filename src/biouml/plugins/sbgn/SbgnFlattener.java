package biouml.plugins.sbgn;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.diagram.CompositeFlattener;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.DiagramUtility;

/**
 * Convert diagram adding EModel and necessary roles for simulation
 */
public class SbgnFlattener extends DiagramTypeConverterSupport
{
    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        Diagram result = null;
        if (diagram.getType() instanceof SbgnCompositeDiagramType)
        {
            CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
            result = preprocessor.preprocess( diagram, diagram.getOrigin(), diagram.getName() );
            PlotsInfo info = DiagramUtility.getPlotsInfo( diagram );
            if( info != null )
                CompositeFlattener.transformPlots( preprocessor, info, result );
        }
        else if (diagram.getType() instanceof SbgnDiagramType)
        {
            result = diagram.clone( null, diagram.getName());
            result.setType(new SbgnCompositeDiagramType());
        }

        return result;
    }
}
