package biouml.workbench.diagram.viewpart;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import ru.biosoft.util.DPSUtils;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;

import com.developmentontheedge.beans.swing.PropertyInspectorEx;

public class PlotsEditorPart extends EditorPartSupport implements PropertyChangeListener
{
    public static final String PLOTS = "Plots";
    private EModel executableModel;
    private PlotsInfo plot;
    private final PropertyInspectorEx inspector = new PropertyInspectorEx();

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ((Diagram)model ).getRole() instanceof EModel;
    }

    @Override
    public JComponent getView()
    {
        return inspector;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( executableModel != null )
            executableModel.removePropertyChangeListener(this);

        executableModel = ( (Diagram)model ).getRole(EModel.class);
        executableModel.addPropertyChangeListener(this);

        try
        {
            Object plotsObj = executableModel.getParent().getAttributes().getValue(PLOTS);

            if( ! ( plotsObj instanceof PlotsInfo ) )
            {
                plot = new PlotsInfo(executableModel);
                executableModel.getParent().getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(PLOTS, PlotsInfo.class, plot));
            }
            else
                plot = (PlotsInfo)plotsObj;

            inspector.explore(plot);

        }
        catch( Exception e )
        {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Can not explore plots for diagram "+model, e);
        }
    }

    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        inspector.explore(plot);
    }

    @Override
    public void onClose()
    {
        try
        {  
            if( executableModel != null ) //it can be null in some cases - e.g. when we apply antimony, we recreate document but this editor was not even opened (and initialized) before that
                executableModel.getParent().getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(PLOTS, PlotsInfo.class, plot));
        }
        catch( Exception ex )
        {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error with plot pane.", ex);
        }
    }
}

