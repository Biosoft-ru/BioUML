package biouml.plugins.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverter;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.StandardModuleType;
import biouml.standard.type.DiagramInfo;
import biouml.workbench.diagram.DiagramTypeConverterRegistry;
import biouml.workbench.diagram.DiagramTypeConverterRegistry.Conversion;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Response;
import ru.biosoft.server.Service;
import ru.biosoft.server.SynchronizedServiceSupport;
import ru.biosoft.util.Clazz;
import ru.biosoft.util.JsonUtils;

/**
 * Optimization of loading of diagrams
 */
public class DiagramService extends DiagramProtocol implements Service
{
    protected SynchronizedServiceSupport ss;

    protected Response connection;
    protected Map arguments;

    public DiagramService()
    {
        ss = new SynchronizedServiceSupport()
        {
            @Override
            protected boolean processRequest(int command) throws Exception
            {
                return DiagramService.this.processRequest(command);
            }
        };
    }

    @Override
    public synchronized void processRequest(Integer command, Map data, Response out)
    {
        ss.processRequest(command, data, out);
    }


    protected boolean processRequest(int command) throws Exception
    {
        connection = ss.getSessionConnection();
        arguments = ss.getSessionArguments();

        switch( command )
        {
            case DiagramProtocol.DB_GET_DIAGRAM:
                sendDiagram();
                break;
            case DiagramProtocol.DB_GET_DIAGRAM2:
                sendData();
                break;

            case DiagramProtocol.DB_PUT_DIAGRAM:
                putDiagram();
                break;

            case DiagramProtocol.GET_DIAGRAM_TYPES:
                sendDiagramTypes();
                break;

            case DiagramProtocol.CREATE_DIAGRAM:
                createDiagram();
                break;

            case DiagramProtocol.GET_CONVERT_TYPES:
                sendConvertTypes();
                break;

            case DiagramProtocol.DB_CONVERT_DIAGRAM:
                convertDiagram();
                break;

            default:
                return false;
        }
        return true;
    }

    //////////////////////////////////////////////
    // Protocol implementation functions
    //

    protected void sendDiagram() throws Exception
    {
        Object moduleName = arguments.get(DiagramProtocol.KEY_DATABASE);
        if( moduleName == null )
        {
            connection.error("didn't send database name");
            return;
        }

        Object diagramName = arguments.get(DiagramProtocol.KEY_DIAGRAM);
        if( diagramName == null )
        {
            connection.error("didn't send diagram name");
            return;
        }

        DataCollection module = CollectionFactory.getDataCollection(moduleName.toString());
        if( module == null )
        {
            connection.error("invalid database name: " + moduleName.toString());
            return;
        }
        Diagram diagram = getDiagram(module, diagramName.toString());
        if( diagram == null )
        {
            connection.error("invalid diagram name: " + diagramName.toString());
            return;
        }

        DiagramInfo diagramInfo = (DiagramInfo)diagram.getKernel();
        String info = "null";
        if( diagramInfo != null )
        {
            DiagramInfoTransformer transformer = new DiagramInfoTransformer();
            transformer.init(null, diagram.getOrigin());
            info = this.convertToString(diagramInfo, transformer);
        }
        connection.send(info.getBytes("UTF-16BE"), Connection.FORMAT_GZIP);
    }

    private Diagram getDiagram(DataCollection module, String name) throws Exception
    {
        DataElement de = module.get(name);
        if( de instanceof Diagram )
            return (Diagram)de;
        if( module instanceof Module )
            return ( (Module)module ).getDiagram(name);
        return null;
    }

