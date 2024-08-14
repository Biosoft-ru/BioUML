package biouml.plugins.optimization.access;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathField;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.standard.state.State;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

@SuppressWarnings ( "serial" )
public class NewOptimizationExperimentDialog extends OkCancelDialog
{
    protected Logger log = Logger.getLogger(NewOptimizationDialog.class.getName());

    protected Diagram diagram;
    protected List<OptimizationExperiment> experiments;

    protected JTextField nameField = new JTextField();
    protected JComboBox<String> diagramStateBox = new JComboBox<>();
    protected DataElementPathField experimentPath = null;

    public NewOptimizationExperimentDialog(Diagram diagram, List<OptimizationExperiment> experiments)
    {
        super(Application.getApplicationFrame(), "New optimization experiment");

        this.diagram = diagram;
        this.experiments = experiments;

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        contentPane.add(new JLabel("Name: "), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(nameField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        contentPane.add(new JLabel("Diagram state: "), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(diagramStateBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        contentPane.add(new JLabel("File: "), new GridBagConstraints(0, 2, 1, 1, 0.0,
                0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        DataElementPath defaultPath = null;
        if( experiments.size() > 0 )
        {
            defaultPath = DataElementPath.create(experiments.get(0).getFilePath().optParentCollection(), "");
        }
        experimentPath = new DataElementPathField("experimentPath", TableDataCollection.class, "(click to select an experimental file)", defaultPath, true);
        contentPane.add(experimentPath, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        setContent(contentPane);

        initExperimentName();
        initDiagramStatesBox();
    }

    protected void initDiagramStatesBox()
    {
        List<String> names = getDiagramStateNames();
        diagramStateBox.removeAllItems();
        diagramStateBox.addItem("no state");
        if( names != null )
        {
            for( String diagramStateName : names )
            {
                diagramStateBox.addItem(diagramStateName);
            }
        }
    }

    protected void initExperimentName()
    {
        String newName = "experiment_";
        Set<String> used = new HashSet<>();
        for( OptimizationExperiment exp : experiments )
        {
            used.add(exp.getName());
        }
        int cnt = 1;
        while( used.contains(newName + cnt) )
        {
            cnt++;
        }
        nameField.setText(newName + cnt);
    }

    @Override
    protected void okPressed()
    {
        String name = nameField.getText();
        if( ( name != null ) && ( name.length() > 0 ) )
        {
            try
            {
                for( OptimizationExperiment exp : experiments )
                {
                    if( exp.getName().equals(name) )
                    {
                        String message = MessageFormat.format(MessageBundle.getMessage("WARN_EXP_EXISTENCE"), new Object[] {name});
                        int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
                        if( res != JOptionPane.YES_OPTION )
                            return;

                        experiments.remove(exp);
                        break;
                    }
                }

                OptimizationExperiment newExp = new OptimizationExperiment(name, experimentPath.getPath());
                newExp.setDiagram(diagram);
                newExp.setDiagramStateName((String)diagramStateBox.getSelectedItem());

                experiments.add(newExp);

                super.okPressed();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_OPTIMIZATION_EXP_CREATION"), t);
            }
        }
    }

    private List<String> getDiagramStateNames()
    {
        if( diagram != null )
        {
            return diagram.states().map( State::getName ).toList();
        }
        return null;
    }
}
