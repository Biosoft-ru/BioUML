package biouml.plugins.optimization.document.editors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ru.biosoft.gui.ViewPartSupport;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import biouml.plugins.optimization.ExperimentalTableSupport;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.ParameterConnection;

@SuppressWarnings ( "serial" )
public class OptimizationExperimentTab extends ViewPartSupport
{
    protected JComponent view;
    protected JPanel experimentPane;

    protected JTextField diagramStateField;
    protected JTextField dataFileField;
    protected JTextField cellLineField;
    protected JComboBox<String> weightMethodBox;
    protected JComboBox<String> experimentTypeBox;

    protected TabularPropertyInspector inspector;

    public OptimizationExperimentTab(OptimizationExperiment experiment)
    {
        this.experiment = experiment;

        init();
        paint();
    }

    private OptimizationExperiment experiment;
    public OptimizationExperiment getExperiment()
    {
        return this.experiment;
    }

    private void init()
    {
        experimentPane = new JPanel();

        initDiagramStateField();
        initDataFileField();
        initCellLineField();
        initWeightMethodBox();
        initExperimentTypeBox();

        inspector = new TabularPropertyInspector();
        inspector.explore(experiment.getParameterConnections().iterator());

        initListeners();
    }

    private PropertyChangeListener expListener;
    private void initListeners()
    {
        expListener = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if( isUndoOrRedo )
                {
                    if( evt.getPropertyName().equals(OptimizationExperiment.WEIGHT_METHOD) )
                    {
                        weightMethodBox.setSelectedItem(experiment.getWeightMethod());
                        experiment.initWeights();
                        inspector.explore(experiment.getParameterConnections().iterator());
                    }
                    else if( evt.getPropertyName().equals(OptimizationExperiment.EXPERIMENT_TYPE) )
                    {
                        experimentTypeBox.setSelectedItem(experiment.getExperimentType());
                        experiment.initWeights();
                        inspector.explore(experiment.getParameterConnections().iterator());
                    }
                    else if( evt.getPropertyName().equals(OptimizationExperiment.CELL_LINE) )
                    {
                        cellLineField.setText(experiment.getCellLine());
                    }
                }

                if( evt.getPropertyName().equals("relativeTo") )
                {
                    experiment.initWeights();
                    inspector.explore(experiment.getParameterConnections().iterator());
                }
            }
        };

        experiment.addPropertyChangeListener(expListener);

        for( ParameterConnection c : experiment.getParameterConnections() )
            c.addPropertyChangeListener(expListener);
    }

    public void restoreListeners()
    {
        experiment.removePropertyChangeListener( expListener );

        for( ParameterConnection c : experiment.getParameterConnections() )
            c.removePropertyChangeListener( expListener );
    }

    private boolean isUndoOrRedo = true;
    private void initDiagramStateField()
    {
        diagramStateField = new JTextField();
        diagramStateField.setText(experiment.getDiagramStateName());
        diagramStateField.setEditable(false);

        diagramStateField.setPreferredSize(dimension);
        diagramStateField.setMinimumSize(dimension);
    }

    private void initDataFileField()
    {
        dataFileField = new JTextField();
        dataFileField.setText(experiment.getFilePath().getName());
        dataFileField.setEditable(false);

        dataFileField.setPreferredSize(dimension);
        dataFileField.setMinimumSize(dimension);
    }

    private void initCellLineField()
    {
        cellLineField = new JTextField();
        cellLineField.setPreferredSize(dimension);
        cellLineField.setMinimumSize(dimension);
        cellLineField.setText(experiment.getCellLine());

        cellLineField.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = cellLineField.getText();
                isUndoOrRedo = false;
                experiment.setCellLine(text);
                isUndoOrRedo = true;
            }

            @Override
            public void keyPressed(KeyEvent e)
            {

            }
        });
    }

    private void initWeightMethodBox()
    {
        weightMethodBox = new JComboBox<>();
        initBox(weightMethodBox, ExperimentalTableSupport.WeightMethod.getWeightMethods(), experiment.getWeightMethod());
        experiment.setWeightMethod((String)weightMethodBox.getSelectedItem());

        weightMethodBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                String selectedFileName = (String)weightMethodBox.getSelectedItem();
                isUndoOrRedo = false;
                experiment.setWeightMethod(selectedFileName);
                experiment.initWeights();
                inspector.explore(experiment.getParameterConnections().iterator());
                isUndoOrRedo = true;
            }
        });
    }

    private void initExperimentTypeBox()
    {
        experimentTypeBox = new JComboBox<>();
        initBox(experimentTypeBox, OptimizationExperiment.ExperimentType.getExperimentTypes(), experiment.getExperimentType());
        experiment.setExperimentType((String)experimentTypeBox.getSelectedItem());

        experimentTypeBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                String selectedType = (String)experimentTypeBox.getSelectedItem();
                isUndoOrRedo = false;
                experiment.setExperimentType(selectedType);
                experiment.initWeights();
                inspector.explore(experiment.getParameterConnections().iterator());
                isUndoOrRedo = true;
            }
        });
    }

    private Dimension dimension = new Dimension(100, 20);
    private void initBox(JComboBox<String> box, List<String> items, String selectedItem)
    {
        box.setPreferredSize(dimension);
        box.setMinimumSize(dimension);

        if( items == null || items.size() == 0 )
            return;

        for( String name : items )
        {
            box.addItem(name);
        }

        if( items.contains(selectedItem) )
            box.setSelectedItem(selectedItem);
        else
            box.setSelectedIndex(0);
    }

    private JLabel diagramStateLabel = new JLabel("Diagram state:");
    private JLabel dataFileLabel = new JLabel("Experiment file:");
    private JLabel cellLineLabel = new JLabel("Cell line:");
    private JLabel weightMethodLabel = new JLabel("Weight method:");
    private JLabel experimentTypeLabel = new JLabel("Experiment type:");
    private void paint()
    {
        int anchor = GridBagConstraints.WEST;
        Insets insets = new Insets(5, 5, 0, 5);

        experimentPane.setLayout(new GridBagLayout());
        experimentPane.add(diagramStateLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));
        experimentPane.add(dataFileLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));

        experimentPane.add(weightMethodLabel, new GridBagConstraints(2, 0, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));
        experimentPane.add(experimentTypeLabel, new GridBagConstraints(2, 1, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));

        experimentPane.add(cellLineLabel, new GridBagConstraints(4, 0, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));

        insets = new Insets(5, 0, 0, 10);

        experimentPane
                .add(diagramStateField, new GridBagConstraints(1, 0, 1, 1, 1, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        experimentPane.add(dataFileField, new GridBagConstraints(1, 1, 1, 1, 1, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0, 0));

        experimentPane.add(weightMethodBox, new GridBagConstraints(3, 0, 1, 1, 1, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        experimentPane
                .add(experimentTypeBox, new GridBagConstraints(3, 1, 1, 1, 1, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0, 0));

        experimentPane.add(cellLineField, new GridBagConstraints(5, 0, 1, 1, 1, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0, 0));

        experimentPane.add(inspector, new GridBagConstraints(0, 2, 6, 1, 1, 1, anchor, GridBagConstraints.BOTH, insets, 0, 0));

        add(experimentPane, BorderLayout.CENTER);
    }
}
