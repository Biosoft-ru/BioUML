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

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.action.ActionInitializer;

import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.document.OptimizationDocument;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

public class OptimizationConstraintsViewPart extends ViewPartSupport
{
    protected TabularPropertyInspector editor;

    public static final String ADD = "add constrait";
    public static final String REMOVE = "remove onstrait";

    protected Action addAction = new AddAction(ADD);
    protected Action removeAction = new RemoveAction(REMOVE);

    private final PropertyChangeListener optListener;

    public OptimizationConstraintsViewPart()
    {
        editor = new TabularPropertyInspector();

        List<OptimizationConstraint> constraints = new ArrayList<>();
        editor.explore(constraints.iterator());

        editor.addListSelectionListener(event -> {
            if( event.getFirstIndex() != -1 )
            {
                firstInd = event.getFirstIndex();
                lastInd = event.getLastIndex();
                alreadyDeleted = false;
            }
        });

        optListener = evt -> {
            if( evt.getPropertyName().equals(OptimizationParameters.OPTIMIZATION_CONSTRAINTS) )
            {
                editor.explore( ( (Optimization)model ).getParameters().getOptimizationConstraints()
                        .iterator());
            }
        };

        add(BorderLayout.CENTER, editor);
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( model != null )
        {
            ( (Optimization)model ).getParameters().removePropertyChangeListener(optListener);
            ( (Optimization)model ).getParameters().addPropertyChangeListener(optListener);

            List<OptimizationConstraint> constraints = ( (Optimization)model ).getParameters()
                    .getOptimizationConstraints();
            editor.explore(constraints.iterator());
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
            OptimizationParameters optParames = ( (Optimization)model ).getParameters();
            List<OptimizationConstraint> constraints = optParames.getOptimizationConstraints();
            List<OptimizationConstraint> newValue = copy(constraints);

            OptimizationConstraint newConstr = new OptimizationConstraint();
            newConstr.setAvailableExperiments( optParames.getOptimizationExperiments() );
            newConstr.setDiagram(( (Optimization)model ).getDiagram());
            newConstr.addPropertyChangeListener((OptimizationDocument)document);

            newValue.add(newConstr);
            refreshDocument(newValue);
        }
    }

    private int firstInd = -1;
    private int lastInd;
    private boolean alreadyDeleted = false;
    class RemoveAction extends AbstractAction
    {
        public RemoveAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( firstInd > -1 && !alreadyDeleted )
            {
                List<OptimizationConstraint> constraints = ( (Optimization)model ).getParameters()
                        .getOptimizationConstraints();
                List<OptimizationConstraint> newValue = copy(constraints);
                if( newValue.size() == lastInd - firstInd + 1 )
                {
                    newValue = new ArrayList<>();
                }
                else
                {
                    for( int i = lastInd; i > firstInd - 1; --i )
                        newValue.remove(i);
                }
                refreshDocument(newValue);
                alreadyDeleted = true;
            }
            else
            {
                JOptionPane.showMessageDialog(null, MessageBundle.getMessage("WARN_CONSTRAINT_SELECTION"));
            }
        }
    }

    private List<OptimizationConstraint> copy(List<OptimizationConstraint> list)
    {
        List<OptimizationConstraint> copyList = new ArrayList<>();
        for( int j = 0; j < list.size(); ++j )
            copyList.add(list.get(j));
        return copyList;
    }

    private void refreshDocument(List<OptimizationConstraint> newValue)
    {
        ( (Optimization)model ).getParameters().setOptimizationConstraints(newValue);
        ( (OptimizationDocument)document ).fireValueChanged(new EventObject(this));
    }
}
