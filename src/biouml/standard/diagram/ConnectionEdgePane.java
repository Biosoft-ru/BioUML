package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import one.util.streamex.StreamEx;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.support.IdGenerator;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.OkCancelDialog;
import ru.biosoft.util.Pair;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.MultipleDirectedConnection;
import biouml.model.dynamics.MultipleUndirectedConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Base;
import biouml.standard.type.Specie;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.ContactConnectionPort;
import biouml.standard.type.Stub.InputConnectionPort;
import biouml.standard.type.Stub.OutputConnectionPort;

/**
 * Creates connections between composite diagram elements.
 * Acceptable edges:
 * - diagram to diagram (if not nested)
 * - port to port
 * - port to bus,
 * - bus to port
 *
 */
@SuppressWarnings ( "serial" )
public class ConnectionEdgePane extends SemanticRelationPane
{
    protected Stub edgeStub;
    protected Class<?> connectionType;
    protected boolean directedConnection;
    protected Compartment parent;

    public ConnectionEdgePane(Module module, ViewEditorPane viewEditor, Class<?> connectionType, Compartment parent)
    {
        super(module, viewEditor);
        this.connectionType = connectionType;
        directedConnection = Stub.DirectedConnection.class.isAssignableFrom(connectionType);
        this.parent = parent;
    }

    @Override
    public boolean createRelation()
    {
        try
        {
            String stubName = IdGenerator.generateUniqueName(parent, new DecimalFormat("CONNECTION0000"));
            Constructor<?> c = connectionType.getConstructor( ru.biosoft.access.core.DataCollection.class, String.class );
            directedConnection = Stub.DirectedConnection.class.isAssignableFrom(connectionType);
            Stub edgeStub = (Stub)c.newInstance(new Object[] {null, stubName});

            this.edgeStub = edgeStub;
            return true;
        }
        catch( Throwable t )
        {
            JOptionPane.showMessageDialog(this, "Exception: " + t, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    @Override
    protected void createEdge()
    {
        try
        {
            Node inNode = (Node)inSelector.getDiagramElement();
            Node outNode = (Node)outSelector.getDiagramElement();

            Compartment origin = Node.findCommonOrigin(inNode, outNode);

            //valid
            if( inNode.equals(outNode) )
            {
                JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Connections for the same element are not allowed.");
                return;
            }

            Edge edge = null;

            //diagram to diagram or diagram to plot cases
            if( Util.isSubDiagram(inNode) && ( Util.isSubDiagram(outNode) || Util.isPlot(outNode) ) )
            {
                edge = createMultipleConnection(inNode, outNode, origin);
            }
            else
            {
                // port to port, port to bus, bus to port, bus to plot, constant to bus,port or plot
                edge = createConnection(inNode, outNode);
            }

            if( edge != null )
            {
                viewEditor.startTransaction("Add");
                origin.put(edge);
                viewEditor.completeTransaction();
            }
        }
        catch( Throwable t )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Can not create Edge for connection '" + edgeStub.getName()
                    + "'");
        }
    }

    /**
     * Creates connection edge inNode must be diagram, outNode can be diagram or plot
     * @param inNode
     * @param outNode
     * @param origin
     * @return
     * @throws Exception
     */
    private Edge createMultipleConnection(Node inNode, Node outNode, Compartment origin) throws Exception
    {
        //valid
        if( inNode.equals(origin) || outNode.equals(origin) )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Connections for main diagram are not allowed");
            return null;
        }
        else if( Util.isPlot(outNode) && !directedConnection )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Undirected connections are not allowed for plot");
            return null;
        }

        Edge oldEdge = getEdgeIfAlreadyExists(inNode, outNode, origin, connectionType);

        ParametersConnectionDialog dialog = new ParametersConnectionDialog(null, "Establish parameters connections", inNode, outNode,
                oldEdge);

        if( !dialog.doModal() )
            return null;

        Set<Pair<String, String>> connections = dialog.getConnections();

        Edge newEdge = makeDiagramConnectionEdge(oldEdge, inNode, outNode, origin, connections);

