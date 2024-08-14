package ru.biosoft.plugins.graph;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import ru.biosoft.graph.Layouter;

import com.developmentontheedge.beans.swing.PropertyInspector;
import ru.biosoft.jobcontrol.JobProgressBar;

public class LayouterPane extends JPanel implements LayouterOptionsListener
{

    private List<LayouterOptionsListener> listeners = new ArrayList<>();

    private JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private PropertyInspector inspector = new PropertyInspector();

    private LayouterOptions options;

    public LayouterPane()
    {
        setLayout(new BorderLayout());
        splitPane.setLeftComponent(inspector);
        splitPane.setRightComponent(new JPanel());
        add(splitPane, BorderLayout.CENTER);
    }

    public LayouterPane(Layouter layouter)
    {
        this();

        options = new LayouterOptions(layouter);
        layouterSwitched(layouter);
    }

    public Layouter getLayouter()
    {
        return options.getLayouter();
    }

    @Override
    public void layouterSwitched(Layouter layouter)
    {
        options = new LayouterOptions(layouter);
        options.addListener(this);
        for( LayouterOptionsListener listener : listeners )
            listener.layouterSwitched(layouter);

        inspector.explore(options);
        inspector.expandAll(true);
        repaint();
    }

    public void addListener(LayouterOptionsListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(LayouterOptionsListener listener)
    {
        listeners.remove(listener);
    }

    public void changeExpertMode()
    {
        if( inspector.getPropertyShowMode() == PropertyInspector.SHOW_EXPERT )
        {
            inspector.setPropertyShowMode(PropertyInspector.SHOW_USUAL);
        }
        else
        {
            inspector.setPropertyShowMode(PropertyInspector.SHOW_EXPERT);
        }
        inspector.explore(options);
    }

    /**
     * Set preview text
     */
    public void setPreviewText(String text)
    {
        setPreview(new JLabel(text));
    }

    /**
     * Set preview image
     */
    public void setPreviewImage(BufferedImage image)
    {
        setPreview(new ImagePanel(image));
    }

    /**
     * Set preview progress bar
     */
    public void setPreviewProgressBar(JobProgressBar jpb)
    {
        setPreview(jpb);
    }

    /**
     * Set preview null
     */
    public void resetPreview()
    {
        splitPane.setRightComponent(new JPanel());
        splitPane.invalidate();
    }


    protected void setPreview(Component comp)
    {
        JPanel previewPanel = new JPanel(new GridBagLayout());
        previewPanel.add(comp, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
                0, 0, 0, 0), 0, 0));
        splitPane.setRightComponent(new JScrollPane(previewPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        splitPane.invalidate();
        splitPane.setDividerLocation(getWidth() / 2);
    }

    public static class ImagePanel extends JPanel
    {
        private BufferedImage image;
        public ImagePanel(BufferedImage image)
        {
            this.image = image;
            setPreferredSize(new Dimension(image.getWidth() , image.getHeight()));
            setSize(image.getWidth(), image.getHeight());
        }

        @Override
        public void paintComponent(Graphics g)
        {
            g.drawImage(image, 0, 0, null); // see javadoc for more info on the parameters
        }
    }

    public Dimension getViewDimension()
    {
        return new Dimension(splitPane.getRightComponent().getWidth(), splitPane.getRightComponent().getHeight());
    }
}
