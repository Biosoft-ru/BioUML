package biouml.plugins.optimization.document.editors;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.optimization.access.NewOptimizationExperimentDialog;
import biouml.plugins.optimization.document.OptimizationDocument;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

public class OptimizationExperimentViewPart extends ViewPartSupport
{
    protected JTabbedPane tabbedPane;

    public static final String ADD = "add experiment";
    public static final String REMOVE = "remove experiment";

    protected Action addAction = new AddAction(ADD);
    protected Action removeAction = new RemoveAction(REMOVE);

    private final PropertyChangeListener optParamsListener;

    public OptimizationExperimentViewPart()
    {
        tabbedPane = new JTabbedPane(SwingConstants.LEFT);

        optParamsListener = evt -> {
            if( evt.getPropertyName().equals(OptimizationParameters.OPTIMIZATION_EXPERIMENTS) )
            {
                List<OptimizationExperiment> experiments = ( (Optimization)model ).getParameters()
                        .getOptimizationExperiments();
                if( experiments.size() != tabbedPane.getTabCount() || isTransaction() )
                {
                    initTabbedPane(experiments);
                }
            }
        };

        add(BorderLayout.CENTER, tabbedPane);
    }
   
    private void initTabbedPane(List<OptimizationExperiment> experiments)
    {
        for( int i = 0; i < tabbedPane.getTabCount(); ++i )
        {
            OptimizationExperimentTab tab = (OptimizationExperimentTab)tabbedPane.getComponentAt(i);
            tab.restoreListeners();
        }

        tabbedPane.removeAll();
        for( int i = 0; i < experiments.size(); ++i )
        {
            if( experiments.get(i).getTableSupport().getTable() != null )
            {
                OptimizationExperimentTab expTab = new OptimizationExperimentTab(experiments.get(i));
                tabbedPane.addTab(experiments.get(i).getName(), expTab);
            }
        }
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    private boolean isTransaction()
    {
        return ( (OptimizationDocument)document ).getUndoManager().isUndo() || ( (OptimizationDocument)document ).getUndoManager().isRedo();
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( model != null )
        {
            OptimizationParameters params = ( (Optimization)model ).getParameters();
            params.removePropertyChangeListener(optParamsListener);

            List<OptimizationExperiment> experiments = ( (Optimization)model ).getParameters()
                    .getOptimizationExperiments();
            initTabbedPane(experiments);

            params.addPropertyChangeListener(optParamsListener);
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        return ( model instanceof Optimization );
    }

    ///////////////////////////////////////////////////////////////////////////
    // Actions
    //
    @Override
    public Action[] getActions()
    {
        ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
        initializer.initAction(addAction, ADD);
        initializer.initAction(removeAction, REMOVE);

        Action[] action = new Action[] {addAction, removeAction};

        return action;
    }

    class AddAction extends AbstractAction
    {
        public AddAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            List<OptimizationExperiment> experiments = ( (Optimization)model ).getParameters()
                    .getOptimizationExperiments();
            List<OptimizationExperiment> newValue = new ArrayList<>( experiments );

            NewOptimizationExperimentDialog dialog = new NewOptimizationExperimentDialog( ( (Optimization)model ).getDiagram(), newValue);
            if( dialog.doModal() )
            {
                OptimizationExperiment exp = newValue.get(newValue.size() - 1);
                if( exp.getTableSupport().getTable() != null && exp.getTableSupport().getTable().getSize() > 0
                        && exp.getTableSupport().getTable().getColumnModel().getColumnCount() > 0 )
                {
                    initTabbedPane(newValue);
                    exp.addPropertyChangeListener((OptimizationDocument)document);

                    for( ParameterConnection connection : exp.getParameterConnections() )
                        connection.addPropertyChangeListener((OptimizationDocument)document);

                    refreshDocument(newValue);
                }
                else
                {
                    JOptionPane.showMessageDialog(Application.getApplicationFrame(), MessageBundle
                            .getMessage("ERROR_EXPERIMENT_FILE_IS_INCORRECT"));
                }
            }
        }
    }

    class RemoveAction extends AbstractAction
    {
        public RemoveAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            OptimizationExperimentTab expTab = (OptimizationExperimentTab)tabbedPane.getSelectedComponent();
            OptimizationExperiment exp = expTab.getExperiment();
            List<OptimizationExperiment> experiments = ( (Optimization)model ).getParameters()
                    .getOptimizationExperiments();
            List<OptimizationExperiment> newValue = new ArrayList<>( experiments );

            newValue.remove(exp);
            tabbedPane.remove(expTab);

            refreshDocument(newValue);
        }
    }

    private void refreshDocument(List<OptimizationExperiment> newValue)
    {
        ( (Optimization)model ).getParameters().setOptimizationExperiments(newValue);
        ( (OptimizationDocument)document ).fireValueChanged(new EventObject(this));
    }
}
