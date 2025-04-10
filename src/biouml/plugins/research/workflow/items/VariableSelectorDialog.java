package biouml.plugins.research.workflow.items;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.util.TextUtil2;
import biouml.model.Compartment;

import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.application.dialog.OkCancelDialog;

@SuppressWarnings ( "serial" )
public class VariableSelectorDialog extends OkCancelDialog
{
    protected Logger log = Logger.getLogger(VariableSelectorDialog.class.getName());
    private JTree tree = new JTree();
    private JTextField infoField = new JTextField();
    private JTextField pathField = new JTextField();
    private JScrollPane scrollPane = new JScrollPane(tree);
    private VariablesTreeModel model;
    private TreeCellRenderer renderer = new VariableTreeCellRenderer();

    public VariableSelectorDialog(JFrame frame, Compartment d)
    {
        super(frame, "Select variable");
        model = new VariablesTreeModel(d);
        tree.setRootVisible(true);
        tree.setModel(model);
        tree.setCellRenderer(renderer);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scrollPane.setPreferredSize(new Dimension(300, 400));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(scrollPane);
        content.add(new JLabel("Selected variable:"));
        content.add(pathField);
        content.add(new JLabel("Value:"));
        content.add(infoField);
        add(content);
        pathField.setEditable(false);
        infoField.setEditable(false);
        tree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2 && okButton.isEnabled())
                    okPressed();
            }
        });
        tree.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent event)
            {
                String selected = event.getPath().getLastPathComponent().toString();
                pathField.setText(selected);
                String value = "";
                try
                {
                    if(!selected.equals(""))
                    {
                        okButton.setEnabled(true);
                        Property property = model.getProperty(selected);
                        if(property != null)
                        {
                            Object valueObj = property.getValue();
                            if(valueObj != null)
                                value = valueObj.toString();
                        }
                    } else
                        okButton.setEnabled(false);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, e.getMessage(), e);
                    value = "Error: "+e.toString();
                }
                infoField.setText(value);
            }
        });
    }
    
    public String getSelectedVariable()
    {
        return tree.getSelectionPath().getLastPathComponent().toString();
    }

    private static class VariableTreeCellRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if(value.equals(""))
                setText("/");
            else
            {
                String[] fields = TextUtil2.split( value.toString(), '/' );
                setText(fields[fields.length-1]);
            }
            return this;
        }
    }
}
