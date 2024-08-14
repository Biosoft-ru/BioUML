package biouml.plugins.optimization.document.editors;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

import biouml.plugins.optimization.OptimizationMethodRegistry;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.jobcontrol.JobProgressBar;

@SuppressWarnings ( "serial" )
public class OptimizationMethodViewPane extends JSplitPane
{
    protected Logger log = Logger.getLogger(OptimizationMethodViewPane.class.getName());
    private final static String[] CATEGORY_LIST = {"ru.biosoft.analysis.optimization", "biouml.plugins.optimization"};

    protected JComponent view;

    protected JPanel methodParamsPane;
    protected PropertyInspector inspector;
    protected JComboBox<String> methodBox;

    protected JTextField objectiveFunctionValueField;
    protected JTextField penaltyFunctionValueField;
    protected JTextField evaluationsNumberField;

    protected JPanel infoPane;
    protected TextPaneAppender appender;

    protected JobProgressBar jpb;

    private Option mParams;
    private PropertyChangeListener mParamsListener;

    public OptimizationMethodViewPane()
    {
        init();
        paint();
    }

    private void init()
    {
        inspector = new PropertyInspector();
        infoPane = new JPanel();
        methodParamsPane = new JPanel();
        jpb = new JobProgressBar();

        initMethodBox();
        initAppender();
        initTextFields();

        mParamsListener = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if(evt.getPropertyName().equals("randomSeedHidden") || evt.getPropertyName().equals("useStartingParameters"))
                {
                    inspector.explore(mParams);
                }
            }
        };
    }

    private void initMethodBox()
    {
        methodBox = new JComboBox<>();
        OptimizationMethodRegistry.getOptimizationMethodNames().forEach( methodBox::addItem );
    }

    private void initAppender()
    {
        appender = new TextPaneAppender( new PatternFormatter( "[%4$-7s] :  %5$s%n" ), "Application Log" );
        appender.setLevel( Level.INFO );
        appender.addToCategories(CATEGORY_LIST);
        log.addHandler( appender );
    }

    private void initTextFields()
    {
        objectiveFunctionValueField = new JTextField();
        penaltyFunctionValueField = new JTextField();
        evaluationsNumberField = new JTextField();

        objectiveFunctionValueField.setEditable(false);
        penaltyFunctionValueField.setEditable(false);
        evaluationsNumberField.setEditable(false);
    }

    public void explore(Option mParams)
    {
        if(this.mParams != null)
            this.mParams.removePropertyChangeListener(mParamsListener);
        mParams.addPropertyChangeListener(mParamsListener);
        this.mParams = mParams;
        inspector.explore(mParams);
    }

    public String getMethodName()
    {
        return (String)methodBox.getSelectedItem();
    }

    public void setMethodName(String methodName)
    {
        methodBox.setSelectedItem(methodName);
    }

    public JComboBox<String> getMethodNamesBox()
    {
        return this.methodBox;
    }

    public JobControlListener getJobControlListener()
    {
        return jpb;
    }

    public void stopJobControl()
    {
        jpb.setValue(0);
    }

    public void setObjectiveFunctionValue(String value)
    {
        objectiveFunctionValueField.setText(value);
    }

    public void setPenaltyFunctionValue(String value)
    {
        penaltyFunctionValueField.setText(value);
    }

    public void setEvaluationsNumber(String number)
    {
        evaluationsNumberField.setText(number);
    }

    private final JLabel methodLabel = new JLabel("Method:");
    private final JLabel objectiveFunctionValueLabel = new JLabel("Objective function:");
    private final JLabel penaltyFunctionValueLabel = new JLabel("Penalty function:");
    private final JLabel evaluationsNumberLabel = new JLabel("Simulations:");
    private void paint()
    {
        int anchor = GridBagConstraints.WEST;
        Insets insets = new Insets(5, 0, 0, 0);

        methodParamsPane.setLayout(new GridBagLayout());
        methodParamsPane.add(methodLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, new Insets(5, 5, 0, 5),
                0, 0));
        methodParamsPane.add(methodBox, new GridBagConstraints(1, 0, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, insets, 0, 0));
        methodParamsPane.add(inspector, new GridBagConstraints(0, 1, 2, 1, 1, 1, anchor, GridBagConstraints.BOTH, insets, 0, 0));

        infoPane.setLayout(new GridBagLayout());
        infoPane.add(objectiveFunctionValueLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, new Insets(5,
                5, 0, 5), 0, 0));
        infoPane.add(objectiveFunctionValueField, new GridBagConstraints(1, 0, 1, 1, 0, 0, anchor, GridBagConstraints.HORIZONTAL, insets,
                0, 0));
        infoPane.add(penaltyFunctionValueLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, new Insets(5, 5,
                0, 5), 0, 0));
        infoPane.add(penaltyFunctionValueField, new GridBagConstraints(1, 1, 1, 1, 0, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0,
                0));
        infoPane.add(evaluationsNumberLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, anchor, GridBagConstraints.NONE, new Insets(5, 5, 0,
                5), 0, 0));
        infoPane.add(evaluationsNumberField, new GridBagConstraints(1, 2, 1, 1, 0, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        infoPane.add(jpb, new GridBagConstraints(0, 3, 2, 1, 0, 0, anchor, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        infoPane.add(appender.getLogTextPanel(), new GridBagConstraints(0, 4, 2, 1, 1, 1, anchor, GridBagConstraints.BOTH, insets, 0, 0));

        setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        setContinuousLayout(false);
        add(methodParamsPane, JSplitPane.LEFT);
        add(infoPane, JSplitPane.RIGHT);
        setDividerLocation(0.5);
    }

}
