package ru.biosoft.bsa.project;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementDroppable;
import ru.biosoft.access.repository.DataElementImportTransferHandler;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.MapView;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneLayer;
import ru.biosoft.util.ListComboBoxModel;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * View pane for BSA project with specific horizontal scroll bar
 */
@SuppressWarnings ( "serial" )
public class ProjectViewPane extends ViewPane
{
    private static final int NONE = Integer.MIN_VALUE;
    private static final int DEFAULT_DELAY = ToolTipManager.sharedInstance().getInitialDelay();
    
    protected enum Mode
    {
        NORMAL, SELECT, DRAG, TRACK_DRAG
    }
    
    protected Mode mode = Mode.NORMAL;
    protected JScrollBar horizontalScrollBar;
    protected ProjectDocument document;
    protected Project project;
    protected PositionPane positionPane;

    private String toolTipText;

    public ProjectViewPane(ProjectDocument document)
    {
        super();
        this.document = document;
        this.project = document.getProject();
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        horizontalScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
        horizontalScrollBar.addAdjustmentListener(new ScrollBarHandler());
        add(horizontalScrollBar, BorderLayout.SOUTH);
        positionPane = new PositionPane();
        add(positionPane, BorderLayout.NORTH);
        
        // Order of listeners is important
        addViewPaneListener(new DragTrackHandler());
        HorizontalSelectionHandler selectionHandler = new HorizontalSelectionHandler();
        addViewPaneListener(selectionHandler);
        addViewPaneListener(new DragScrollHandler());
        addViewPaneListener(new ToolTipHandler());

        scrollPane.addMouseWheelListener(new WheelZoomHandler());
        
        addLayer(selectionHandler);

        mPanel.setTransferHandler(new DataElementImportTransferHandler(new DropTrackHandler()));
    }

    private void updateScrollBar()
    {
        Region region = project.getRegions()[0];
        Interval interval = region.getSequence().getInterval();
        Interval regionInterval = region.getInterval();
        horizontalScrollBar.setMinimum(interval.getFrom());
        horizontalScrollBar.setMaximum(interval.getTo()-regionInterval.getLength());
        horizontalScrollBar.setValue(regionInterval.getFrom());
        horizontalScrollBar.setBlockIncrement(regionInterval.getLength());
        horizontalScrollBar.setUnitIncrement(regionInterval.getLength()/10);
    }

    @Override
    public void scale(double sx, double sy)
    {
        double oldValue = getScaleX();
        super.scale(sx, sy);
        document.propertyChange(new PropertyChangeEvent(this, "scaleCoefficient", oldValue, getScaleX()));
    }

    @Override
    public Point getToolTipLocation(MouseEvent event)
    {
        Point pt = new Point(event.getPoint());
        pt.y+=30;
        return pt;
    }

    @Override
    public String getToolTipText(MouseEvent event)
    {
        return toolTipText == null ? "" : toolTipText;
    }

    @Override
    public void setView(CompositeView value, Point offset)
    {
        updateScrollBar();
        super.setView(value, offset);
    }

