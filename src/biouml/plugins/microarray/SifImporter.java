package biouml.plugins.microarray;

import java.io.File;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graph.ForceDirectedLayouter;
import biouml.model.Diagram;
import biouml.model.DiagramImporter;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.diagram.SemanticNetworkDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.workbench.graph.DiagramToGraphTransformer;

public class SifImporter extends DiagramImporter
{
    protected static final Logger log = Logger.getLogger(SifImporter.class.getName());
    protected static final String SIF_FORMAT = "sif";

    @Override
    public int accept(File file)
    {
        if( file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase().equals(SIF_FORMAT) )
        {
            return ACCEPT_MEDIUM_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }

    /**
     * @todo Play with diagramName and diagram.getName() -
     * there is something bad
     *
     * @todo Implement clone() method of EModel
     */
    @Override
    public DataElement doImport(Module module, File file, String diagramName) throws Exception
    {
        DataCollection<Diagram> origin = (DataCollection<Diagram>)module.get(Module.DIAGRAM);
        Diagram diagram = readDiagram(file, origin, diagramName);

        origin.put(diagram);
        return diagram;
    }

    public Diagram readDiagram(File file, DataCollection<Diagram> origin, String diagramName)
    {
        try
        {
            AttributeMatrixFileParser amfp = new AttributeMatrixFileParser();
            Map<String, List<Object[]>> values = amfp.parseFile(file);
            
            DiagramType diagramType = new SemanticNetworkDiagramType();
            DiagramInfo diagramInfo = new DiagramInfo(origin, diagramName);
            Diagram diagram = new Diagram(origin, diagramInfo, diagramType);
            diagram.setTitle(diagramName);

            diagram.setNotificationEnabled(false);
            
            for(Entry<String, List<Object[]>> entry : values.entrySet())
            {
                String key = entry.getKey();
                if(!key.equals("types") && !key.equals(amfp.getHeaderName()))
                {
                    for(Object[] objects : entry.getValue())
                    {
                        if(objects.length == 3)
                        {
                            Node nodeStart = new Node(diagram, new Stub(null, objects[0].toString(), Type.TYPE_PHYSICAL_ENTITY));
                            diagram.put(nodeStart);
                            
                            Node nodeFinish = new Node(diagram, new Stub(null, objects[2].toString(), Type.TYPE_PHYSICAL_ENTITY));
                            diagram.put(nodeFinish);
                            
                            SemanticRelation sr = new SemanticRelation(null, objects[0].toString()+":"+objects[2].toString());
                            sr.setRelationType(objects[1].toString());
                            Edge edge = new Edge(diagram, sr, nodeStart, nodeFinish);
                            diagram.put(edge);
                        }
                    }
                }
            }
            diagram.setNotificationEnabled(true);
            
            ForceDirectedLayouter layouter = new ForceDirectedLayouter();
            DiagramToGraphTransformer.layout(diagram, layouter);
            
            return diagram;
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create diagram: ", t);
        }
        return null;
    }
}
