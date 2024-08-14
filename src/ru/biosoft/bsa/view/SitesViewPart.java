package ru.biosoft.bsa.view;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LimitedSizeSitesCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.access.SitesTableCollection;
import ru.biosoft.bsa.access.TransformedSite;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.table.MessageStubTableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ListComboBoxModel;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.swing.table.ColumnModel;

@SuppressWarnings ( "serial" )
public class SitesViewPart extends ViewPartSupport implements ListSelectionListener, PropertyChangeListener
{
    protected TabularPropertyInspector sitesTable;
    protected ListComboBoxModel<TrackInfo> trackListModel;
    protected TrackInfo currentTrack;

    public SitesViewPart()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel trackListPanel = new JPanel();
        trackListPanel.setLayout(new BoxLayout(trackListPanel, BoxLayout.X_AXIS));
        trackListModel = new ListComboBoxModel<>();
        trackListPanel.add(new JLabel("Track:"));
        final JComboBox<TrackInfo> trackSelector = new JComboBox<>(trackListModel);
        trackSelector.addActionListener(e -> {
            currentTrack = (TrackInfo)trackSelector.getSelectedItem();
            contentChanged();
        });
        trackListPanel.add(trackSelector);
        
        add(trackListPanel);
        
        sitesTable = new TabularPropertyInspector();
        add(sitesTable, BorderLayout.CENTER);
        sitesTable.addListSelectionListener(this);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    protected Action[] actions;
    @Override
    public Action[] getActions()
    {
        if( actions == null )
        {
            actions = new Action[] {};
        }

        return actions;
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof Project )
            return true;
        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.model instanceof Project )
        {
            ( (Project)this.model ).removePropertyChangeListener(this);
        }

        if( model instanceof Project )
        {
            this.model = model;
            this.document = document;
            ((Project)this.model).addPropertyChangeListener(this);
            propertyChange(null);
        } else
        {
            this.model = null;
        }
    }

    protected void contentChanged()
    {
        if(!isVisible() || model == null) return;
        Project project = (Project)model;
        Region region = project.getRegions()[0];
        Interval interval = region.getInterval();
        DataCollection<?> sites;
        try
        {
            sites = getSitesCollection(currentTrack.getTrack(), region.getSequenceName(), interval.getFrom(), interval.getTo());
        }
        catch( Exception e )
        {
            sites = new MessageStubTableDataCollection(e.getMessage());
        }
        ColumnModel columnModel = TableDataCollectionUtils.getColumnModel(sites);
        sitesTable.setSortEnabled(sites.getSize() <= 1000);
        sitesTable.explore(new DataCollectionRowModelAdapter(sites), columnModel);
    }
    
    public static DataCollection<?> getSitesCollection(Track track, String sequenceName, int from, int to) throws Exception
    {
        DataCollection<Site> sitesCol = track.getSites(sequenceName, from, to);
        int size;
        if(sitesCol instanceof LimitedSizeSitesCollection)
        {
            size = ((LimitedSizeSitesCollection)sitesCol).getSizeLimited(TrackViewBuilder.SITE_COUNT_HARD_LIMIT+1);
        } else
        {
            size = sitesCol.getSize();
        }
        if(size > TrackViewBuilder.SITE_COUNT_HARD_LIMIT)
            throw new IllegalArgumentException("Too many sites");
        return new SitesTableCollection(track, sitesCol);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(!isVisible() || model == null) return;
        trackListModel.updateList(Arrays.asList(((Project)model).getTracks()));
        contentChanged();
    }
    //
    // list selection listener
    //

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        ListSelectionModel lsm = sitesTable.getTable().getSelectionModel();
        if( !lsm.isSelectionEmpty() )
        {
            Object obj = sitesTable.getModelForRow(lsm.getMinSelectionIndex());
            if( obj instanceof TransformedSite )
            {
                Site site = ((TransformedSite)obj).getSite();
                GUI.getManager().explore( site );
                document.getViewPane().getSelectionManager().selectModel(site, true);
                document.getViewPane().repaint();
            }
        }
    }
}