    protected MapView getMapView()
    {
        CompositeView view = getView();
        if(view == null) return null;
        for( View child : view )
        {
            if( child instanceof CompositeView )
            {
                for( View grandChild : (CompositeView)child )
                {
                    if( grandChild instanceof MapView )
                    {
                        return (MapView)grandChild;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Translates screen coordinate to genome coordinate
     * @param x screen x coordinate
     * @return genomic coordinate or null if x is out of bounds
     */
    protected Integer screenToGenome(int x)
    {
        if(x < MapView.LEFT_TITLE_OFFSET) return null;
        MapView map = getMapView();
        if(map == null) return null;
        double pixelWidth = getNucleotideWidth();
        double result = ( (double)x - MapView.LEFT_TITLE_OFFSET ) / pixelWidth + map.getRange().getFrom();
        if(!map.getRange().inside((int)result)) return null;
        int zoom = 1;
        while(pixelWidth * zoom < 1)
        {
            zoom *= 2;
            if(pixelWidth * zoom >= 1) break;
            zoom *= 2.5;
            if(pixelWidth * zoom >= 1) break;
            zoom *= 2;
        }
        return (int) ( Math.round(result/zoom)*zoom );
    }

    protected double getNucleotideWidth()
    {
        return SequenceView.getNucleotideWidth(project.getViewOptions().getRegionViewOptions()
                .getSequenceViewOptions(), ApplicationUtils.getGraphics());
    }

    protected Interval getVisibleRange()
    {
        return project.getRegions()[0].getInterval();
    }

    /**
     * Change tracks order after drag-n-drop
     * @param mainTrack Track which was dragged
     * @param mainTrackPos Vertical position of dragged track
     */
    protected void reorderTracks(Track mainTrack, int mainTrackPos)
    {
        MapView mapView = getMapView();
        if(mapView == null) return;
        Map<Integer, Track> tracks = new TreeMap<>();
        tracks.put(mainTrackPos, mainTrack);
        for(View child: mapView)
        {
            if((child.getModel() != mainTrack && child.getModel() instanceof Track) || child instanceof SequenceView)
            {
                int pos = child.getBounds().y;
                while(tracks.containsKey(pos)) pos++;
                tracks.put(pos, (Track)child.getModel());
            }
        }
        List<Track> sortedTracks = new ArrayList<>(tracks.values());
        int sequencePos = sortedTracks.indexOf(null);
        for(TrackInfo ti: project.getTracks())
        {
            ti.setOrder(sortedTracks.indexOf(ti.getTrack())-sequencePos);
        }
    }
    
    /**
     * This class handles floating tooltip displaying current position or site description
     * @author lan
     */
    private class ToolTipHandler extends ViewPaneAdapter
    {
        @Override
        public void mouseMoved(ViewPaneEvent e)
        {
            if(mode != Mode.NORMAL) return;
            Point pt = new Point(e.getPoint());
            View view = e.getViewSource();
            String description = view == null ? null : view.getDescription();
            if(description == null)
            {
                Integer pos = screenToGenome(pt.x);
                if(pos != null)
                {
                    description = String.valueOf(pos);
                    int len = description.length();
                    if(len > 6) description = description.substring(0,len-6)+" "+description.substring(len-6,len-3)+" "+description.substring(len-3);
                    else if(len > 3) description = description.substring(0,len-3)+" "+description.substring(len-3);
                }
            }
            ToolTipManager.sharedInstance().setEnabled(description != null);
            toolTipText = description;
        }

        @Override
        public void mouseEntered(ViewPaneEvent e)
        {
            ToolTipManager.sharedInstance().setInitialDelay(0);
            ToolTipManager.sharedInstance().registerComponent(mPanel);
        }

        @Override
        public void mouseExited(ViewPaneEvent e)
        {
            ToolTipManager.sharedInstance().unregisterComponent(mPanel);
            ToolTipManager.sharedInstance().setInitialDelay(DEFAULT_DELAY);
            ToolTipManager.sharedInstance().setEnabled(true);
        }

    }
    
    /**
     * This class handles reordering of tracks by dragging track name
     * @author lan
     */
    private class DragTrackHandler extends ViewPaneAdapter
    {
        private View view = null;
        private int offset = 0;
        
        @Override
        public void mousePressed(ViewPaneEvent e)
        {
            if(mode != Mode.NORMAL) return;
            View view = e.getViewSource();
            if(view != null && view.getModel() instanceof Track)
            {
                this.view = view;
                offset = e.getPoint().y-view.getBounds().y;
                mode = Mode.TRACK_DRAG;
            }
        }

        @Override
        public void mouseReleased(ViewPaneEvent e)
        {
            if(mode != Mode.TRACK_DRAG) return;
            Track mainTrack = (Track)view.getModel();
            reorderTracks(mainTrack, e.getY()-offset);
            view = null;
            mode = Mode.NORMAL;
        }

        @Override
        public void mouseDragged(ViewPaneEvent e)
        {
            if(mode != Mode.TRACK_DRAG) return;
            view.setLocation(view.getBounds().x, e.getY()-offset);
            mPanel.repaint();
        }
    }

    /**
     * This class handles horizontal scrolling by mouse dragging
     * @author lan
     */
    private class DragScrollHandler extends ViewPaneAdapter
    {
        private final Cursor defCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        private final Cursor hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        private int curX = 0;
        private double startPos;
        
        @Override
        public void mouseDragged(ViewPaneEvent e)
        {
            if(mode == Mode.NORMAL) mousePressed(e);
            if(mode == Mode.DRAG)
            {
                horizontalScrollBar.setValue((int) ( startPos+(curX-e.getX())/getNucleotideWidth() ));
            }
        }
        
        @Override
        public void mousePressed(ViewPaneEvent e)
        {
            if(mode != Mode.NORMAL) return;
            JComponent jc = (JComponent)e.getSource();
            jc.setCursor(hndCursor);
            curX = e.getX();
            startPos = horizontalScrollBar.getValue();
            mode = Mode.DRAG;
        }
        
        @Override
        public void mouseReleased(ViewPaneEvent e)
        {
            ( (JComponent)e.getSource() ).setCursor(defCursor);
            mode = Mode.NORMAL;
        }
    }
    
    /**
     * This class handles zoom in/out by mouse wheel
     * @author lan
     */
    private class WheelZoomHandler implements MouseWheelListener
    {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            if(e.isControlDown() || e.isMetaDown())
            {
                Integer pos = screenToGenome((int) ( e.getX()/getScaleX() ));
                if(pos == null) pos = getVisibleRange().getCenter();
                project.getRegions()[0].setInterval(getVisibleRange().zoom(pos, e.getWheelRotation() > 0 ? 2 : 0.5));
            }
        }
    }
    
    /**
     * This class handles select-and-zoom with pressed control
     * @author lan
     */
    private class HorizontalSelectionHandler extends ViewPaneAdapter implements ViewPaneLayer
    {
        private int from = NONE, to = NONE;
        private final Stroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1,1}, 0);
        private final Color color = new Color(0, 0, 255);
        private final Paint paint = new Color(128, 128, 128, 128);
        
        @Override
        public void paintLayer(Graphics2D g2)
        {
            if(mode != Mode.SELECT || from == NONE || to == NONE) return;
            Rectangle r = g2.getDeviceConfiguration().getBounds();
            g2.setPaint(paint);
            g2.fillRect(Math.min(from, to), r.y, Math.abs(to-from+1), r.height);
            g2.setStroke(stroke);
            g2.setColor(color);
            g2.drawLine(from, r.y, from, r.y+r.height);
            g2.drawLine(to, r.y, to, r.y+r.height);
        }

        @Override
        public void mousePressed(ViewPaneEvent e)
        {
            if(e.isControlDown() || e.isMetaDown())
            {
                int x = e.getX();
                if(screenToGenome(x) != null)
                {
                    from = x;
                    mode = Mode.SELECT;
                    mPanel.repaint();
                }
            }
        }

        @Override
        public void mouseReleased(ViewPaneEvent e)
        {
            if(mode != Mode.SELECT) return;
            if(from != NONE && to != NONE)
            {
                if(to < from)
                {
                    int tmp = to;
                    to = from;
                    from = tmp;
                }
                if(to == from) to++;
                Integer fromGenome = screenToGenome(from);
                Integer toGenome = screenToGenome(to);
                if(fromGenome != null && toGenome != null)
                {
                    project.getRegions()[0].setInterval(new Interval(fromGenome, toGenome));
                }
            }
            from = to = NONE;
            mode = Mode.NORMAL;
        }

        @Override
        public void mouseDragged(ViewPaneEvent e)
        {
            if(mode == Mode.SELECT)
            {
                int x = e.getX();
                if(screenToGenome(x) != null)
                {
                    to = x;
                    mPanel.repaint();
                }
            }
        }
    }
    
    /**
     * Handles navigation via scrollbar
     * @author lan
     */
    private final class ScrollBarHandler implements AdjustmentListener
    {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e)
        {
            Region region = project.getRegions()[0];
            if(region.getInterval().getFrom() != e.getValue())
            {
                region.setInterval(new Interval(e.getValue(), e.getValue()+region.getInterval().getLength()-1));
            }
        }
    }

    /**
     * Handles dropping tracks
     * @author lan
     */
    private final class DropTrackHandler implements DataElementDroppable
    {
        @Override
        public boolean doImport(DataElementPath path, Point point)
        {
            if( !Track.class.isAssignableFrom(DataCollectionUtils.getElementType(path)) )
                return false;
            Track track = (Track)path.optDataElement();
            for(TrackInfo info: project.getTracks())
            {
                if(info.getTrack() == track) return false;
            }
            TrackInfo trackInfo = new TrackInfo(track);
            trackInfo.setTitle(track.getName());
            
            project.addTrack(trackInfo);
            reorderTracks(track, (int) ( point.y/getScaleY() ));
            return true;
        }
    }

    /**
     * Navigation pane on the top of ProjectViewPane
     * @author lan
     */
    private class PositionPane extends JPanel
    {
        private final JComboBox<String> sequenceSelector;
        private final ActionListener changePositionAction;

        public PositionPane()
        {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel("Sequence (chromosome): "));
            JTextField position = new JTextField();
            DataCollection<? extends DataElement> sequencesCollection = project.getRegions()[0].getSequencePath().getParentCollection();
            changePositionAction = e -> {
                String positionStr = position.getText().trim();
                int colonPos = positionStr.indexOf(':');
                String sequence = getCurrentSequence();
                if(colonPos >= 0)
                {
                    String newSequence = positionStr.substring(0, colonPos).trim();
                    if(sequencesCollection.contains(newSequence))
                    {
                        sequence = newSequence;
                    } else
                    {
                        newSequence = Pattern.compile("^chr[\\.\\:]?").matcher(newSequence).replaceFirst("").trim();
                        if(sequencesCollection.contains(newSequence))
                        {
                            sequence = newSequence;
                        }
                    }
                    positionStr = positionStr.substring(colonPos+1).trim();
                }
                String[] coords = StreamEx.split( positionStr, "[\\-\\.]+" ).map( str -> str.replaceAll( "\\D", "" ) )
                        .toArray( String[]::new );
                int from = coords[0].isEmpty() ? 1 : Integer.parseInt(coords[0]);
                int to = coords.length < 2 || coords[1].isEmpty() ? from+getVisibleRange().getLength() : Integer.parseInt(coords[1]);
                DataElementPath sequencePath = sequencesCollection.getCompletePath().getChildPath(sequence);
                Region region = new Region(sequencePath.getDataElement(AnnotatedSequence.class));
                region.setInterval(new Interval(from, to));
                project.removeRegion(project.getRegions()[0]);
                project.addRegion(region);
            };
            
            sequenceSelector = new JComboBox<>(new ListComboBoxModel<>(sequencesCollection.getNameList()));
            
            sequenceSelector.addActionListener(e -> {
                String newSequence = (String)sequenceSelector.getSelectedItem();
                String oldSequence = getCurrentSequence();
                if(!newSequence.equals(oldSequence))
                {
                    position.setText(newSequence+":1-"+getVisibleRange().getLength());
                    this.changePositionAction.actionPerformed(e);
                }
            });
            
            add(sequenceSelector);
            add(new JLabel(" "));
            add(new JLabel("Position: "));
            add(position);
            JButton goButton = new JButton("Go");
           
            position.addActionListener(changePositionAction);
            goButton.addActionListener(changePositionAction);
            add(goButton);
            
            project.addPropertyChangeListener(evt -> {
                Interval interval = getVisibleRange();
                String sequenceName = getCurrentSequence();
                position.setText(sequenceName + ":" + interval.getFrom() + "-"
                        + interval.getTo());
                sequenceSelector.setSelectedItem(sequenceName);
            });
        }

        /**
         * @return name of currently selected sequence
         */
        private String getCurrentSequence()
        {
            return project.getRegions()[0].getSequencePath().getName();
        }
    }
}
