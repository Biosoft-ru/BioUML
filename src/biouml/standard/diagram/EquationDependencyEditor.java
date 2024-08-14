package biouml.standard.diagram;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import biouml.model.Diagram;

public class EquationDependencyEditor extends EditorPartSupport implements PropertyChangeListener
{
    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof biouml.model.Diagram )
        {
            return ( (Diagram)model ).getType() instanceof MathDiagramType;
        }
        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        super.explore(model, document);
    }

    protected Action[] actions;
    protected GenerateDependencyAction generateAction = new GenerateDependencyAction();
    protected RemoveDependencyAction removeAction = new RemoveDependencyAction();
    
    @Override
    public Action[] getActions()
    {
        if( actions == null )
        {
//            ActionInitializer.init(r, l);
// TODO: seems that there's no initialization bundle or icons for these actions
//            ActionInitializer.initAction(generateAction, GenerateDependencyAction.KEY);
//            ActionInitializer.initAction(removeAction, RemoveDependencyAction.KEY);
            generateAction.setEnabled(true);
            generateAction.setEnabled(true);
            actions = new Action[] {generateAction, removeAction};
        }

        return new Action[] {generateAction};
    }

    public class GenerateDependencyAction extends AbstractAction
    {
        public static final String KEY = "Generate dependencies";

        public GenerateDependencyAction()
        {
            super(KEY);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MathDiagramUtility.generateDependencies((Diagram)model);
        }
    }

    public class RemoveDependencyAction extends AbstractAction
    {
        public static final String KEY = "Remove dependencies";

        public RemoveDependencyAction()
        {
            super(KEY);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MathDiagramUtility.generateDependencies((Diagram)model);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // TODO Auto-generated method stub

    }
}
