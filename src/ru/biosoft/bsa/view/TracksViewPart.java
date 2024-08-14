package ru.biosoft.bsa.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.view.colorscheme.SiteColorScheme;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;

@SuppressWarnings ( "serial" )
public class TracksViewPart extends ViewPartSupport implements PropertyChangeListener
{
    public static final String VIEW_OPTIONS = "View options";
    public static final String NOTES = "Notes";
    public static final String LEGEND = "Legend";
    public static final String COLOR_SCHEME = "Color scheme";

    protected TabularPropertyInspector tracksTable;
    protected JTabbedPane tabbedPane;
    protected PropertyInspector propertiesEditor;
    protected JTextArea notes;
    protected ViewPane legendViewPane;

    public TracksViewPart()
    {
        tracksTable = new TabularPropertyInspector();
        tabbedPane = new JTabbedPane(JTabbedPane.RIGHT);
        propertiesEditor = new PropertyInspector();
        notes = new JTextArea();
        notes.setEditable(false);

        legendViewPane = new ViewPane();

        tabbedPane.add(VIEW_OPTIONS, propertiesEditor);
        tabbedPane.add(NOTES, notes);
        tabbedPane.add(LEGEND, legendViewPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tracksTable, tabbedPane);
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);

        tracksTable.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tracksTable.addListSelectionListener(event -> {
            if( event.getFirstIndex() != -1 )
            {
                TrackInfo trackInfo = (TrackInfo)tracksTable.getModelOfSelectedRow();
                if( trackInfo != null )
                {
                    selectTrack(trackInfo);
                    return;
                }
            }

            propertiesEditor.explore(null);
            notes.setText("");
            notes.setEditable(false);
            removeAction.setEnabled(false);
        });

        removeAction.setEnabled(false);
    }

    protected void selectTrack(TrackInfo trackInfo)
    {
        removeAction.setEnabled(true);
        if( model instanceof Project )
        {
            SiteViewOptions trackViewOptions = ( (Project)model ).getViewOptions().getTrackViewOptions(trackInfo.getTrack().getCompletePath());
            ComponentFactory.recreateChildProperties(ComponentFactory.getModel(trackViewOptions));
            propertiesEditor.explore(trackViewOptions);

            notes.setText(trackInfo.getDescription());
            notes.setEditable(true);
            SiteColorScheme colorScheme = trackViewOptions.getColorScheme();
            legendViewPane.setView(colorScheme.getLegend(Application.getApplicationFrame().getGraphics()));
        }
    }

    private static MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    public static MessageBundle getMessageBundle()
    {
        return messageBundle;
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(addAction, TracksViewPart.ADD_ACTION);
            initializer.initAction(removeAction, TracksViewPart.REMOVE_ACTION);

            actions = new Action[] {addAction, removeAction};
        }

        return actions;
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Project;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.model instanceof Project )
        {
            ((Project)this.model).removePropertyChangeListener(this);
        }
        super.explore(model, document);
        if( this.model instanceof Project )
        {
            ((Project)this.model).addPropertyChangeListener(this);
        }
        updatePanes();
    }

    protected void updatePanes()
    {
        if(!isVisible()) return;
        if( model instanceof Project )
        {
            TrackInfo selected = (TrackInfo)tracksTable.getModelOfSelectedRow();
            Project project = (Project)model;
            TrackInfo[] tracks = project.getTracks();
            tracksTable.explore(tracks);

            if( tracks.length > 0 )
            {
                int selectedIndex = 0;
                for(int i=0; i<tracks.length; i++)
                {
                    if(tracks[i] == selected)
                    {
                        selectedIndex = i;
                        break;
                    }
                }
                tracksTable.getTable().getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
            }
            else
            {
                propertiesEditor.explore(null);
                notes.setText("");
                notes.setEditable(false);
                legendViewPane.setView(new CompositeView());
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        updatePanes();
    }

    //
    // Actions
    //

    public static final String ADD_ACTION = "add track";
    public static final String REMOVE_ACTION = "remove track";

    protected Action addAction = new AddAction(ADD_ACTION);
    protected Action removeAction = new RemoveAction(REMOVE_ACTION);
    protected Action[] actions;


    class AddAction extends AbstractAction
    {
        public AddAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( model instanceof Project )
            {
                DataElementPathDialog dialog = new DataElementPathDialog();
                dialog.setElementClass(Track.class);
                dialog.setElementMustExist(true);
                dialog.setValue((DataElementPath)null);
                if( dialog.doModal() )
                {
                    DataElementPath trackPath = dialog.getValue();
                    Track track = (Track)trackPath.optDataElement();
                    int maxOrder = 0;
                    for(TrackInfo info: ( (Project)model).getTracks())
                    {
                        if(info.getTrack() == track) return;
                        maxOrder = Math.max(info.getOrder(), maxOrder);
                    }
                    TrackInfo trackInfo = new TrackInfo(track);
                    trackInfo.setOrder(maxOrder+1);
                    trackInfo.setTitle(track.getName());
                    ( (Project)model ).addTrack(trackInfo);
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
            TrackInfo trackInfo = (TrackInfo)tracksTable.getModelOfSelectedRow();
            if( trackInfo != null && model != null )
            {
                String title = getMessageBundle().getResourceString("REMOVE_TRACK_CONFIRM_TITLE");
                String message = MessageFormat.format(getMessageBundle().getResourceString("REMOVE_TRACK_CONFIRM_MESSAGE"),
                        new Object[] {trackInfo.getTitle()});
                int answer = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message, title, JOptionPane.YES_NO_OPTION);
                if( answer == JOptionPane.YES_OPTION )
                {
                    ( (Project)model ).removeTrack(trackInfo);
                }
            }
        }
    }
}