        if( oldEdge != null )
        {
            newEdge.setPath(oldEdge.getPath());

            viewEditor.startTransaction("Remove");
            origin.remove(oldEdge.getName());
            viewEditor.completeTransaction();
        }
        return newEdge;
    }

    //for webDiagramsProvider
    public Edge makeDiagramConnectionEdge(Edge oldEdge, @Nonnull Node inNode, @Nonnull Node outNode, Compartment origin,
            Set<Pair<String, String>> connections) throws Exception
    {
        if( connections == null || connections.size() == 0 )
            return null;

        Edge newEdge = new Edge(origin, edgeStub, inNode, outNode);

        MultipleConnection role = ( directedConnection ) ? new MultipleDirectedConnection(newEdge) : new MultipleUndirectedConnection(
                newEdge);
        role.setInputPort(new Connection.Port(inNode));
        role.setOutputPort(new Connection.Port(outNode));

        StringBuffer title = new StringBuffer();

        for( Pair<String, String> pair : connections )
        {
            String intitle = getConnectionTitle(inNode, pair.getFirst());
            String outtitle = getConnectionTitle(outNode, pair.getSecond());

            if( title.length() != 0 )
                title.append(", ");
            title.append(intitle);


            if( directedConnection )
            {
                DirectedConnection dc = new DirectedConnection(newEdge);
                dc.setInputPort(new Connection.Port(pair.getFirst(), intitle));
                dc.setOutputPort(new Connection.Port(pair.getSecond(), outtitle));
                role.addConnection(dc);
            }
            else
            {
                UndirectedConnection udc = new UndirectedConnection(newEdge);
                udc.setInputPort(new Connection.Port(pair.getFirst(), intitle));
                udc.setOutputPort(new Connection.Port(pair.getSecond(), outtitle));
                role.addConnection(udc);
            }
        }

        newEdge.setRole(role);
        newEdge.setTitle(title.toString());
        if( oldEdge != null )
        {
            newEdge.setPath(oldEdge.getPath());
        }

        return newEdge;
    }

    /**
     * Creates connection
     * @param inNode
     * @param outNode
     * @return
     */
    public Edge createConnection(Node inNode, Node outNode)
    {
        boolean valid = false;

        if( Util.isBus(inNode) && Util.isBus(outNode) )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Connection between buses is not allowed");
            return null;
        }

        if (Util.isDiagram(inNode) || Util.isDiagram(outNode))
        {
            JOptionPane.showMessageDialog( Application.getApplicationFrame(), "Connection between selected elements is not allowed." );
            return null;
        }

        if ((inNode.getKernel() instanceof Specie && !Util.isPort(outNode)) || outNode.getKernel() instanceof Specie && !Util.isPort(inNode))
        {
            JOptionPane.showMessageDialog( Application.getApplicationFrame(), "Connection between selected elements is not allowed." );
            return null;
        }

        if( ( !Util.isModulePort( inNode ) && Util.isPropagatedPort2( inNode ) )
                || ( !Util.isModulePort( outNode ) && Util.isPropagatedPort2( outNode ) ) )
        {
            JOptionPane.showMessageDialog( Application.getApplicationFrame(), "Propagated ports can not be used for connections." );
            return null;
        }

        if( ( !Util.isModulePort( inNode ) && Util.isPublicPort( inNode ) )
                || ( !Util.isModulePort( outNode ) && Util.isPublicPort( outNode ) ) )
        {
            JOptionPane.showMessageDialog( Application.getApplicationFrame(), "Public ports can not be used for connections." );
            return null;
        }

        if( directedConnection && canBeInput(inNode) && canBeOutput(outNode) )
        {
            valid = true;
        }
        else if( !directedConnection && canBeContact(inNode) && canBeContact(outNode) )
        {
            if( Util.isOutputPort( inNode ) && Util.isOutputPort( outNode ) )
                valid = false;
            valid = true;
        }

        if( !valid )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Can not create connection between " + inNode.getName()
                    + " and " + outNode.getName());
            return null;
        }
        Edge edge = new Edge(edgeStub, inNode, outNode);
        Connection role = ( directedConnection ) ? new DirectedConnection(edge) : new UndirectedConnection(edge);
        role.setInputPort(new Connection.Port(getPortVariableName(inNode), inNode.getTitle()));
        role.setOutputPort(new Connection.Port(getPortVariableName(outNode), outNode.getTitle()));
        edge.setRole(role);

        return edge;
    }

    public static String getPortVariableName(Node node)
    {
        String name = node.getName();
        if( Util.isPort(node) )
        {
            if( node.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR) != null )
            {
                name = (String)node.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR).getValue();
            }
        }
        else if( node.getRole() instanceof VariableRole )
        {
            name = node.getRole( VariableRole.class ).getName();
        }

        return name.replace("_port", "");
    }



    protected String getConnectionTitle(Node node, String name)
    {
        if (node instanceof SubDiagram)
            node = ((SubDiagram)node).getDiagram();
        Diagram d = Diagram.getDiagram(node);
        if( d.getRole() instanceof EModel )
        {
            EModel model = d.getRole( EModel.class );
            Variable var = model.getVariable(name);
            if( var instanceof VariableRole )
            {
                return ( (VariableRole)var ).getDiagramElement().getTitle();
            }
        }
        return name;
    }

    public static Edge getEdgeIfAlreadyExists(Node inDiagram, Node outDiagram, Compartment origin, Class<?> connectionType)
    {
        for(Edge edge : origin.stream( Edge.class ))
        {
            Role role = edge.getRole();
            if( role instanceof MultipleConnection )
            {
                Connection con = (Connection)role;
                if( con.getInputPort().getVariableName().equals(inDiagram.getName())
                        && con.getOutputPort().getVariableName().equals(outDiagram.getName()) )
                {
                    if( ( role instanceof MultipleDirectedConnection )
                            && Stub.DirectedConnection.class.isAssignableFrom(connectionType)
                            || ( role instanceof MultipleUndirectedConnection )
                            && Stub.UndirectedConnection.class.isAssignableFrom(connectionType) )
                        return edge;
                }
            }
        }
        return null;
    }

    public String[][] getExistedConnections(Edge edge)
    {
        if( edge == null )
            return null;
        String[][] existedConnections = new String[1][2];
        Role edgeRole = edge.getRole();
        int counter = 0;
        if( edgeRole instanceof MultipleConnection )
        {
            Connection[] dcList = ( (MultipleConnection)edgeRole ).getConnections();
            existedConnections = new String[dcList.length][2];
            for( Connection dc : dcList )
            {
                existedConnections[counter][0] = dc.getInputPort().getVariableName();
                existedConnections[counter][1] = dc.getOutputPort().getVariableName();
                counter++;
            }
        }
        return existedConnections;
    }

    private class ParametersConnectionDialog extends OkCancelDialog
    {
        private JTable inTable;
        private JTable outTable;
        private final JTable connectionTable;
        private Node notDiagramNode;

        public ParametersConnectionDialog(Component parent, String title, Node input, Node output, Edge edge) throws Exception
        {
            super(parent, title);

            if (input instanceof SubDiagram)
                input = ((SubDiagram)input).getDiagram();

            if (output instanceof SubDiagram)
                output = ((SubDiagram)output).getDiagram();

            boolean twoDiagramFlag = ( input instanceof Diagram && output instanceof Diagram );
            if( !twoDiagramFlag )
                notDiagramNode = ( input instanceof Diagram ) ? output : input; // ConnectionPort or Plot

            JPanel mainPanel = new JPanel();
            mainPanel.setPreferredSize(new Dimension(500, 315));
            mainPanel.setLayout(new GridBagLayout());

            int anchor = GridBagConstraints.CENTER;
            Insets insets = new Insets(5, 5, 5, 5);

            String[] resultColumnNames = {"From", "To"};
            String[][] resultData = getExistedConnections(edge);
            DefaultTableModel model = new DefaultTableModel(resultData, resultColumnNames);
            connectionTable = new JTable(model);
            connectionTable.setPreferredScrollableViewportSize(new Dimension(300, 110));
            connectionTable.setFillsViewportHeight(true);
            JScrollPane pane = new JScrollPane(connectionTable);
            mainPanel.add(pane, new GridBagConstraints(0, 0, 2, 1, 1, 1, anchor, GridBagConstraints.BOTH, insets, 0, 0));

            JButton addButton = new JButton("Add");
            ActionListener actionListener = new AddActionListener();
            addButton.addActionListener(actionListener);
            mainPanel.add(addButton, new GridBagConstraints(0, 1, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));

            JButton deleteButton = new JButton("Remove");
            ActionListener deleteActionListener = new DeleteActionListener();
            deleteButton.addActionListener(deleteActionListener);
            mainPanel.add(deleteButton, new GridBagConstraints(1, 1, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));



            if( input instanceof Diagram )
            {
                String[] inColumnNames = {input.getName() + " parameters"};
                Object[] inParameters = PortProperties.getParameters((Diagram)input);
                Object[][] inData = StreamEx.of( inParameters ).map( p -> new Object[] {p} ).toArray( Object[][]::new );
                model = new DefaultTableModel(inData, inColumnNames);
                inTable = new JTable(model);
                inTable.setPreferredScrollableViewportSize(new Dimension(150, 120));
                inTable.setFillsViewportHeight(true);

                pane = new JScrollPane(inTable);
                mainPanel.add(pane, new GridBagConstraints(0, 2, twoDiagramFlag ? 1 : 2, 1, 1, 1, anchor, GridBagConstraints.BOTH, insets,
                        0, 0));
            }

            if( output instanceof Diagram )
            {
                String[] outColumnNames = {output.getName() + " parameters"};
                Object[] outParameters = PortProperties.getParameters((Diagram)output);
                Object[][] outData = StreamEx.of( outParameters ).map( p -> new Object[] {p} ).toArray( Object[][]::new );
                model = new DefaultTableModel(outData, outColumnNames);
                outTable = new JTable(model);
                outTable.setPreferredScrollableViewportSize(new Dimension(150, 120));
                outTable.setFillsViewportHeight(true);

                pane = new JScrollPane(outTable);
                mainPanel.add(pane, new GridBagConstraints(twoDiagramFlag ? 1 : 0, 2, twoDiagramFlag ? 1 : 2, 1, 1, 1, anchor,
                        GridBagConstraints.BOTH, insets, 0, 0));
            }

            add(mainPanel, BorderLayout.CENTER);
        }

        public class AddActionListener implements ActionListener
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int inRowIndex = -1;
                int outRowIndex = -1;
                Object[] newLine = new Object[2];
                if( inTable != null )
                    inRowIndex = inTable.getSelectedRow();
                if( outTable != null )
                    outRowIndex = outTable.getSelectedRow();

                if( inRowIndex != -1 && outRowIndex != -1 )
                {
                    Object fromName = inTable.getModel().getValueAt(inRowIndex, 0);
                    Object toName = outTable.getModel().getValueAt(outRowIndex, 0);
                    newLine[0] = fromName;
                    newLine[1] = toName;
                }
                else if( inRowIndex != -1 )
                {
                    Object fromName = inTable.getModel().getValueAt(inRowIndex, 0);
                    Object toName = "";
                    if( notDiagramNode != null && Util.isPort(notDiagramNode) )
                    {
                        toName = notDiagramNode.getName();
                    }
                    else if( notDiagramNode != null && Util.isPlot(notDiagramNode) )
                    {
                        toName = fromName;
                    }

                    newLine[0] = fromName;
                    newLine[1] = toName;
                }
                else if( outRowIndex != -1 )
                {
                    Object toName = outTable.getModel().getValueAt(outRowIndex, 0);
                    Object fromName = "";
                    if( notDiagramNode != null )
                        fromName = notDiagramNode.getName();
                    newLine[0] = fromName;
                    newLine[1] = toName;
                }

                if( newLine[0].toString().length() > 0 && newLine[1].toString().length() > 0 )
                {
                    if( connectionTable.getModel().getRowCount() < 1 )
                        ( (DefaultTableModel)connectionTable.getModel() ).addRow(newLine);
                    else if( connectionTable.getModel().getValueAt(0, 0) == null
                            || connectionTable.getModel().getValueAt(0, 0).toString().length() < 1 )
                    {
                        ( (DefaultTableModel)connectionTable.getModel() ).setValueAt(newLine[0], 0, 0);
                        ( (DefaultTableModel)connectionTable.getModel() ).setValueAt(newLine[1], 0, 1);
                    }
                    else
                        ( (DefaultTableModel)connectionTable.getModel() ).addRow(newLine);
                }
            }
        }

        public class DeleteActionListener implements ActionListener
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if( connectionTable.getModel() != null && connectionTable.getModel().getRowCount() > 0
                        && connectionTable.getSelectedRow() != -1 )
                {
                    int[] columnsNumbers = connectionTable.getSelectedRows();
                    Arrays.sort(columnsNumbers);
                    for( int i = columnsNumbers.length; i > 0; i-- )
                    {
                        int deleteIndex = columnsNumbers[i - 1];
                        ( (DefaultTableModel)connectionTable.getModel() ).removeRow(deleteIndex);
                    }
                }
            }
        }

        public Set<Pair<String, String>> getConnections()
        {
            Set<Pair<String, String>> connections = new HashSet<>();
            if( connectionTable != null && connectionTable.getRowCount() > 0 )
            {
                for( int i = 0; i < connectionTable.getRowCount(); i++ )
                {
                    String left = connectionTable.getValueAt(i, 0).toString();
                    String right = connectionTable.getValueAt(i, 1).toString();
                    connections.add( new Pair<>( left, right ) );
                }
            }
            return connections;
        }

    }

    @Override
    public void release()
    {
        viewEditor.setSelectionEnabled(true);
        viewEditor.removeViewPaneListener(adapter);
    }

    @Override
    public void okPressed()
    {
        createEdge();
        release();
    }

    @Override
    protected void cancelPressed()
    {
        release();
    }

    protected boolean canBeInput(Node node)
    {
        Base kernel = node.getKernel();
        if( kernel == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Node " + node.getName() + " has no kernel");
            return false;
        }

        if( Util.isContactPort( node ) || Util.isOutputPort( node ) )
        {
            if( node.edges().anyMatch( e -> Util.isConnection( e ) ) )
            {
                JOptionPane.showMessageDialog( Application.getApplicationFrame(),
                        "Only one connection per port is allowed. Use buses for more connections." );
                return false;
            }
            else
                return true;
        }

        if( kernel instanceof Stub.Constant )
            return true;

        //Bus can be input of directed connection if it has no undirected connections
        else if( Util.isBus( node ) )
        {
            for( Edge edge : node.getEdges() )
            {
                Role role = edge.getRole();
                if( role instanceof UndirectedConnection )
                {
                    JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Bus " + node.getName()
                            + " already has undirected connections");
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected boolean canBeOutput(Node node)
    {
        Base kernel = node.getKernel();
        if( kernel == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Node " + node.getName() + " has no kernel");
            return false;
        }

        if( Util.isContactPort( node ) || Util.isInputPort( node ) )
        {
            if( node.edges().anyMatch( e -> Util.isConnection( e ) ) )
            {
                JOptionPane.showMessageDialog( Application.getApplicationFrame(),
                        "Only one connection per port is allowed. Use buses for more connections." );
                return false;
            }
            else
                return true;
        }

        //Bus or input connection port can be output of directed connection if it has no undirected connections and is not output node for any directed connection
        else if( Util.isBus( node ) )
        {
            for( Edge edge : node.getEdges() )
            {
                Role role = edge.getRole();
                if( role instanceof UndirectedConnection )
                {
                    JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Bus " + node.getName()
                            + " has undirected connections. No directed connections allowed");
                    return false;
                }
                else if( role instanceof DirectedConnection && edge.getOutput().equals(node) )
                {
                    JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Node " + node.getName()
                            + "already has incoming directed conection");
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    protected boolean canBeContact(Node node)
    {
        Base kernel = node.getKernel();
        if( kernel == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Node " + node.getName() + " has no kernel");
            return false;
        }

        if( kernel instanceof ConnectionPort )
        {
            if( node.edges().anyMatch( e -> Util.isConnection( e ) ) )
            {
                JOptionPane.showMessageDialog( Application.getApplicationFrame(),
                        "Only one connection per port is allowed. Use buses for more connections." );
                return false;
            }
            else
                return true;
        }

        if( kernel instanceof Specie )
            return true;
        //Bus can be part of undirected connection if it has no directed connections
        else if( Util.isBus( node ) )
        {
            for( Edge edge : node.getEdges() ) //check all nodes
            {
                if( edge.getRole() instanceof DirectedConnection )
                {
                    JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Bus " + node.getName()
                            + " has directed connections. No undirected connections allowed");
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
