package biouml.standard.diagram;

import java.util.Map;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;

public class CompositeFlattener extends DiagramTypeConverterSupport
{
    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        Diagram result = null;
        if( diagram.getType() instanceof CompositeDiagramType )
        {
            CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
            result = preprocessor.preprocess( diagram, diagram.getOrigin(), diagram.getName() );
            PlotsInfo info = DiagramUtility.getPlotsInfo( diagram );
            if( info != null )
                transformPlots( preprocessor, info, result );
        }
        else if( diagram.getType() instanceof MathDiagramType )
        {
            diagram.setType( new CompositeDiagramType() );
        }
        return result;
    }


    /**
     * Transforms plot info 
     * Curves corresponding to variables in submodels now refer to their new names in flat diagram 
     */
    public static void transformPlots(CompositeModelPreprocessor preprocessor, PlotsInfo plotsInfo, Diagram diagram)
    {
        EModel newModel = diagram.getRole( EModel.class );
        PlotsInfo newInfo = new PlotsInfo( newModel );
        PlotInfo[] oldPlots = plotsInfo.getPlots();
        PlotInfo[] newPlots = new PlotInfo[oldPlots.length];
        for( int i = 0; i < oldPlots.length; i++ )
        {
            PlotInfo oldPlot = oldPlots[i];
            newPlots[i] = new PlotInfo( newModel, false );
            newPlots[i].setActive( oldPlot.isActive() );
            newPlots[i].setTitle( oldPlot.getTitle() );
            if( oldPlot.getExperiments() != null )
                newPlots[i].setExperiments( oldPlot.getExperiments().clone() );

            PlotVariable var = oldPlot.getXVariable();
            String path = var.getCompleteName();            
            Map<String, String> mapping = preprocessor.getVarPathMapping( "");
            String newName = mapping.get( path );

            newPlots[i].setXVariable( new PlotVariable( "", newName, var.getTitle(), newModel ) );

            Curve[] oldCurves = oldPlot.getYVariables();
            Curve[] newCurves = new Curve[oldCurves.length];
            for( int j = 0; j < newCurves.length; j++ )
            {
                Curve oldCurve = oldCurves[j];
                path = oldCurve.getCompleteName();              
                
                newName = mapping.get( path );
                newCurves[j] = new Curve( "", newName, oldCurve.getTitle(), newModel );
                newCurves[j].setPen( oldCurve.getPen() );
            }
            newPlots[i].setYVariables( newCurves );
        }
        newInfo.setPlots( newPlots );
        DiagramUtility.setPlotsInfo( diagram, newInfo );
    }
}
