package biouml.plugins.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Request;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.server.access.ClientModule;
import biouml.standard.type.DiagramInfo;

/**
 * Optimize diagram loader
 */
public class DiagramClient extends DiagramProtocol
{
    Request connection;

    public DiagramClient(Request conn)
    {
        this.connection = conn;
    }

    public void close()
    {
        if( connection != null )
            connection.close();
    }

    ////////////////////////////////////////
    // Request functions
    //

    /**
     * Opens the connection with the server,
     * sends request, reads the answer,
     * check it, and close the connection.
     *
     * @param command  request command (cod)
     * @param argument request argument
     *
     * @see Connection
     */
    public byte[] request(int command, Map<String, String> arguments, boolean readAnswer) throws BiosoftNetworkException
    {
        if( connection != null )
        {
            return connection.request(DiagramProtocol.DIAGRAM_SERVICE, command, arguments, readAnswer);
        }
        return null;
    }

    /**
     * Request diagram info, then request diagram,
     * parse, preload all necessary ru.biosoft.access.core.DataElement-s
     * and create diagram
     */
    public Diagram getDiagram(DataCollection<Diagram> parent, String module, String name) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(DiagramProtocol.KEY_DATABASE, module);
        map.put(DiagramProtocol.KEY_DIAGRAM, name);

        byte[] data = request(DiagramProtocol.DB_GET_DIAGRAM, map, true);

        DataElement diagramInfo = null;

        // get DiagramInfo
        if( data != null )
        {
            String infoStr = new String(data, "UTF-16BE");

            if( !"null".equals(infoStr) )
            {
                DiagramInfoTransformer transformer = new DiagramInfoTransformer();
                transformer.init(null, parent);
                diagramInfo = convertToDataElement(parent, transformer, name, infoStr);
            }
        }

        // get Diagram
        data = request(DiagramProtocol.DB_GET_DIAGRAM2, map, true);

        if( diagramInfo == null )
            diagramInfo = new DiagramInfo(parent, name);

        if( data == null )
            return null;

        // check if we can preload data elements and prevent gc before diagram reading
        Module parentModule = Module.optModule(parent);
        if( parentModule instanceof ClientModule )
        {
            // find necessary DataElements            
            List<String> kernelNames = new ArrayList<>();
            DiagramXmlReader.readDiagram(name, new ByteArrayInputStream(data), (DiagramInfo)diagramInfo, parent, parentModule, kernelNames, null);
            ( (ClientModule)parentModule ).preload(kernelNames);
        }
        // load diagram
        return DiagramXmlReader.readDiagram(name, new ByteArrayInputStream(data), (DiagramInfo)diagramInfo, parent, parentModule);
    }

    public void putDiagram(DataCollection<Diagram> parent, String module, Diagram diagram) throws Exception
    {
        DataElement diagramInfo = diagram.getKernel();
        if( !(diagramInfo instanceof DiagramInfo) )
            diagramInfo = new DiagramInfo(diagram.getName());
        DiagramInfoTransformer transformer = new DiagramInfoTransformer();
        transformer.init(null, parent);
        String infoStr = convertToString((DiagramInfo)diagramInfo, transformer);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DiagramXmlWriter writer = new DiagramXmlWriter(baos);
        writer.write(diagram);
        baos.flush();

        Map<String, String> map = new HashMap<>();
        map.put(DiagramProtocol.KEY_DATABASE, module);
        map.put(DiagramProtocol.KEY_DIAGRAM, diagram.getName());
        map.put(DiagramProtocol.KEY_INFO, "" + infoStr);
        map.put(DiagramProtocol.KEY_DATA, baos.toString());
        request(DiagramProtocol.DB_PUT_DIAGRAM, map, false);
    }
}
