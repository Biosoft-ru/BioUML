package biouml.plugins.microarray;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.type.Base;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class BindingTab extends JPanel
{
    protected Logger log = Logger.getLogger( BindingTab.class.getName() );

    public static final String DEFAULT_BINDER = "none";

    protected JComboBox<String> microarray = new JComboBox<>();
    protected JTable bindingTable = new JTable();

    protected JComboBox<String> geneHub = new JComboBox<>();
    protected Map<String, BioHubRegistry.BioHubInfo> bioHubMap = null;

    protected Diagram diagram;

    protected MessageBundle messageBundle = new MessageBundle();

    public static final String DATA_ELEMEMT = "Data element";
    public static final String TITLE = "Title";
    public static final String EXPERIMENT = "Experiment";
    public static final String COMMENT = "Comment";

    public BindingTab()
    {
        setLayout(new GridBagLayout());

        JLabel text = new JLabel(messageBundle.getResourceString("MICROARRAY_LIST_LABEL"));
        add(text, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
                0));

        DataCollection<?> microarrays = CollectionFactory.getDataCollection("data/microarray");
        if( microarrays != null )
        {
            microarrays.names().forEach( microarray::addItem );
        }

        microarray.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                String selected = (String)microarray.getSelectedItem();
                microarray.removeAllItems();
                DataCollection<?> microarrays = CollectionFactory.getDataCollection("data/microarray");
                if( microarrays != null )
                {
                    microarrays.names().forEach( microarray::addItem );
                }
                microarray.setSelectedItem(selected);
            }
        });

        add(microarray, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,
                0, 0, 0), 0, 0));

        text = new JLabel(messageBundle.getResourceString("BINDER_LIST_LABEL"));
        add(text, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
                0));

        geneHub.addItem(DEFAULT_BINDER);
        bioHubMap = BioHubRegistry.getBioHubs();
        for(String hubName : bioHubMap.keySet())
        {
            geneHub.addItem(hubName);
        }

        add(geneHub, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0,
                0, 0), 0, 0));

        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> refreshBindingTable());
        add(apply, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0,
                0), 0, 0));

        bindingTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(bindingTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, new GridBagConstraints(0, 1, 5, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0,
                0), 0, 0));
    }

    public void refreshDiagram(Diagram diagram)
    {
        this.diagram = diagram;
        refreshBindingTable();
    }

    public void refreshBindingTable()
    {
        if( diagram != null )
        {
            BioHub hub = null;
            String selectedHub = (String)geneHub.getSelectedItem();
            if( !selectedHub.equals(DEFAULT_BINDER) )
            {
                hub = bioHubMap.get(selectedHub).getBioHub();
            }
            new Binder(hub).bindDiagram(diagram, (String)microarray.getSelectedItem());
        }

        BindingTableModel btm = new BindingTableModel();
        bindingTable.setModel(btm);
    }

    public String getSelectedMicroarray()
    {
        return (String)microarray.getSelectedItem();
    }

    private class BindingTableModel extends AbstractTableModel
    {
        private Node nodes[] = null;
        public BindingTableModel()
        {
            try
            {
                nodes = diagram.recursiveStream().select( Node.class ).without( diagram ).toArray( Node[]::new );
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't get diagram nodes", t);
            }
        }
        @Override
        public int getColumnCount()
        {
            return 4;
        }
        @Override
        public int getRowCount()
        {
            try
            {
                if( nodes != null )
                {
                    return nodes.length;
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't get diagram nodes", t);
            }
            return 0;
        }
        @Override
        public Object getValueAt(int row, int col)
        {
            try
            {
                if( col == 0 )
                {
                    Base kernel = nodes[row].getKernel();
                    return ru.biosoft.access.core.DataElementPath.escapeName(kernel.getType()) + "/" + ru.biosoft.access.core.DataElementPath.escapeName(kernel.getName());
                }
                else if( col == 1 )
                {
                    return nodes[row].getKernel().getTitle();
                }
                else if( col == 2 )
                {
                    String result = "";
                    MicroarrayLink maLink = (MicroarrayLink)nodes[row].getAttributes().getValue("maLink");
                    if( maLink != null )
                    {
                        result = String.join( ", ", maLink.getGenes() );
                    }
                    return result;
                }
                else if( col == 3 )
                {
                    MicroarrayLink maLink = (MicroarrayLink)nodes[row].getAttributes().getValue("maLink");
                    if( maLink != null )
                    {
                        return maLink.getComment();
                    }
                    return "";
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't get diagram nodes", t);
            }
            return "";
        }

        @Override
        public void setValueAt(Object value, int row, int col)
        {
            /*if( col == 1 )
             {
             if( "true".equalsIgnoreCase(value.toString()) )
             {
             experimentsUse.set(row, true);
             }
             else
             {
             experimentsUse.set(row, false);
             }
             }*/
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return false;
        }

        @Override
        public String getColumnName(int col)
        {
            if( col == 0 )
            {
                return DATA_ELEMEMT;
            }
            else if( col == 1 )
            {
                return TITLE;
            }
            else if( col == 2 )
            {
                return EXPERIMENT;
            }
            else if( col == 3 )
            {
                return COMMENT;
            }
            return "";
        }
    }
}
