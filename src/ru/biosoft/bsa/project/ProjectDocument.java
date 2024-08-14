package ru.biosoft.bsa.project;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;

import javax.swing.Action;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.bsa.gui.ExportTrackDialog;
import ru.biosoft.bsa.view.MapView;
import ru.biosoft.bsa.view.ViewFactory;
import ru.biosoft.bsa.view.ViewOptions;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.SaveDocumentAction;
import ru.biosoft.gui.ZoomInAction;
import ru.biosoft.gui.ZoomOutAction;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.application.action.SeparatorAction;
import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 * BSA project document
 */
public class ProjectDocument extends Document implements PropertyChangeListener
{
    protected boolean newProject;
    protected boolean changed;
    protected boolean updating;

    public static final int RIGHT_OFFSET = 50;

    /**
     * Mode for view options
     */
    public enum ViewOptionsMode
    {
        MODE_CUSTOM, MODE_OVERVIEW, MODE_DEFAULT, MODE_DETAILED
    }

    public ProjectDocument(Project project, boolean newProject)
    {
        super(project);

        this.newProject = newProject;

        project.addPropertyChangeListener(this);

        viewPane = new ProjectViewPane(this);

        viewPane.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateView();
            }
        });
        viewPane.addViewPaneListener( DocumentManager.getDocumentViewAccessProvider().getDocumentViewListener());

        updateView();

        this.changed = false;
    }

    protected void updateView()
    {
        if(updating) return;
        try
        {
            updating = true;
            Graphics g = Application.getApplicationFrame().getGraphics();
            int pixels = calculateVisibleWidthPixels();
            double scale = ((double)pixels)/getProject().getRegions()[0].getInterval().getLength();
            getViewOptions().semanticZoomSet(scale);
            CompositeView view = (CompositeView)ViewFactory.createProjectView(getProject(), g, null);
            viewPane.setView(view);
        }
        finally
        {
            updating = false;
        }
    }

    public int getExtent(ViewOptionsMode mode)
    {
        Region region = getProject().getRegions()[0];
        switch(mode)
        {
            case MODE_DETAILED:
                FontMetrics fontMetrics = ApplicationUtils.getGraphics().getFontMetrics(getViewOptions().getRegionViewOptions().getSequenceViewOptions().getFont().getFont());
                return calculateVisibleWidthPixels()/fontMetrics.stringWidth("a");
            case MODE_DEFAULT:
                return (int) ( Math.sqrt(region.getSequence().getInterval().getLength())*10 );
            default:
                return region.getSequence().getInterval().getLength();
        }
    }

    protected int calculateVisibleWidthPixels()
    {
        double scale = viewPane.getScaleX();
        return (int) ( Math.max(0, viewPane.getBounds().width / scale - MapView.LEFT_TITLE_OFFSET) );
    }

    public void projectChanged()
    {
        updateView();

        changed = true;
        if( Document.getActiveDocument() == this )
        {
            Application.getActionManager().enableActions( true, SaveDocumentAction.KEY );
        }
    }

    @Override
    public String getDisplayName()
    {
        return getProject().getOrigin() == null ? getProject().getName() : getProject().getOrigin().getName() + " : " + getProject().getName();
    }

    public ViewOptions getViewOptions()
    {
        return getProject().getViewOptions();
    }

    public Project getProject()
    {
        return (Project)getModel();
    }

    @Override
    public void close()
    {
        getProject().removePropertyChangeListener(this);

        super.close();
    }

    @Override
    public boolean isChanged()
    {
        return ( newProject || changed );
    }

    @Override
    public boolean isMutable()
    {
        return true;
    }

    @Override
    public void save()
    {
        try
        {
            if(newProject)
            {
                DataElementPathDialog dialog = new DataElementPathDialog();
                dialog.setElementClass(Project.class);
                dialog.setPromptOverwrite(true);
                dialog.setValue((DataElementPath)null);
                if(dialog.doModal())
                {
                    Project clone = (Project)getProject().clone(dialog.getValue().optParentCollection(), dialog.getValue().getName());
                    dialog.getValue().save(clone);
                    setModel(clone);
                    projectChanged();
                }
            } else
            {
                DataElementPath.create(getProject()).save(getProject());
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to save project "+getProject().getName(), e);
        }
    }

    /*
     * Property change listener
     *
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        projectChanged();
    }

    private static boolean actionInitialized = false;
    @Override
    public Action[] getActions(ActionType actionType)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;

            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

            //toolbar actions
            Action action = new ZoomInAction(true);
            actionManager.addAction(ZoomInAction.KEY, action);
            initializer.initAction(action, ZoomInAction.KEY);

            action = new ZoomOutAction(true);
            actionManager.addAction(ZoomOutAction.KEY, action);
            initializer.initAction(action, ZoomOutAction.KEY);

            action = new SemanticZoomInAction();
            actionManager.addAction(SemanticZoomInAction.KEY, action);
            initializer.initAction(action, SemanticZoomInAction.KEY);

            action = new SemanticZoomOutAction();
            actionManager.addAction(SemanticZoomOutAction.KEY, action);
            initializer.initAction(action, SemanticZoomOutAction.KEY);

            action = new SetModeAction(SetModeAction.KEY_OVERVIEW, ViewOptionsMode.MODE_OVERVIEW);
            actionManager.addAction(SetModeAction.KEY_OVERVIEW, action);
            initializer.initAction(action, SetModeAction.KEY_OVERVIEW);

            action = new SetModeAction(SetModeAction.KEY_DEFAULT, ViewOptionsMode.MODE_DEFAULT);
            actionManager.addAction(SetModeAction.KEY_DEFAULT, action);
            initializer.initAction(action, SetModeAction.KEY_DEFAULT);

            action = new SetModeAction(SetModeAction.KEY_DETAILED, ViewOptionsMode.MODE_DETAILED);
            actionManager.addAction(SetModeAction.KEY_DETAILED, action);
            initializer.initAction(action, SetModeAction.KEY_DETAILED);
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action zoomInAction = actionManager.getAction(ZoomInAction.KEY);
            Action zoomOutAction = actionManager.getAction(ZoomOutAction.KEY);
            AbstractProjectAction semanticZoomInAction = (AbstractProjectAction)actionManager.getAction(SemanticZoomInAction.KEY);
            semanticZoomInAction.setProject(getProject());
            AbstractProjectAction semanticZoomOutAction = (AbstractProjectAction)actionManager.getAction(SemanticZoomOutAction.KEY);
            semanticZoomOutAction.setProject(getProject());

            Action overviewAction = actionManager.getAction(SetModeAction.KEY_OVERVIEW);
            overviewAction.putValue(SetModeAction.DOCUMENT, this);
            Action defaultAction = actionManager.getAction(SetModeAction.KEY_DEFAULT);
            defaultAction.putValue(SetModeAction.DOCUMENT, this);
            Action detailedAction = actionManager.getAction(SetModeAction.KEY_DETAILED);
            detailedAction.putValue(SetModeAction.DOCUMENT, this);

            return new Action[] {new SeparatorAction(), detailedAction, defaultAction, overviewAction, new SeparatorAction(),
                    semanticZoomInAction, semanticZoomOutAction, zoomInAction, zoomOutAction};
        }
        return null;
    }

    @Override
    public OkCancelDialog getExportDialog()
    {
        return new ExportTrackDialog(Application.getApplicationFrame(), (Project)getModel(), null);
    }
}
