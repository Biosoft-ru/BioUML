package ru.biosoft.plugins.graph.testkit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.graph.Graph;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.plugins.graph.GraphViewBuilder;
import ru.biosoft.plugins.graph.GraphViewOptions;
import ru.biosoft.util.ApplicationUtils;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.PanelInfo;
import com.developmentontheedge.application.PanelManager;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

@SuppressWarnings ( "serial" )
public class GraphViewer extends ApplicationFrame
{
    protected static final Logger log = Logger.getLogger(GraphViewer.class.getName());

    public static final String GRAPH_PANE = "GRAPH_PANE";
    public static final String TEXT_PANE = "TEXT_PANE";
    public static final String INFO_PANE = "INFO_PANE";

    protected ViewPane graphPane = new ViewPane();
    protected JPanel editingPane = new JPanel(new BorderLayout(5, 5));
    protected JEditorPane textPane = new JEditorPane();
    protected JTabbedPane tabPane = new JTabbedPane();

    protected Graph graph;
    protected GraphViewOptions viewOptions = new GraphViewOptions();
    protected GraphViewBuilder viewBuilder = new GraphViewBuilder();

    ////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args)
    {
        GraphViewer frame = new GraphViewer();

        Dimension parentSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(parentSize.width - 50, parentSize.height - 50);
        ApplicationUtils.moveToCenter(frame);
        frame.setVisible(true);
    }

    public GraphViewer()
    {
        super(new ActionManager(), null, new PanelManager(), "Graph viewer");

        initActions();
        initPanels();
        initToolbar();

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                dispose();
                System.exit(0);
            }
        });
    }

    /////////////////////////////////////////////////////////////////////////////
    // initialisation
    //

    protected void initPanels()
    {
        // init content panes
        int dividerPos = 450;

        graphPane.setView(new CompositeView());
        PanelInfo graphPaneInfo = new PanelInfo(GRAPH_PANE, graphPane, true, null);
        panelManager.addPanel(graphPaneInfo, null, 0);

        editingPane.add(new JScrollPane(textPane), BorderLayout.CENTER);
        PanelInfo textPaneInfo = new PanelInfo(TEXT_PANE, editingPane, true, null);
        panelManager.addPanel(textPaneInfo, GRAPH_PANE, PanelInfo.BOTTOM, dividerPos);

        TextPaneAppender appender = new TextPaneAppender( new PatternFormatter( "%4$-7s :  %5$s%n" ), "logTextPanel" );
        tabPane.addTab("log", appender.getLogTextPanel());

        PanelInfo tabPaneInfo = new PanelInfo(INFO_PANE, tabPane, true, null);
        panelManager.addPanel(tabPaneInfo, TEXT_PANE, PanelInfo.RIGHT, 750);

        graphPane.setBackground(new Color(255, 255, 240));
    }


    protected void initActions()
    {
        ActionManager actionManager = Application.getActionManager();

        actionManager.addAction(ZoomInAction.KEY, new ZoomInAction());
        actionManager.addAction(ZoomOutAction.KEY, new ZoomOutAction());

        actionManager.addAction(LoadGraphAction.KEY, new LoadGraphAction());
        actionManager.addAction(SaveImageAction.KEY, new SaveImageAction());

        actionManager.initActions(ru.biosoft.plugins.graph.testkit.MessageBundle.class);
    }

    protected static final int GROUP_ZOOM = 1;
    protected static final int GROUP_GRAPH = 2;
    protected static final int GROUP_LAYOUT = 3;
    protected static final int GROUP_PANE = 4;

    protected void initToolbar()
    {
        ActionManager actionManager = Application.getActionManager();
        Action action = null;

        toolBar.addSeparator(GROUP_ZOOM);
        action = actionManager.getAction(ZoomInAction.KEY);
        toolBar.addAction(action);
        action = actionManager.getAction(ZoomOutAction.KEY);
        toolBar.addAction(action);

        toolBar.addSeparator(GROUP_GRAPH);
        action = actionManager.getAction(LoadGraphAction.KEY);
        toolBar.addAction(action);
        action = actionManager.getAction(SaveImageAction.KEY);
        toolBar.addAction(action);
    }

    class ZoomInAction extends AbstractAction
    {
        public static final String KEY = "Zoom in";

        public ZoomInAction()
        {
            super(KEY);
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( graphPane != null )
            {
                graphPane.scale(1.2, 1.2);
            }
        }
    }

    class ZoomOutAction extends AbstractAction
    {
        public static final String KEY = "Zoom out";

        public ZoomOutAction()
        {
            super(KEY);
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( graphPane != null )
            {
                graphPane.scale(1 / 1.2, 1 / 1.2);
            }
        }
    }

    class LoadGraphAction extends AbstractAction
    {
        public static final String KEY = "Load graph";

        public LoadGraphAction()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                graph = new Graph();
                String text = textPane.getText();
                graph.fillFromText(text);
                updateGraphView();

                enableActions();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Loading graph error: " + t, t);
            }
        }
    }

    protected void enableActions()
    {
        ActionManager actionManager = Application.getActionManager();
        actionManager.enableActions( true, ZoomInAction.KEY, ZoomOutAction.KEY, SaveImageAction.KEY );
    }

    class SaveImageAction extends AbstractAction
    {
        public static final String KEY = "save image";
        public static final String IMAGE_EXT = ".gif";

        public SaveImageAction()
        {
            super(KEY);
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(false);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setDialogTitle("Save image");

            chooser.setFileFilter(new FileFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    if( f.isDirectory() )
                    {
                        return true;
                    }
                    else if( f.isFile() )
                    {
                        return f.getName().toLowerCase().endsWith(IMAGE_EXT);
                    }

                    return false;
                }

                @Override
                public String getDescription()
                {
                    return "Image file (.gif)";
                }
            });

            int res = chooser.showSaveDialog(Application.getApplicationFrame());
            if( res == JFileChooser.APPROVE_OPTION )
            {
                File file = chooser.getSelectedFile();
                if( !file.getName().toLowerCase().endsWith(IMAGE_EXT) )
                    file = new File(file.getParentFile(), file.getName() + IMAGE_EXT);

                Image image = generateImage(graphPane.getView());
                encodeImage(file, image);
            }
        }

        private Image generateImage(View view)
        {
            Rectangle r = view.getBounds();
            r.width += 2 * r.x;
            r.height += 2 * r.y;

            BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_BYTE_INDEXED);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.white);
            graphics.fill(new Rectangle(0, 0, r.width, r.height));
            view.paint(graphics);

            return image;
        }
    }

    public static void encodeImage(File file, Image image)
    {
        // TODO:
    }

    /////////////////////////////////////////////////////////////////////////////

    public void updateGraphView()
    {
        CompositeView graphView = viewBuilder.createGraphView(graph, viewOptions, graphPane.getGraphics());
        graphPane.setView(graphView);
    }
}