    protected void sendData() throws Exception
    {
        Object moduleName = arguments.get(DiagramProtocol.KEY_DATABASE);
        if( moduleName == null )
        {
            connection.error("didn't send database name");
            return;
        }

        Object diagramName = arguments.get(DiagramProtocol.KEY_DIAGRAM);
        if( diagramName == null )
        {
            connection.error("didn't send diagram name");
            return;
        }

        DataCollection module = CollectionFactory.getDataCollection(moduleName.toString());
        if( module == null )
        {
            connection.error("invalid database name: " + moduleName.toString());
            return;
        }
        Diagram diagram = getDiagram(module, diagramName.toString());
        if( diagram == null )
        {
            connection.error("invalid diagram name: " + diagramName.toString());
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DiagramXmlWriter writer = new DiagramXmlWriter(baos);
        writer.write(diagram);
        baos.flush();

        connection.send(baos.toByteArray(), Connection.FORMAT_GZIP);
    }

    public static class Entry implements Serializable
    {
        public Entry(String name, String dcName, String entry)
        {
            this.name = name;
            this.dcName = dcName;
            this.entry = entry;
        }
        public String name;
        public String dcName;
        public String entry;
    }

    protected void putDiagram() throws Exception
    {
        Object moduleName = arguments.get(DiagramProtocol.KEY_DATABASE);
        if( moduleName == null )
        {
            connection.error("didn't send database name");
            return;
        }

        Object diagramName = arguments.get(DiagramProtocol.KEY_DIAGRAM);
        if( diagramName == null )
        {
            connection.error("didn't send diagram name");
            return;
        }

        Object infoStr = arguments.get(DiagramProtocol.KEY_INFO);
        if( infoStr == null )
        {
            connection.error("didn't send diagram info");
            return;
        }

        Object data = arguments.get(DiagramProtocol.KEY_DATA);
        if( data == null )
        {
            connection.error("didn't send diagram data");
            return;
        }

        DataCollection module = CollectionFactory.getDataCollection(moduleName.toString());
        if( module == null )
        {
            connection.error("invalid database name: " + moduleName.toString());
            return;
        }
        if( ! ( module instanceof Module ) )
        {
            connection.error("there is no database: " + moduleName.toString());
            return;
        }
        DiagramInfo info = null;

        DiagramInfoTransformer transformer = new DiagramInfoTransformer();
        transformer.init(null, ( (Module)module ).getDiagrams());
        DataElement de = convertToDataElement( ( (Module)module ).getDiagrams(), transformer, diagramName.toString(), infoStr.toString());

        if( de != null )
            info = (DiagramInfo)de;
        else
            info = new DiagramInfo( ( (Module)module ).getDiagrams(), diagramName.toString());

        Diagram diagram = DiagramXmlReader.readDiagram(diagramName.toString(), new ByteArrayInputStream(data.toString().getBytes()), info, ( (Module)module ).getDiagrams(), (Module)module);
        if( diagram != null )
            ( (Module)module ).getDiagrams().put(diagram);

        connection.send(null, Connection.FORMAT_SIMPLE);
    }

    protected void sendDiagramTypes() throws Exception
    {
        Object dcName = arguments.get(DiagramProtocol.KEY_DC);
        if( dcName == null )
        {
            connection.error("didn't send database name");
            return;
        }

        DataCollection dc = DataElementPath.create(dcName.toString()).getDataCollection();
        Module module = null;
        try
        {
            module = Module.getModule(dc);
        }
        catch (Exception e)
        {
        }
        StreamEx<DiagramType> types;
        if( module == null )
        {
            types = StandardModuleType.getGeneralPurposeTypes().map( Clazz.of( DiagramType.class )::createOrLog );
        }
        else
        {
            ModuleType moduleType = module.getType();
            types = moduleType.getDiagramTypeObjects();
        }

        JsonArray diagramTypes = types
                .map( dt -> new JsonObject()
                    .add( "name", dt.getTitle() )
                    .add( "title", dt.getTitle() )
                    .add( "description", dt.getDescription() ) )
                .collect( JsonUtils.toArray() );
        connection.send(diagramTypes.toString().getBytes("UTF-16BE"), Connection.FORMAT_GZIP);
    }

    protected void createDiagram() throws IOException
    {
        Object dcName = arguments.get(DiagramProtocol.KEY_DC);
        if( dcName == null )
        {
            connection.error("didn't send database name");
            return;
        }

        DataCollection dc = DataElementPath.create(dcName.toString()).getDataCollection();

        Module module = null;
        try
        {
            module = Module.getModule( dc );
        }
        catch (Exception e)
        {
        }
        Object diagramType = arguments.get( DiagramProtocol.KEY_TYPE );

        DataCollection origin = null;

        String diagramName = arguments.get(DiagramProtocol.KEY_DIAGRAM).toString();
        try
        {
            DiagramType type = null;
            if( module != null )
            {
                ModuleType moduleType = module.getType();
                type = moduleType.getDiagramTypeObjects().findAny( dt -> dt.getTitle().equals( diagramType ) ).orElse( null );
                try
                {
                    origin = module.getDiagrams();
                }
                catch (Exception e)
                {
                    connection.error( "Can not get Diagram collection in database: " + module.getCompletePath() );
                }
                if( origin == null )
                    origin = dc;
            }
            else
            {
                origin = dc;
                type = StandardModuleType.getGeneralPurposeTypes().map( Clazz.of( DiagramType.class )::createOrLog ).findAny( dt -> dt.getTitle().equals( diagramType ) )
                        .orElse( null );
            }

            if( type != null )
            {
                Diagram diagram = type.createDiagram(origin, diagramName, null);
                CollectionFactoryUtils.save(diagram);
                JsonObject diagramObj = new JsonObject()
                    .add( "name", diagram.getCompletePath().toString() )
                    .add( "type", diagram.getType().getClass().getName() );
                connection.send(diagramObj.toString().getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
            }
            else
                connection.error("Can not create diagram " + diagramName);
        }
        catch( Exception e )
        {
            connection.error("Can not create diagram " + diagramName);
        }

    }

    /**
     * Send possible convert types for selected diagram
     */
    protected void sendConvertTypes() throws Exception
    {
        Object dcName = arguments.get(DiagramProtocol.KEY_DC);
        if( dcName == null )
        {
            connection.error("didn't send diagram name");
            return;
        }

        DataElement de = CollectionFactory.getDataElement(dcName.toString());
        if( de instanceof Diagram )
        {
            JsonArray possibleTypes = new JsonArray();
            Conversion[] conversions = DiagramTypeConverterRegistry.getPossibleConversions( ( (Diagram)de ).getType().getClass().getName());
            for( int i = 0; i < conversions.length; i++ )
            {
                possibleTypes.add(new JsonObject().add("title", conversions[i].getDiagramTypeDisplayName()).add("id", i));
            }
            connection.send(possibleTypes.toString().getBytes("UTF-16BE"), Connection.FORMAT_GZIP);
        }
        else
        {
            connection.error("Invalid diagram path: " + dcName.toString());
        }
    }

    /**
     * Convert diagram to selected type
     */
    protected void convertDiagram() throws Exception
    {
        Object dcName = arguments.get(DiagramProtocol.KEY_DC);
        if( dcName == null )
        {
            connection.error("didn't send diagram name");
            return;
        }

        DataElement de = CollectionFactory.getDataElement(dcName.toString());
        if( de instanceof Diagram )
        {
            Object targetName = arguments.get("newdc");
            Object conversionIdObj = arguments.get("id");
            if( targetName != null && conversionIdObj != null )
            {
                Conversion[] conversions = DiagramTypeConverterRegistry.getPossibleConversions( ( (Diagram)de ).getType().getClass()
                        .getName());
                try
                {
                    int conversionId = Integer.parseInt((String)conversionIdObj);
                    Conversion conversion = conversions[conversionId];
                    DiagramTypeConverter converter = conversion.getConverter().newInstance();
                    DataElementPath path = DataElementPath.create((String)targetName);
                    DataCollection origin = path.getParentCollection();
                    if( !origin.isMutable() )
                    {
                        connection.error("Converted diagram can not be saved into " + origin.getCompletePath()
                                + ". Please, select another path.");
                        return;
                    }
                    Diagram newDiagram = ( (Diagram)de ).clone(origin, path.getName());
                    newDiagram = converter.convert(newDiagram, conversion.getDiagramType());
                    try
                    {
                        origin.put(newDiagram);
                        connection.send(null, Connection.FORMAT_SIMPLE);
                    }
                    catch( Exception e )
                    {
                        connection.error("Can not save converted diagram into " + origin.getCompletePath() + " : " + e.getMessage());
                    }
                }
                catch( Exception e )
                {
                    connection.error("Cannot convert diagram: " + ExceptionRegistry.log(e));
                }
            }
            else
            {
                connection.error("Invalid parameters");
            }
        }
        else
        {
            connection.error("Invalid diagram path: " + dcName.toString());
        }
    }
}
