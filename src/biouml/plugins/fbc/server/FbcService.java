package biouml.plugins.fbc.server;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.plugins.fbc.FbcConstant;
import biouml.plugins.fbc.FbcDiagramUpdater;
import biouml.plugins.fbc.FbcModelCreator;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.ServiceSupport;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.Util;

public class FbcService extends ServiceSupport
{
    public static final String FBC_TABLE = "fbc/table/";
    public static final String FBC_TABLE_RESULT = "fbc/result/";
    public static final String KEY_DE = "de";
    public static final String KEY_FUNCTION_TYPE = "funcType";
    public static final String KEY_SOLVER = "solver";


    @Override
    protected boolean processRequest(ServiceRequest request, int command) throws Exception
    {
        try
        {
            switch( command )
            {
                case 801:
                    saveTableToDiagram( request );
                    break;
                case 802:
                    startCalculation( request );
                    break;
                case 803:
                    sendParameters( request );
                    break;
                case 804:
                    sendCalculationStatus( request );
                    break;
                case 805:
                    sendOptimalValues( request );
                    break;
                default:
                    return false;
            }
            return true;
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Fbc.processRequest: ", e );
            request.error( "Internal error while processing request: " + e.toString() );
        }
        return true;
    }

    private void sendCalculationStatus(ServiceRequest request) throws IOException
    {
        // TODO why DE
        String jobId = getParam( request, KEY_DE );
        Object jobControlObj = WebServicesServlet.getSessionCache().getObject( jobId );
        if( jobControlObj instanceof FbcJobControl )
        {
            FbcJobControl jobControl = (FbcJobControl)jobControlObj;
            request.send( jobControl.getTextStatus() );
        }
        else
        {
            request.error( "Invalid process ID specified: " + jobId );
        }
    }

    private Diagram getDiagram(ServiceRequest request)
    {
        DataElementPath diagramPath = DataElementPath.create( request.get( KEY_DE ) );
        DataElement de = diagramPath.optDataElement();
        if( de != null && de instanceof Diagram )
            return (Diagram)de;
        else
            throw new InvalidParameterException( "Missing diagram " + diagramPath );
    }

    private TableDataCollection getTable(ServiceRequest request)
    {
        String dePath = request.get( KEY_DE );
        String completeName = FbcService.FBC_TABLE + dePath;
        Object tableObj = WebServicesServlet.getSessionCache().getObject( completeName );
        if( tableObj != null && tableObj instanceof TableDataCollection )
            return (TableDataCollection)tableObj;
        else
            throw new InvalidParameterException( "Missing table for diagram " + dePath );
    }

    private String getParam(ServiceRequest request, String paramName)
    {
        String paramObj = request.get( paramName );
        if( paramObj == null )
            throw new InvalidParameterException( "Missing parameter " + paramName );
        return paramObj;
    }

    private FbcModelCreator getModelCreator(ServiceRequest request)
    {
        String solverType = getParam( request, KEY_SOLVER );
        return FbcConstant.getSolverByType( solverType );
    }

    private void startCalculation(ServiceRequest request) throws Exception
    {
        Diagram diagram = getDiagram( request );
        TableDataCollection table = getTable( request );
        String typeObjectiveFunc = getParam( request, KEY_FUNCTION_TYPE );
        FbcModelCreator creator = getModelCreator( request );
        if( diagram != null )
        {
            FbcJobControl jobControl = new FbcJobControl( creator, diagram, table, typeObjectiveFunc );
            Thread t = new Thread( jobControl );
            WebSession.addThread( t );
            t.setPriority( Thread.MIN_PRIORITY );
            t.start();

            String processName = "beans/process/" + Util.getUniqueId();
            WebServicesServlet.getSessionCache().addObject( processName, jobControl, true );
            request.send( processName );
            return;
        }
        request.error( "No calculation started" );
    }

    private void sendOptimalValues(ServiceRequest request) throws Exception
    {
        String jobId = getParam( request, KEY_DE );
        String diagramPathStr = getParam( request, "diagram" );
        Object jobControlObj = WebServicesServlet.getSessionCache().getObject( jobId );
        if( jobControlObj instanceof FbcJobControl )
        {
            FbcJobControl jobControl = (FbcJobControl)jobControlObj;
            TableDataCollection tdc = jobControl.getResult();
            if( tdc != null )
            {
                String completeName = FBC_TABLE_RESULT + diagramPathStr;
                WebServicesServlet.getSessionCache().addObject( completeName, tdc, true );
                request.send( "ok" );
            }
            else
                request.error( "No result calculated" );

        }
        else
        {
            request.error( "Invalid process ID specified: " + jobId );
        }
    }

    private void saveTableToDiagram(ServiceRequest request) throws Exception
    {
        Diagram diagram = getDiagram( request );
        TableDataCollection table = getTable( request );
        final ColumnModel cm = table.getColumnModel();
        final int cnt = cm.getColumnCount();
        List<String> names = new ArrayList<>();
        for( int i = 0; i < cnt; i++ )
        {
            TableColumn col = cm.getColumn( i );
            names.add( col.getName() );
        }
        for( int i = 0; i < table.getSize(); i++ )
        {
            RowDataElement rde = table.getAt( i );
            String name = rde.getName();
            Object[] vals = rde.getValues();
            for( int j = 0; j < vals.length; j++ )
            {
                FbcDiagramUpdater.update( diagram, name, names.get( j ), vals[j].toString() );
            }
        }
        request.send( "ok" );
    }

    private void sendParameters(ServiceRequest request) throws Exception
    {
        JSONObject result = new JSONObject();
        JSONArray funcTypes = new JSONArray( FbcConstant.getAvailableFunctionTypes() );
        JSONArray solvers = new JSONArray( FbcConstant.getAvailableSolverNames() );
        result.put( KEY_FUNCTION_TYPE, funcTypes );
        result.put( KEY_SOLVER, solvers );
        request.send( result.toString() );
    }
}
