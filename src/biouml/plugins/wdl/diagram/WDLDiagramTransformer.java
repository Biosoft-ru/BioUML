package biouml.plugins.wdl.diagram;


import java.io.File;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.type.DiagramInfo;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.PriorityTransformer;
import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.PathwayLayouter;

public class WDLDiagramTransformer extends AbstractFileTransformer<Diagram> implements PriorityTransformer
{
    private static final Logger log = Logger.getLogger( WDLDiagramTransformer.class.getName() );
    
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.wdl$", Pattern.CASE_INSENSITIVE );
    
    public WDLDiagramTransformer()
    {
    }

    @Override
    public Class<Diagram> getOutputType()
    {
        return Diagram.class;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(EXTENSION_REGEXP.matcher( name ).find())
            return 2;//lower then WDLScriptTransformer
        return 0;
    }

    @Override
    public Diagram load(File input, String name, DataCollection<Diagram> origin) throws Exception
    {
        Diagram diagram = new Diagram( origin, name );
        DiagramInfo kernel = new DiagramInfo( name );
        diagram.setKernel( kernel );
        diagram.setType( new WDLDiagramType() );
        WDLSemanticController controller = (WDLSemanticController)diagram.getType().getSemanticController();
        
        Object graph = loadWDLGraph( input );
        
        Map<Object, Node> wdlToBiouml = new IdentityHashMap<>();
//        controller.handleGraph( graph, diagram, wdlToBiouml );
        
        layout( diagram );
        
        return diagram;
    }
    
    private static void layout(Diagram diagram)
    {
        HierarchicLayouter layouter = new HierarchicLayouter();
        layouter.setHoistNodes(true);
        layouter.getSubgraphLayouter().layerDeltaY = 50;
        ru.biosoft.graph.Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        pathwayLayouter.doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
        diagram.setView(null);
        diagram.getViewOptions().setAutoLayout( true );
    }
    
    @Override
    public void save(File output, Diagram element) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement de)
    {
        if(de instanceof Diagram && ( (Diagram)de ).getType() instanceof WDLDiagramType)
            return 20;
        return 0;
    }
    
    
    //////
    
    public static Object loadWDLGraph(File wdlFile)
    {
        return null;
//        Path path = DefaultPathBuilder.build(wdlFile.getAbsolutePath()).get();
//        Either<NonEmptyList<String>, WomBundle> e = WomGraphMaker.getBundle(path);
//        if(e.isLeft())
//        {
//            NonEmptyList<String> err = e.left().get();
//            log.warning( "Can not obtain wdl graph for " + wdlFile.getAbsolutePath() + ": " + err );
//            return new Graph( scala.collection.immutable.Set$.MODULE$.empty() );
//        }
//        WomBundle bundle = WomGraphMaker.getBundle(path).right().toOption().get();
//        ExecutableCallable exec = bundle.toExecutableCallable().right().toOption().get();
//        return exec.graph();
    }
}

