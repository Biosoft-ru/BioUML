package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;

import com.developmentontheedge.beans.model.Property;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.table.ColumnEx;
import ru.biosoft.treetable.TreeTableModel;
import ru.biosoft.treetable.TreeTableElement;

public class WebTreeTablesProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        String actionStr = arguments.getAction();
        TreeTableModel model = getModel(arguments.getDataElementPath());

        if( actionStr.equals("sceleton") )
        {
            StringBuilder buffer = new StringBuilder();
            buffer.append("<table><thead><tr>");
            for( int i = 0; i < model.getColumnCount(); i++ )
            {
                String columnName = model.getColumnName(i);
                buffer.append("<th>");
                buffer.append(columnName);
                buffer.append("</th>");
            }
            buffer.append("</tr></thead><tbody>");

            buffer.append("</tbody></table>");
            response.sendString(buffer.toString());
        }
        else if( actionStr.equals("children") )
        {
            String parentPath = arguments.get("parentPath");
            if( parentPath != null )
            {
                Object parent = getParent(model.getRoot(), parentPath);
                if( parent != null && ( model.getChildCount(parent) > 0 ) )
                {
                    JsonArray rows = new JsonArray();
                    for( int i = 0; i < model.getChildCount(parent); i++ )
                    {
                        Object child = model.getChild(parent, i);
                        String nodeId = (String)model.getValueAt(child, 0);
                        JsonArray values = new JsonArray();
                        
                        for( int j = 1; j < model.getColumnCount(); j++ )
                        {
                            ReferenceType type = null;
                            boolean displayTitle = false;
                            if(model.getColumn(j) instanceof ColumnEx)
                            {
                                ColumnEx columnEx = (ColumnEx)model.getColumn(j);
                                type = ReferenceTypeRegistry.optReferenceType(columnEx.getValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY));
                                displayTitle =  Boolean.valueOf( columnEx.getValue( ColumnEx.DISPLAY_TITLE ) );
                            }
                            String value = "";
                            Object valueObj = model.getValueAt(child, j);
                            if( valueObj instanceof Property )
                                valueObj = ( (Property)valueObj ).getValue();
                            if( valueObj != null )
                                value = WebTablesProvider.getControlCode( valueObj, true, nodeId + ":" + j, null, type, displayTitle )
                                        .withData( "id", nodeId ).render();
                            values.add(value);
                        }
                        JsonObject row = new JsonObject().add( "id", nodeId ).add( "isLeaf", ( model.getChildCount( child ) == 0 ) )
                                .add( "values", values );
                        rows.add(row);
                    }
                    response.sendJSON(rows);
                }
                else
                {
                    throw new WebException("EX_QUERY_EMPTY_BRANCH", parentPath);
                }
            }
        } else
            throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
    }

    protected Object getParent(Object root, String parentPath)
    {
        if( parentPath.length() == 0 )
            return root;

        if( root instanceof DataElementPath )
            return DataElementPath.create( ( (DataElementPath)root ).toString() + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + parentPath);

        return null;
    }

    protected TreeTableModel getModel(DataElementPath dePath) throws WebException
    {
        Object model = WebServicesServlet.getSessionCache().getObject(dePath.toString());
        if( model instanceof TreeTableModel )
        {
            return (TreeTableModel)model;
        }

        TreeTableElement de = getDataElement(dePath, TreeTableElement.class);
        model = new TreeTableModel(de);
        WebServicesServlet.getSessionCache().addObject(dePath.toString(), model, false);
        return (TreeTableModel)model;
    }
}
