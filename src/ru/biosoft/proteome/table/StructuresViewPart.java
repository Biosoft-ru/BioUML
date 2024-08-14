package ru.biosoft.proteome.table;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.logging.Logger;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.document.TableDocument;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * View part for structure viewer
 */
@SuppressWarnings ( "serial" )
public class StructuresViewPart extends ViewPartSupport implements ListSelectionListener
{
    protected Logger log = Logger.getLogger(StructuresViewPart.class.getName());

    private JPanel mainPane;
    private JComboBox<String> modelSelector;
    private Structure3D currentStructure;

    private JmolPanel jmolPanel;

    public StructuresViewPart()
    {
        mainPane = new JPanel();
        add(mainPane, BorderLayout.WEST);

        modelSelector = new JComboBox<>();
        add(modelSelector, BorderLayout.NORTH);

        modelSelector.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String title = (String)modelSelector.getSelectedItem();
                if( title != null && currentStructure != null )
                {
                    for( int i = 0; i < currentStructure.getSize(); i++ )
                    {
                        if( currentStructure.getLink(i).getFirst().equals(title) )
                        {
                            mainPane.removeAll();
                            mainPane.add(new JLabel("Loading..."));
                            mainPane.validate();
                            mainPane.repaint();

                            new Thread(new Loader(currentStructure.getLink(i).getSecond())).start();
                            break;
                        }
                    }
                }
            }
        });

        currentStructure = null;
        jmolPanel = new JmolPanel();
        jmolPanel.setPreferredSize(new Dimension(400, 400));
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.document instanceof TableDocument )
        {
            ( (TableDocument)this.document ).removeSelectionListener(this);
        }
        ( (TableDocument)document ).addSelectionListener(this);

        this.model = model;
        this.document = document;

        updateView();
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof TableDataCollection )
        {
            for( TableColumn column : ( (TableDataCollection)model ).getColumnModel() )
            {
                if( column.getValueClass() == Structure3D.class )
                    return true;
            }
        }
        return false;
    }

    @Override
    public Action[] getActions()
    {
        return new Action[0];
    }

    @Override
    public void valueChanged(ListSelectionEvent arg0)
    {
        updateView();
    }

    protected void updateView()
    {
        List<DataElement> selectedDe = ( (TableDocument)document ).getSelectedItems();
        if( selectedDe.size() > 0 )
        {
            DataElement de = selectedDe.get(0);
            Structure3D newStructure = null;
            for( TableColumn column : ( (TableDataCollection)model ).getColumnModel() )
            {
                if( column.getValueClass() == Structure3D.class )
                {
                    Object structure = ( (RowDataElement)de ).getValue(column.getName());
                    if( structure instanceof Structure3D )
                    {
                        newStructure = (Structure3D)structure;
                    }
                    break;
                }
            }

            if( newStructure == null )
            {
                currentStructure = null;
                mainPane.removeAll();
                mainPane.add(new JLabel("Select row to show correcponding structure"));
                mainPane.validate();
                mainPane.repaint();
                modelSelector.removeAllItems();
                modelSelector.setEnabled(false);
            }
            else if( currentStructure != newStructure )
            {
                currentStructure = newStructure;
                mainPane.removeAll();
                mainPane.add(new JLabel("Loading..."));
                mainPane.validate();
                mainPane.repaint();

                modelSelector.removeAllItems();
                for( int i = 0; i < currentStructure.getSize(); i++ )
                {
                    modelSelector.addItem(currentStructure.getLink(i).getFirst());
                }
                modelSelector.setEnabled(true);

                new Thread(new Loader(currentStructure.getLink(0).getSecond())).start();
            }
        }
    }

    protected static final Map<String, String> dataCache = new WeakHashMap<>();

    class Loader implements Runnable
    {
        protected String link;
        public Loader(String link)
        {
            this.link = link;
        }

        @Override
        public void run()
        {
            try
            {
                String data = dataCache.get(link);
                if( data == null )
                {
                    URL url = new URL(link);
                    data = ApplicationUtils.readAsString(url.openStream());
                    dataCache.put(link, data);
                }
                mainPane.removeAll();
                mainPane.add(jmolPanel);
                mainPane.validate();
                mainPane.repaint();

                jmolPanel.viewer.openStringInline(data);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot open structure view", e);
            }
        }
    }

    static class JmolPanel extends JPanel
    {
        JmolViewer viewer;

        private final Dimension currentSize = new Dimension();

        JmolPanel()
        {
            viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), null, null, null, null, null);
            viewer.setColorBackground("white");
        }

        @Override
        public void paint(Graphics g)
        {
            getSize(currentSize);
            viewer.renderScreenImage(g, currentSize.width, currentSize.height);
        }
    }
}
