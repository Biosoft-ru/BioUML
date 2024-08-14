package biouml.plugins.hemodynamics;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class HemodynamicsDiagramGenerator
{
    private final Diagram diagram;
    private boolean addBranches;
    private boolean generatePorts;
    private double extentAreaFactor;
    private double outletAreaFactor;

    public HemodynamicsDiagramGenerator(DataCollection origin, String name) throws Exception
    {
        diagram = new HemodynamicsDiagramType().createDiagram(origin, name, new DiagramInfo(name));
    }

    public HemodynamicsDiagramGenerator(String name) throws Exception
    {
        this(null, name);
    }

    public HemodynamicsDiagramGenerator(Diagram diagram) throws Exception
    {
        this.diagram = diagram;
        diagram.clear();
    }


    public Diagram createDiagram(DataCollection collection, File file) throws Exception
    {
        return this.createDiagram(collection, loadFromFileNew(file));
    }

    public Diagram createDiagram(DataCollection collection, ArterialBinaryTreeModel model) throws Exception
    {
        if( addBranches )
            process(model);

        Node heartNode = new Node(diagram, new Stub(null, "heart", "heart"));
        diagram.put(heartNode);
        addVessel(heartNode, model.root);

        diagram.setRole(new HemodynamicsEModel(diagram));

        createEquation("systole", "piecewise( s <= T_D => 0; 1 )");
        createEquation("s", "mod(time, T_C)");
        createEquation("T_C", "60/HR");
        createEquation("T_D", "T_C-T_S");
        createEquation("a", "0.27*T_S");
        createEquation("arg", "3.1416925*piecewise( s < a => s/a - 0.5; s <= T_S => (s - a)/(T_S - a) + 0.5; 1.5 )");
        createEquation("inputFlow", "SV/T_S*(1 + sin(arg))");



//        createEquation("sh", "mod(time, 0.86)");
//        createEquation("inputPressure", "piecewise( sh < 0.46 => 80; 80 + 40*sin(3.14*(sh - 0.46)/0.4) )");
//        createEquation("inputFlow", "piecewise( sh < 0.4 => 300 - 375*sh; 0 )");

        if( generatePorts )
        {
            //feedback: integral parameters
            createPort("averagePressure", "P_A", Type.TYPE_OUTPUT_CONNECTION_PORT);
            createPort("totalVolume", "V_A", Type.TYPE_OUTPUT_CONNECTION_PORT);
            createPort("outputFlow", "Q_AC", Type.TYPE_OUTPUT_CONNECTION_PORT);
            createPort("arterialResistance", "R_A", Type.TYPE_OUTPUT_CONNECTION_PORT);

            //additional control
            createPort("vascularity", "vas", Type.TYPE_INPUT_CONNECTION_PORT);
            createPort("nueroReceptorsControl", "e_aum", Type.TYPE_INPUT_CONNECTION_PORT);


            //PP-condition:
            //        State state = new State(null, "PP-conditions");
            //        diagram.addState( state );
            //        diagram.setStateEditingMode( state );
            createPort("outputPressure", "P_out", Type.TYPE_INPUT_CONNECTION_PORT);
            createPort("inputPressure", "P_in", Type.TYPE_INPUT_CONNECTION_PORT);
            //        diagram.restore();

            //PQ-condition
            //        state = new State(null, "PQ-conditions");
            //        diagram.addState( state );
            //        diagram.setStateEditingMode( state );
            //        createPort("inputPressure", "P_in", Type.TYPE_INPUT_CONNECTION_PORT);
            createPort("outputFlow", "Q_out", Type.TYPE_INPUT_CONNECTION_PORT);
            //        diagram.restore();

            //QQ-condition
            //        state = new State(null, "QQ-conditions");
            //        diagram.addState( state );
            //        diagram.setStateEditingMode( state );
            createPort("inputFlow", "Q_in", Type.TYPE_INPUT_CONNECTION_PORT);
            //        createPort("outputFlow", "Q_out", Type.TYPE_INPUT_CONNECTION_PORT);
            //        diagram.restore();

            //QFi-condition
            //        state = new State(null, "QFi-conditions");
            //        diagram.addState( state );
            //        diagram.setStateEditingMode( state );
            //        createPort("inputFlow", "Q_in", Type.TYPE_INPUT_CONNECTION_PORT);
            createPort("venousPressure", "P_V", Type.TYPE_INPUT_CONNECTION_PORT);
            createPort("capillaryResistance", "R_C", Type.TYPE_INPUT_CONNECTION_PORT);
            //        diagram.restore();

            //PFi-condition
            //        state = new State(null, "PFi-conditions");
            //        diagram.addState( state );
            //        diagram.setStateEditingMode( state );
            //        createPort("inputPressure", "P_in", Type.TYPE_INPUT_CONNECTION_PORT);
            //        createPort("venousPressure", "P_V", Type.TYPE_INPUT_CONNECTION_PORT);
            //        createPort("capillaryResistance", "R_C", Type.TYPE_INPUT_CONNECTION_PORT);
            //        diagram.restore();
        }
        if( collection != null )
            collection.put(diagram);
        return diagram;
    }

    public void addRootVessel(Vessel vessel) throws Exception
    {
        BifurcationProperties properties = new BifurcationProperties(diagram);
        properties.setParentVesselName("Heart");
        properties.setVessel(vessel);
        DiagramElementGroup elements = properties.createElements( diagram, new Point(), null );
        elements.putToCompartment( );
    }

    public void addVessel(Vessel parent, Vessel vessel) throws Exception
    {
        BifurcationProperties properties = new BifurcationProperties(diagram);
        properties.setParentVesselName(parent.getTitle());
        properties.setVessel(vessel);
        DiagramElementGroup elements = properties.createElements( diagram, new Point(), null );
        elements.putToCompartment( );
    }

    public void addVessel(Node junction, SimpleVessel vessel) throws Exception
    {
        Node nextJunction = new Node(diagram, new Stub(null, getUniqueName("n"), HemodynamicsType.BIFURCATION));
        nextJunction.setTitle(vessel.getTitle());

        Edge edge = vesselToEdge(vessel, junction, nextJunction);
        diagram.put(nextJunction);
        diagram.put(edge);

//        DynamicProperty dp = new DynamicProperty("vesselRef", String.class, edge.getName());
//        dp.setReadOnly(true);
//        nextJunction.getAttributes().add(dp);

        if( vessel.left != null )
        {
            addVessel(nextJunction, vessel.left);
        }

        if( vessel.right != null )
        {
            addVessel(nextJunction, vessel.right);
        }
    }

    public Edge vesselToEdge(SimpleVessel vessel, Node from, Node to) throws Exception
    {
        try
        {
            String id = DefaultSemanticController.generateUniqueNodeName(diagram, "Vessel");
            Edge edge = new Edge(new Stub(null, id, HemodynamicsType.VESSEL), from, to);
            Vessel v = new Vessel(vessel.name, edge, vessel.length, vessel.unweightedArea, vessel.unweightedArea1, vessel.beta);
            v.setReferencedPressure(100);
            edge.getAttributes().add(new DynamicProperty("vessel", Vessel.class, v));
            return edge;
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return null;
        }
    }

    String getUniqueName(String base)
    {
        int index = 1;

        while( diagram.contains(base + index) )
        {
            index++;
        }

        return base + index;
    }

    public void createPort(String variableName, String title, String type) throws Exception
    {
        String name = getUniqueName("port");
        Node node = new Node(diagram, new Stub(null, name, type));
        node.setTitle(title);
        node.getAttributes().add(new DynamicProperty("direction", PortOrientation.class, PortOrientation.RIGHT));
        node.getAttributes().add(new DynamicProperty("portType", String.class, type));
        node.getAttributes().add(new DynamicProperty("xmlElementType", String.class, "port"));
        node.getAttributes().add(new DynamicProperty("variableName", String.class, variableName));
        diagram.put(node);
    }

    public void createDefaultEuations() throws Exception
    {
        createEquation("systole", "piecewise( s <= T_D => 0; 1 )");
        createEquation("s", "mod(time, T_C)");
        createEquation("T_C", "60/HR");
        createEquation("T_D", "T_C-T_S");
        createEquation("a", "0.27*T_S");
        createEquation("arg", "3.1416925*piecewise( s < a => s/a - 0.5; s <= T_S => (s - a)/(T_S - a) + 0.5; 1.5 )");
        createEquation("inputFlow", "SV/T_S*(1 + sin(arg))");
    }

    public void createEquation(String variableName, String formula) throws Exception
    {
        String name = getUniqueName("eq");
        Node node = new Node(diagram, new Stub(null, name, Type.MATH_EQUATION));
        Equation eq = new Equation(node, Equation.TYPE_SCALAR, variableName, formula);
        node.setRole(eq);

        EModel emodel =diagram.getRole(EModel.class);

//        if( !emodel.containsVariable(variableName) )
//            emodel.declareVariable(variableName, 0);
//
//        emodel.readMath(formula, eq);
        diagram.put(node);
    }

    public static ArterialBinaryTreeModel loadFromFileNew(File file) throws Exception
    {
        ArterialBinaryTreeModel atm = new ArterialBinaryTreeModel();
        readVessels(file, atm);
        return atm;
    }

    public static void readVessels(File file, ArterialBinaryTreeModel model) throws Exception
    {
        SimpleVessel rootVessel = null;
        Map<String, SimpleVessel> vessels = new HashMap<>();
        Map<String, List<SimpleVessel>> vesselChildren = new HashMap<>();
        try (BufferedReader br = ApplicationUtils.utfReader(file))
        {
            String s = br.readLine();

            while( s != null )
            {
                if( s.charAt(0) == '#' )
                {
                    s = br.readLine();
                    continue; //comment
                }

                String[] fields = s.split("(\t)+");

                double area = Double.parseDouble(fields[2]);
                double area1 = area;

                SimpleVessel vessel = new SimpleVessel(fields[0], fields[0], Double.parseDouble(fields[1]), area, area1,
                        Double.parseDouble(fields[3]));

//                if( fields.length == 6 )
//                {
                    String parentName = fields[4];
                    if (parentName.equals("ROOT"))
                        rootVessel = vessel;

                    if (!vesselChildren.containsKey(parentName))
                        vesselChildren.put(parentName, new ArrayList<SimpleVessel>());

                    List<SimpleVessel> children = vesselChildren.get(parentName);
                    children.add(vessel);
//                }
//                else
//                {
//                    if (rootVessel != null)
//                        throw new Exception("More then one root found");
//                    rootVessel = vessel;
//                }
                vessels.put(vessel.name, vessel);
                s = br.readLine();
            }

            if( rootVessel == null )
                throw new Exception("No root found");

            model.setRoot(rootVessel);

            List<SimpleVessel> children = vesselChildren.get(rootVessel.name);
            for( SimpleVessel v : children)
                add(model, v, rootVessel, vesselChildren );
        }
    }

    protected static void add(ArterialBinaryTreeModel model, SimpleVessel vessel, SimpleVessel parent, Map<String, List<SimpleVessel>> map )
    {
        model.addVessel(parent, vessel, parent.left == null);

        if (!map.containsKey(vessel.name))
            return;

        List<SimpleVessel> children = map.get(vessel.name);
        for (SimpleVessel child: children)
            add(model, child, vessel, map );
    }

    protected void process(ArterialBinaryTreeModel atm)
    {
        HashMap<String, SimpleVessel> newVessels = new HashMap<>();
        for( SimpleVessel v : atm.vessels )
        {
            SimpleVessel v1 = new SimpleVessel(v.name, v.title, v.length / 2, v.unweightedArea,  v.unweightedArea,v.beta);
            SimpleVessel v2 = new SimpleVessel(v.name + "_ext", v.title, v.length / 2, v.unweightedArea * extentAreaFactor,v.unweightedArea * extentAreaFactor, v.beta);
            SimpleVessel v3 = new SimpleVessel(v.name + "_out", v.title + "_out", v.length / 2, v.unweightedArea * outletAreaFactor, v.unweightedArea * outletAreaFactor, v.beta);
            v1.left = v2;
            v1.right = v3;
            newVessels.put(v1.name, v1);
            newVessels.put(v2.name, v2);
            newVessels.put(v3.name, v3);
        }

        atm.vesselMap.forEach( (vName, oldVessel) -> {
            SimpleVessel extent = newVessels.get( vName + "_ext" );
            if( oldVessel.left != null )
            {
                SimpleVessel newLeft = newVessels.get( oldVessel.left.name );
                extent.left = newLeft;
            }
            if( oldVessel.right != null )
            {
                SimpleVessel newRight = newVessels.get( oldVessel.right.name );
                extent.right = newRight;
            }
        } );

        atm.setRoot(newVessels.get(atm.root.name));

        atm.vessels.clear();
        atm.vesselMap.clear();

        atm.vessels.addAll(newVessels.values());
        atm.vesselMap.putAll(newVessels);
    }

    public void setAddBranches(boolean addBranches)
    {
        this.addBranches = addBranches;
    }

    public void setGeneratePorts(boolean generatePorts)
    {
        this.generatePorts = generatePorts;
    }

    public void setExtentAreaFactor(double extentAreaFactor)
    {
        this.extentAreaFactor = extentAreaFactor;
    }

    public void setOutletAreaFactor(double outletAreaFactor)
    {
        this.outletAreaFactor = outletAreaFactor;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }


}
