package biouml.standard.diagram;

import java.awt.Point;
import java.beans.IntrospectionException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ( "serial" )
public class SplitDiagramAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        return object instanceof Diagram && DiagramUtility.isComposite((Diagram)object);
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        if( !isApplicable(selectedItems) )
            return null;

        return new SplitDiagramActionParameters(selectedItems);
    }

    boolean isApplicable(List<DataElement> selectedItems)
    {
        return true;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {

                if( !isApplicable(selectedItems) )
                    return;

                doSplit((Diagram)model, StreamEx.of(selectedItems).map(d -> (DiagramElement)d).toList(),
                        (SplitDiagramActionParameters)properties);
            }
        };
    }

    public Diagram doSplit(Diagram diagram, List<DiagramElement> selectedItems, SplitDiagramActionParameters parameters)
    {
        try
        {
            DataCollection<?> origin = diagram.getOrigin();
            DataElementPath path = parameters.getResultPath();
            if( path != null )
            {
                DataCollectionUtils.createSubCollection(path.getParentPath(), true);
                origin = path.getParentPath().getDataCollection();
            }
            SemanticController controller = diagram.getType().getSemanticController();
            EModel emodel = diagram.getRole(EModel.class);

            String name = path == null ? parameters.getSubDiagramName() : path.getName();
            Diagram innerDiagram = diagram.getType().createDiagram( origin, name, new DiagramInfo( name ) );
            EModel innerEModel = innerDiagram.getRole(EModel.class);

            //nodes and edges selected for new diagram
            Set<Node> selectedNodes = StreamEx.of(selectedItems).select(Node.class).toSet();
            Set<Edge> selectedEdges = StreamEx.of(selectedItems).select(Edge.class).toSet();

            //if option is set - add all reactions for selected species
            if( parameters.isAutoIncludeReactions() )
            {
                Set<Node> excludedReactions = getReactions(diagram, selectedNodes);
                selectedNodes.addAll(excludedReactions);
            }

            //reaction participants that should be added to new diagram along with their reactions
            Set<Node> excludedParticipants = StreamEx.of(selectedNodes).filter(n -> Util.isReaction(n)).flatMap(n -> getParticipants(n))
                    .toSet();
            excludedParticipants.removeAll(selectedNodes); //these elements were not chosen manually hence they should stay in original diagram as well
            selectedNodes.addAll(excludedParticipants);

            //add all inner elements of selected compartments
            Set<Node> excludedContent = StreamEx.of(selectedNodes).flatMap(n -> getAllContent(n)).toSet();
            selectedNodes.addAll(excludedContent);

            //add edges (e.g. reaction edges) if both are already included
            selectedEdges
                    .addAll(diagram.recursiveStream().select(Edge.class).filter(e -> selectedNodes.containsAll(e.nodes().toSet())).toSet());


            Set<Node> excludedEquations = new HashSet<>();
            Set<String> excludedParameters = new HashSet<>();
            Set<Node> excludedVariables = new HashSet<>();
            fillExcludedParamsEquations(diagram, selectedNodes, excludedEquations, excludedParameters, excludedVariables);
            selectedNodes.addAll(excludedVariables);
            selectedNodes.addAll(excludedEquations);

            //add all compartments which contain selected elements. These compartments will be added without their content (besides explicitly selected)
            Set<Compartment> excludedCompartments = StreamEx.of(selectedNodes).flatMap(n -> getAllCompartments(n)).toSet();
            excludedCompartments.removeAll(selectedNodes);
            selectedNodes.addAll(excludedCompartments);

            //add all parameters used in selected reactions, equations etc.
            //            Set<String> excludedParameters = copyParameters(selectedNodes, diagram, innerDiagram);
            for( Variable var : StreamEx.of(excludedParameters).map(p -> emodel.getVariable(p)) )
                innerEModel.getVariables().put(var.clone(var.getName()));

            //used means that these elements should be preserved in original diagram as well because they are used in some elements which will not be moved to the new diagram
            Set<Node> usedNodes = new HashSet<>();
            Set<String> usedParameters = new HashSet<>();
            getUsedNodes(diagram, selectedNodes, selectedEdges, usedNodes, usedParameters);
            usedNodes.addAll(excludedParticipants);
            usedNodes.addAll(excludedCompartments);
            usedNodes.addAll(excludedVariables);

            //depth in diagram to nodes. Node has 0 depth if it is on the top level of diagram
            TreeMap<Integer, Set<Node>> nodeToLevel = new TreeMap<>();
            StreamEx.of(selectedNodes).forEach(n -> nodeToLevel.computeIfAbsent(getDepth(n), k -> new HashSet<>()).add(n));

            for( Entry<Integer, Set<Node>> entry : nodeToLevel.entrySet() )
            {
                for( Node node : entry.getValue() )
                    copyNode(node, innerDiagram);
            }

            //copy edges to new diagram
            for( Edge e : selectedEdges )
            {
                Compartment oldParent = e.getCompartment();
                Compartment newParent = oldParent.equals(diagram) ? innerDiagram
                        : (Compartment)innerDiagram.findNode(oldParent.getCompleteNameInDiagram());
                Edge newEdge = e.clone(newParent, e.getName());
                newParent.put(newEdge);

                if( parameters.isAddModule() )
                    oldParent.remove(e.getName());
            }

            SemanticController subDiagramController = innerDiagram.getType().getSemanticController();

            Set<String> calculatedInModule = getCalculatedVariables(innerDiagram, new HashSet<Node>());
            Set<String> calculatedInRest = getCalculatedVariables(diagram, selectedNodes);

            for( Node node : selectedNodes )
            {
                if( usedNodes.contains(node) )
                {

                    if (node.getKernel() instanceof biouml.standard.type.Compartment)
                        continue;

                    if( Util.isVariable(node) )
                    {
                        String varName = node.getRole( VariableRole.class ).getName();
                        boolean isCalculatedInModule = calculatedInModule.contains(varName);
                        boolean isCalculatedInRest = calculatedInRest.contains(varName);

                        String portType = "contact";
                        if( isCalculatedInModule )
                        {
                            if( isCalculatedInRest )
                                portType = "contact";
                            else
                                portType = "output";
                        }
                        else
                            portType = "input";

                        PortProperties portProperties = new PortProperties(innerDiagram, "contact");
                        portProperties.setVarName(varName);
                        portProperties.setName("port_" + node.getName());
                        portProperties.setPortType(portType);
                        subDiagramController.createInstance(innerDiagram, ConnectionPort.class, new Point(), portProperties);

                        if( parameters.isAddModule() )
                        {
                            portProperties = new PortProperties(diagram, "contact");
                            portProperties.setVarName(varName);
                            portProperties.setName("port_" + node.getName());
                            controller.createInstance(diagram, ConnectionPort.class, new Point(), portProperties);
                        }
                    }
                }
                else
                {
                    if( parameters.isAddModule() )
                        node.getCompartment().remove(node.getName());
                }
            }

            if( parameters.isAddModule() )
            {
                SubDiagram subDigaram = (SubDiagram)controller.createInstance(diagram, SubDiagram.class, getLocation(selectedItems),
                        innerDiagram ).getElement();
                diagram.put(subDigaram);

                for( Node port : subDigaram.getNodes() )
                {
                    Node topLevelPort = diagram.findNode(port.getName());
                    createConnection(diagram, port, topLevelPort);
                }
            }

            arrangePorts(innerDiagram);

            innerDiagram.save();
            return innerDiagram;
        }
        catch( Exception e )
        {
            log.severe( "Error during split: " + e.getMessage() );
            return null;
        }
    }

    private Set<String> getCalculatedVariables(Diagram diagram, Set<Node> exclude)
    {
        Set<Equation> eqs = StreamEx.of(diagram.recursiveStream()).select(Node.class).filter(n -> !exclude.contains(n))
                .map(n -> n.getRole()).select(Equation.class).toSet();
        Set<Equation> reactioneqs = StreamEx.of(diagram.recursiveStream()).select(Node.class)
                .filter(n -> !exclude.contains(n) && Util.isReaction(n)).flatCollection(n -> n.edges().toList()).map(e -> e.getRole())
                .select(Equation.class).toSet();

        return StreamEx.of(eqs).append(reactioneqs).map(eq->eq.getVariable()).toSet();
    }

    private static void copyNode(Node node, Diagram to)
    {
        Compartment oldParent = node.getCompartment();

        Compartment newParent = oldParent instanceof Diagram ? to : (Compartment)to.findNode(oldParent.getCompleteNameInDiagram());

        Node newNode;
        if( node.getKernel() instanceof biouml.standard.type.Compartment )
            newNode = cloneWithoutContent((Compartment)node, newParent, node.getName());
        else
            newNode = node.clone(newParent, node.getName());

        newParent.put(newNode);
        if( Util.isVariable(newNode) )
            to.getRole(EModel.class).getVariables().put((VariableRole)newNode.getRole());
    }

    private static Compartment cloneWithoutContent(Compartment comp, Compartment newParent, String newName)
    {
        Compartment result = new Compartment(newParent, newName, comp.getKernel());
        result.setShapeType(comp.getShapeType());
        result.setShapeSize(comp.getShapeSize());
        result.setVisible(comp.isVisible());

        result.setTitle(comp.getTitle());

        Role compRole = comp.getRole();
        if( compRole != null )
            result.setRole(compRole.clone(result));

        if( comp.getAttributes() != null )
        {
            Iterator<String> iter = comp.getAttributes().nameIterator();
            while( iter.hasNext() )
            {
                DynamicProperty oldProp = comp.getAttributes().getProperty(iter.next());
                DynamicProperty prop = null;
                try
                {
                    prop = DynamicPropertySetSupport.cloneProperty(oldProp);
                }
                catch( Exception e )
                {
                    prop = oldProp;
                }
                result.getAttributes().add(prop);
            }
        }

        result.setComment(comp.getComment());
        result.setLocation(comp.getLocation());
        result.setShapeSize(comp.getShapeSize());
        result.setFixed(comp.isFixed());
        result.setVisible(comp.isVisible());
        result.setShowTitle(comp.isShowTitle());
        result.setPredefinedStyle(comp.getPredefinedStyle());
        if( result.getPredefinedStyle().equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
            result.setCustomStyle(comp.getCustomStyle().clone());
        return result;
    }

    private Edge createConnection(Diagram diagram, Node port, Node otherNode)
    {
        String name = DefaultSemanticController.generateUniqueNodeName(diagram, "connection");
        Edge edge = new Edge(new Stub.UndirectedConnection(null, name), port, otherNode);
        Connection role = new UndirectedConnection(edge);
        String varName = Util.getPortVariable(port);
        role.setInputPort(new Connection.Port(varName, port.getTitle()));
        role.setOutputPort(new Connection.Port(varName, otherNode.getTitle()));
        edge.setRole(role);
        diagram.put(edge);
        return edge;
    }

    public static void getUsedNodes(Diagram diagram, Set<Node> selectedNodes, Set<Edge> selectedEdges, Set<Node> usedNodes,
            Set<String> usedVariables)
    {
        EModel emodel = diagram.getRole(EModel.class);

        Set<String> usedVariableRoles = emodel.getEquations()
                .filter(eq -> !selectedNodes.contains(eq.getDiagramElement()) || selectedEdges.contains(eq.getDiagramElement()))
                .flatCollection(eq -> Utils.getVariables(eq.getMath())).toSet();

        usedVariables.addAll(StreamEx.of(usedVariableRoles).filter(s -> !s.startsWith("$")).toSet());

        //used by edge
        usedNodes.addAll(StreamEx.of(selectedNodes).filter(n -> n.edges().anyMatch(e -> !selectedEdges.contains(e))).toSet());

        //is used as variable
        usedNodes.addAll(StreamEx.of(selectedNodes)
                .filter( n -> n.getRole() instanceof VariableRole && usedVariables.contains( n.getRole( VariableRole.class ).getName() ) )
                .toSet());
    }

    /**
     * Method return all equations which are used to calculate variables from given set except that from exclude set
     */
    public static Set<Node> getDefiningEquations(Set<String> variables, Diagram diagram, Set<Node> exclude)
    {
        return diagram.recursiveStream().select(Node.class).filter(n -> !exclude.contains(n)
                && n.getKernel().getType().equals( Type.MATH_EQUATION ) && variables.contains( n.getRole( Equation.class ).getVariable() ) )
                .toSet();
    }

    /**
     * Method returns all parameters used in selecetdNodes and selectedEdges ant not contained in set exclude
     */
    public static Set<String> getUsedParameters(Set<Node> selectedNodes, Diagram diagram, Set<String> exclude)
    {
        return StreamEx.of(selectedNodes).map(n -> n.getRole()).select(Equation.class)
                .flatCollection(eq -> StreamEx.of(Utils.getVariables(eq.getMath())).append(eq.getVariable()).toSet())
                .filter(p -> !p.startsWith("$$") && !exclude.contains(p)).toSet();
    }

    public static void fillExcludedParamsEquations(Diagram diagram, Set<Node> selectedNodes, Set<Node> equations, Set<String> parameters,
            Set<Node> variables)
    {
        Set<String> usedParameters = getUsedParameters(selectedNodes, diagram, parameters);

        while( !usedParameters.isEmpty() )
        {
            parameters.addAll(usedParameters);
            Set<Node> usedEquations = getDefiningEquations(usedParameters, diagram, equations);
            equations.addAll(usedEquations);
            usedParameters = getUsedParameters(usedEquations, diagram, parameters);
        }

        EModel emodel = diagram.getRole(EModel.class);
        Set<String> variableRoles = StreamEx.of(parameters).filter(p -> p.startsWith("$")).toSet();
        variables.addAll(StreamEx.of(variableRoles).map(n -> (Node) ( (VariableRole)emodel.getVariable(n) ).getDiagramElement()).toSet());
        variables.removeAll(selectedNodes);
        parameters.removeAll(variableRoles);
    }

    public static Integer getDepth(Node node)
    {
        int result = 0;
        Compartment compartment = node.getCompartment();
        while( ! ( compartment instanceof Diagram ) )
        {
            result++;
            compartment = compartment.getCompartment();
        }
        return result;
    }

    public static StreamEx<Node> getAllContent(Node node)
    {
        if( node instanceof Compartment )
            return StreamEx.of( ( (Compartment)node ).getNodes()).flatMap(n -> getAllContent(n)).append(node);
        else
            return StreamEx.of(node);
    }

    public static StreamEx<Compartment> getAllCompartments(Node node)
    {
        Set<Compartment> result = new HashSet<>();
        Compartment compartment = node.getCompartment();
        while( ! ( compartment instanceof Diagram ) )
        {
            result.add(compartment);
            compartment = compartment.getCompartment();
        }
        return StreamEx.of(result);
    }

    public static StreamEx<Node> getParticipants(Node reactionNode)
    {
        return reactionNode.edges().filter(e -> e.getKernel() instanceof SpecieReference).map(e -> e.getOtherEnd(reactionNode));
    }

    public static StreamEx<Node> getReactions(Node node)
    {
        return node.edges()
                .filter(e -> e.getKernel() instanceof SpecieReference && ( (SpecieReference)e.getKernel() ).isReactantOrProduct())
                .map(e -> e.getOtherEnd(node)).filter(n -> Util.isReaction(n) && ( (Reaction)n.getKernel() ).getSize() == 1);
    }

    public static Set<Node> getReactions(Diagram diagram, Set<Node> nodes)
    {
        return diagram.recursiveStream().select(Node.class)
                .filter(n -> Util.isReaction(n) && nodes.containsAll(getParticipants(diagram, ( (Reaction)n.getKernel() )))).toSet();
    }

    public static Set<Node> getParticipants(Diagram diagram, Reaction reaction)
    {
        return StreamEx.of(reaction.getSpecieReferences()).map(sr -> diagram.findNode(sr.getSpecie())).toSet();
    }

    private static Point getLocation(List<DiagramElement> elements)
    {
        List<Node> nodes = StreamEx.of(elements).select(Node.class).toList();
        if( nodes.isEmpty() )
            return new Point();
        Point result = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        for( Node node : nodes )
        {
            Point p = node.getLocation();
            result.x = Math.min(result.x, p.x);
            result.y = Math.min(result.y, p.y);
        }
        return result;
    }

    private static Set<String> copyParameters(Set<Node> nodes, Diagram from, Diagram to)
    {
        return StreamEx.of(nodes).map(n -> n.getRole()).select(Equation.class)
                .flatMap(eq -> StreamEx.of(Utils.getVariables(eq.getMath())).append(eq.getVariable())).filter(s -> !s.contains("$"))
                .toSet();
    }

    public static class SplitDiagramActionParameters
    {
        private String subDiagramName;
        private DataElementPath resultPath;
        private boolean autoIncludeReactions = false;
        private boolean addModule = false;

        @PropertyName ( "Module name" )
        @PropertyDescription ( "Module name." )
        public String getSubDiagramName()
        {
            return subDiagramName;
        }
        public void setSubDiagramName(String subDiagramName)
        {
            this.subDiagramName = subDiagramName;
        }

        @PropertyName ( "Add all reactions for selected species" )
        @PropertyDescription ( "Add all reactions for selected species." )
        public boolean isAutoIncludeReactions()
        {
            return autoIncludeReactions;
        }
        public void setAutoIncludeReactions(boolean autoIncludeReactions)
        {
            this.autoIncludeReactions = autoIncludeReactions;
        }

        @PropertyName ( "Add created diagram as a module" )
        @PropertyDescription ( "Add created diagram as a module." )
        public boolean isAddModule()
        {
            return addModule;
        }
        public void setAddModule(boolean addModule)
        {
            this.addModule = addModule;
        }

        public SplitDiagramActionParameters()
        {
            subDiagramName = "Module";
        }

        public SplitDiagramActionParameters(List<DataElement> elements)
        {
            if( !elements.isEmpty() )
            {
                Diagram diagram = Diagram.getDiagram( ( (DiagramElement)elements.get(0) ));
                subDiagramName = DefaultSemanticController.generateUniqueNodeName(diagram, "Module");
            }
            subDiagramName = "Module";
        }

        public DataElementPath getResultPath()
        {
            return resultPath;
        }
        public void setResultPath(DataElementPath resultPath)
        {
            this.resultPath = resultPath;
        }
    }

    public static class SplitDiagramActionParametersBeanInfo extends BeanInfoEx2<SplitDiagramActionParameters>
    {
        public SplitDiagramActionParametersBeanInfo()
        {
            super(SplitDiagramActionParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("subDiagramName");
            add("autoIncludeReactions");
            add("addModule");
        }
    }

    public static void arrangePorts(Diagram diagram)
    {
        SemanticController controller = diagram.getType().getSemanticController();
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, ApplicationUtils.getGraphics());
        List<Node> ports = diagram.recursiveStream().select(Node.class).filter(n -> Util.isPort(n)).toList();
        int x = diagram.stream(Node.class).map(n -> n.getView().getBounds()).mapToInt(b -> b.x + b.width).max().orElse(0) + 50;
        int y = 50;

        for( Node port : ports )
        {
            port.setLocation(new Point(x, y));
            y += 50;
            for( Edge e : port.edges() )
            {
                controller.recalculateEdgePath(e);
            }
        }
    }
}
