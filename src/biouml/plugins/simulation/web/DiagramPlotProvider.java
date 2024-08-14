package biouml.plugins.simulation.web;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.Pen;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

/**
 * Class to process operations with diagram plot variables (curves, experiments)  
 * @author anna
 *
 */
public class DiagramPlotProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElementPath dePath = arguments.getDataElementPath();
        String action = arguments.getAction();

        Object de = WebBeanProvider.getBean( dePath.toString() );
        if( ! ( de instanceof Diagram ) )
            throw new IllegalArgumentException( "Object is not a diagram: " + dePath );
        Diagram diagram = (Diagram)de;
        PlotsInfo plots = DiagramUtility.getPlotsInfo( diagram );
        if( plots == null )
        {
            plots = new PlotsInfo( diagram.getRole( EModel.class ) );
            DiagramUtility.setPlotsInfo( diagram, plots );
        }
        if( "plots_list".equals( action ) )
        {
            String[] plotNames = StreamEx.of( plots.getPlots() ).map( pi -> pi.getTitle() ).toArray( String[]::new );
            response.sendStringArray( plotNames );
            return;

        }
        else if( "plot_variables".equals( action ) )
        {
            JsonArray result = new JsonArray();
            if( ! ( diagram.getRole() instanceof EModel ) )
                response.sendJSON( result );
            else
            {
                EModel emodel = (EModel)diagram.getRole();
                String path = arguments.getOrDefault( "subdiagram", "" );
                Diagram subDiagram = Util.getInnerDiagram( emodel.getParent(), path );
                if( ! ( subDiagram.getRole() instanceof EModel ) )
                    response.sendJSON( result );
                else
                {
                    subDiagram.getRole( EModel.class ).getVariables().stream()
                            .forEach( variable -> {
                                JsonObject obj = new JsonObject();
                                obj.add( "name", variable.getName() );
                                obj.add( "title", variable.getTitle() );
                                result.add( obj );
                            } );
                    response.sendJSON( result );
                }
            }
            return;
        }
        else if( "subdiagrams".equals( action ) )
        {
            if( DiagramUtility.isComposite( diagram ) )
            {
                String[] subDiagrams = StreamEx.of( Util.getSubDiagrams( diagram ) ).map( s -> Util.getPath( s ) ).toArray( String[]::new );
                response.sendStringArray( subDiagrams );
            }
            else
                response.sendString( "" );
            return;
        }
        else if("add_plot".equals( action ))
        {
            String plotName = arguments.getString( "plotname" );
            PlotInfo[] oldPlots = plots.getPlots();
            PlotInfo[] newPlots = new PlotInfo[oldPlots.length + 1];
            System.arraycopy( oldPlots, 0, newPlots, 0, oldPlots.length );
            PlotInfo newPlot = new PlotInfo( diagram.getRole( EModel.class ) );
            newPlot.setTitle( plotName );
            newPlots[oldPlots.length] = newPlot;
            plots.setPlots( newPlots );
            response.sendString( "" );
            return;
        }
        else if( "remove_plot".equals( action ) )
        {
            String plotName = arguments.getString( "plotname" );
            PlotInfo[] oldPlots = plots.getPlots();
            PlotInfo[] newPlots = StreamEx.of( oldPlots ).filter( pi -> !pi.getTitle().equals( plotName ) ).toArray( PlotInfo[]::new );
            if( newPlots.length < oldPlots.length )
                plots.setPlots( newPlots );
            response.sendString( "" );
            return;
        }

        String plotName = arguments.get( "plotname" );
        PlotInfo plot = plotName != null
                ? StreamEx.of( plots.getPlots() ).findFirst( pi -> pi.getTitle().equals( plotName ) ).orElse( null ) : plots.getPlots()[0];
        if( plot == null && plotName != null )
            throw new IllegalArgumentException( "Plot " + plotName + " not found in diagram " + dePath );
        if( "add".equals( action ) )
        {
            String varName = arguments.getString( "varname" );
            String lineTitle = arguments.get( "title" );
            String path = arguments.getOrDefault( "subdiagram", "" );
            JSONArray colorArray = arguments.optJSONArray( "color" );
            Curve[] curves = plot.getYVariables();
            if( StreamEx.of( curves ).anyMatch( curve -> curve.getName().equals( varName ) && curve.getPath().equals( path ) ) )
                response.error( "Curve for variable '" + varName + "' already exist" );
            else if( lineTitle != null && StreamEx.of( curves ).anyMatch( curve -> curve.getTitle().equals( lineTitle ) ) )
                response.error( "Curve with title '" + lineTitle + "' already exist. Please, change line title." );
            else
            {
                Curve[] newCurves = new Curve[curves.length + 1];
                System.arraycopy( curves, 0, newCurves, 0, curves.length );
                EModel emodel = diagram.getRole( EModel.class );
                Diagram subDiagram = Util.getInnerDiagram( emodel.getParent(), path );
                if( ! ( subDiagram.getRole() instanceof EModel ) )
                    response.error( "Subdiagram " + subDiagram.getName() + " is not EModel" );
                else
                {
                    Variable var = subDiagram.getRole( EModel.class ).getVariable( varName );
                    String curveTitle = ( lineTitle == null ) ? var.getTitle() : lineTitle;
                    Curve newCurve = new Curve( path, varName, curveTitle, emodel );
                    if( colorArray != null )
                    {
                        Color newColor = JSONUtils.parseColor( colorArray.getString( 0 ) );
                        newCurve.setPen( new Pen( new BasicStroke( 1.0f ), newColor ) );
                    }
                    newCurves[curves.length] = newCurve;
                    plot.setYVariables( newCurves );
                    response.sendString( "" );
                }
            }
        }
        else if( "remove".equals( action ) )
        {
            String[] cnames = arguments.optStrings( "rows" );

            if( cnames != null )
            {
                String what = arguments.getString( "what" );
                if( what.equals( "curves" ) )
                {
                    List<Curve> curves = new ArrayList<>();
                    Set<String> toRemove = new HashSet<>( Arrays.asList( cnames ) );
                    for( Curve c : plot.getYVariables() )
                    {
                        if( !toRemove.contains( c.getCompleteName() ) )
                            curves.add( c );
                    }
                    plot.setYVariables( curves.toArray( new Curve[0] ) );
                }

                else if( what.equals( "experiments" ) )
                {
                    if( plot.getExperiments() != null )
                    {
                        List<Experiment> exps = new ArrayList<>();
                        Set<String> toRemove = new HashSet<>( Arrays.asList( cnames ) );
                        for( Experiment c : plot.getExperiments() )
                        {
                            if( !toRemove.contains( c.getName() ) )
                                exps.add( c );
                        }
                        plot.setExperiments( exps.toArray( new Experiment[0] ) );
                    }
                }
            }
            response.sendString( "" );
        }
        if( "addexp".equals( action ) )
        {
            String source = arguments.getString( "source" );
            DataElementPath sourcePath = DataElementPath.create( source );
            String varX = arguments.getString( "x" );
            String varY = arguments.getString( "y" );
            String lineTitle = arguments.get( "title" );
            JSONArray colorArray = arguments.optJSONArray( "color" );
            Experiment[] experiments = plot.getExperiments();
            if( experiments == null )
                experiments = new Experiment[0];

            if( StreamEx.of( experiments ).anyMatch(
                    curve -> curve.getPath().equals( sourcePath ) && curve.getNameX().equals( varX ) && curve.getNameY().equals( varY ) ) )
                response.error( "Experiment already exist." ); //TODO: complete message
            else if( lineTitle != null && StreamEx.of( experiments ).anyMatch( curve -> curve.getTitle().equals( lineTitle ) ) )
                response.error( "Curve with title '" + lineTitle + "' already exist. Please, change line title." );
            else
            {
                Experiment[] newExps = new Experiment[experiments.length + 1];
                System.arraycopy( experiments, 0, newExps, 0, experiments.length );
                String curveTitle = ( lineTitle == null ) ? varY : lineTitle;
                Experiment newExp = new Experiment( sourcePath, varX, varY, curveTitle, null );
                if( colorArray != null )
                {
                    Color newColor = JSONUtils.parseColor( colorArray.getString( 0 ) );
                    newExp.setPen( new Pen( new BasicStroke( 1.0f ), newColor ) );
                }
                newExps[experiments.length] = newExp;
                plot.setExperiments( newExps );
                response.sendString( "" );
            }
        }
    }

}
