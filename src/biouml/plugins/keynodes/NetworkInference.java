package biouml.plugins.keynodes;

import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.sbgn.extension.SbgnExDiagramType;
import biouml.plugins.sbgn.extension.SbgnExSemanticController;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Specie;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.GreedyLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@ClassIcon ( "resources/AddReactants.gif" )
public class NetworkInference extends AnalysisMethodSupport<NetworkInference.NetworkInferenceParameters>
{
    public NetworkInference(DataCollection<?> origin, String name)
    {
        super(origin, name, new NetworkInferenceParameters());
    }

    @Override
    public Diagram justAnalyzeAndPut() throws Exception //main method to perform analysis
    {
        //Here we create result diagram
        DataCollection origin = getParameters().getOutputDiagram().getParentCollection();
        String name = getParameters().getOutputDiagram().getName();
        Diagram diagram = new SbgnExDiagramType().createDiagram(origin, name,  new DiagramInfo(origin, name));

        //this class knows how to create diagram elements for current diagram properly
        SbgnExSemanticController controller = (SbgnExSemanticController)diagram.getType().getSemanticController();

        //an example of node creation
        Node node1 = (Node)controller.createInstance( diagram, Specie.class, "Node1", null, null ).getElement();
        diagram.put(node1);

        Node node2 = (Node)controller.createInstance( diagram, Specie.class, "Node2", null, null ).getElement();
        diagram.put(node2);


        //semantic relations are created directly for now (not through semantic controller)
        Edge e = new Edge(diagram, new SemanticRelation( null, "Edge1" ), node1, node2);
        e.getAttributes().add(new DynamicProperty("PropertyName", Double.class, 10.0)); //example how to add atributes to diagram element
        e.setTitle("");
        diagram.put(e);

        //get input table and names of its rows
        TableDataCollection table = getParameters().getInputTable().getDataElement(TableDataCollection.class);
        List<String> rowNames = table.getNameList();

        //iterating throw table rows
        for (String rowName: rowNames)
        {
            //create elements here
            //...

            //in the next line we retrieve correspinding row
            Object[] values = TableDataCollectionUtils.getRowValues(table, rowName);

        }

        layout(diagram);
        return diagram;
    }

    //method to layout diagram
    private void layout(Diagram diagram)
    {
        Layouter layouter = new GreedyLayouter();
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        pathwayLayouter.doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
    }

    //parameters class
    @SuppressWarnings ( "serial" )
    public static class NetworkInferenceParameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTable, outputDiagram;

        @PropertyName ( "Input table" )
        @PropertyDescription ( "Input table" )
        public DataElementPath getInputTable()
        {
            return inputTable;
        }
        public void setInputTable(DataElementPath inputTable)
        {
            Object oldValue = this.inputTable;
            this.inputTable = inputTable;
            firePropertyChange("inputTable", oldValue, inputTable);

        }

        @PropertyName ( "Output diagram" )
        @PropertyDescription ( "Output diagram" )
        public DataElementPath getOutputDiagram()
        {
            return outputDiagram;
        }
        public void setOutputDiagram(DataElementPath outputDiagram)
        {
            Object oldValue = this.outputDiagram;
            this.outputDiagram = outputDiagram;
            firePropertyChange("outputDiagram", oldValue, outputDiagram);
        }
    }

    //class to support parameters visual representation to user
    public static class NetworkInferenceParametersBeanInfo extends BeanInfoEx2<NetworkInferenceParameters>
    {
        public NetworkInferenceParametersBeanInfo()
        {
            super(NetworkInferenceParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("inputTable").inputElement(TableDataCollection.class).add(); //registered as input
            property("outputDiagram").outputElement(Diagram.class).auto("$inputTable$ viz").add(); //registred as main output + name autogeneration
        }
    }
}
