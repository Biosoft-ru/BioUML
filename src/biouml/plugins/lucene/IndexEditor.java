package biouml.plugins.lucene;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import biouml.model.Module;

/**
 * Index editor dialog panel
 */
@SuppressWarnings ( "serial" )
public abstract class IndexEditor extends JPanel
{
    protected static final MessageBundle messageBundle = new MessageBundle();

    //colelction names
    protected String[] collectionsNames = null;
    protected int index = -1;

    protected JComboBox<String> dataCollectionSelector = null;
    protected JList<String> properties = null;
    protected JEditorPane indexes = null;

    protected JButton save = null;
    protected JButton update = null;

    protected boolean indexWasChanged = false;
    protected boolean currentIndexWasChanged = false;
    protected TreeMap<Integer, String> changedDC = new TreeMap<>();

    protected LuceneQuerySystem luceneFacade = null;
    protected Module module = null;

    protected boolean lock = false;

    public IndexEditor(LuceneQuerySystem luceneFacade, Module module)
    {
        if( luceneFacade == null )
            throw new NullPointerException("Invalid lucene facade");

        if( module == null )
            throw new NullPointerException("Invalid module");

        //init variables
        this.luceneFacade = luceneFacade;
        this.module = module;
        try
        {
            collectionsNames = LuceneUtils.getCollectionsNames(module, null).toArray(new String[0]);
        }
        catch( Exception e )
        {
            collectionsNames = new String[0];
        }

        //init layout
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(3, 5, 3, 5));

        //////////////////////////////////////////////////////////
        //add buttons
        //

        JPanel bottom = new JPanel(new FlowLayout());
        save = new JButton(messageBundle.getResourceString("BUTTON_SAVE"));
        save.setActionCommand("save");
        save.addActionListener(e -> {
            if( "save".equals(e.getActionCommand()) )
                try
                {
                    if( currentIndexWasChanged )
                    {
                        changedDC.put(dataCollectionSelector.getSelectedIndex(), indexes.getText());
                        currentIndexWasChanged = false;
                    }
                    saveIndex();
                }
                catch( Throwable t )
                {
                }
        });
        save.setEnabled(false);
        bottom.add(save);
        update = new JButton(messageBundle.getResourceString("BUTTON_UPDATE_INDEX"));
        update.setActionCommand("update");
        update.addActionListener(e -> {
            if( "update".equals(e.getActionCommand()) )
                try
                {
                    if( currentIndexWasChanged )
                    {
                        //System.out.println ( getDataCollection( dataCollectionSelector.getSelectedIndex() ) );
                        changedDC.put(dataCollectionSelector.getSelectedIndex(), indexes.getText());
                        currentIndexWasChanged = false;
                    }
                    updateIndex();
                }
                catch( Throwable t )
                {
                }
        });
        update.setEnabled(false);
        bottom.add(update);
        JButton cancel = new JButton(messageBundle.getResourceString("BUTTON_CANCEL"));
        cancel.setActionCommand("cancel");
        cancel.addActionListener(e -> {
            if( "cancel".equals(e.getActionCommand()) )
                try
                {
                    cancel();
                }
                catch( Throwable t )
                {
                }
        });
        bottom.add(cancel);
        add(bottom, BorderLayout.SOUTH);


        //////////////////////////////////////////////////////////
        //check if this module have lucene index directory property
        //

        if( luceneFacade.testHaveLuceneDir() )
        {
            JPanel indexesEditor = getIndexesEditor();
            add(indexesEditor, BorderLayout.CENTER);

            JPanel top = getDataCollectionChooser();
            add(top, BorderLayout.NORTH);
        }
        else
        {
            //this modul doesn't have lucene index directory
            //it's possible when this panel was used incorrectly
            add(getErrorPanel(messageBundle.getResourceString("LUCENE_EMPTY_LUCENE_DIR")), BorderLayout.CENTER);
        }
    }

    protected void saveIndex() throws Exception
    {
        updateIndex();
        cancel();
    }

    protected String[] getChangedDC()
    {
        String[] dc = new String[changedDC.size()];

        int i = 0;
        for( Integer key : changedDC.keySet() )
        {
            dc[i] = getDataCollection(key.intValue());
        }

        return dc;
    }

    protected boolean updateIndex() throws Exception
    {
        if( indexWasChanged )
        {
            for( Entry<Integer, String> entry : changedDC.entrySet() )
            {
                String indexes = entry.getValue();
                String dc = getDataCollection(entry.getKey());
                if( dc != null )
                    luceneFacade.setIndexes(dc, indexes);
            }
            indexWasChanged = false;
            currentIndexWasChanged = false;
            changedDC.clear();
            save.setEnabled(false);
            update.setEnabled(false);
            return true;
        }
        return false;
    }

    protected abstract void cancel();

    protected void selectDC(int index)
    {
        if( this.index != index )
        {
            int oldDCIndex = this.index;
            String oldIndexes = indexes.getText();

            String dc = getDataCollection(index);
            if( dc == null )
                return;

            currentIndexWasChanged = false;

            Vector<String> prop = luceneFacade.getPropertiesNames(dc);
            properties.setListData(prop);

            properties.setPreferredSize(new Dimension(140, properties.getPreferredScrollableViewportSize().height
                    / properties.getVisibleRowCount() * prop.size()));

            String text = changedDC.get(index);
            if( text == null )
                text = luceneFacade.getIndexes(dc);

            this.index = index;
            lock = true;
            indexes.setText(text);
            lock = false;

            if( oldDCIndex >= 0 && oldIndexes != null && oldDCIndex != index )
            {
                //System.out.println ( getDataCollection ( index ) );
                if( oldIndexes.trim().length() > 0 )
                    changedDC.put(oldDCIndex, oldIndexes);
            }
        }
    }

    protected String getDataCollection(int index)
    {
        if( index < 0 )
            return null;
        return collectionsNames[index];
    }

    protected JComponent getErrorPanel(String text)
    {
        JLabel error = new JLabel(text);
        return error;
    }

    protected JPanel getDataCollectionChooser()
    {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        if( collectionsNames != null && collectionsNames.length > 0 )
        {
            JLabel text = new JLabel(messageBundle.getResourceString("LUCENE_DATA_COLLECTION_COOSER_NAME"));
            top.add(text);
            dataCollectionSelector = new JComboBox<>(collectionsNames);
            dataCollectionSelector.setActionCommand("select");
            dataCollectionSelector.addActionListener(e -> {
                if( "select".equals(e.getActionCommand()) )
                    try
                    {
                        boolean desableAll = !indexWasChanged;
                        selectDC(dataCollectionSelector.getSelectedIndex());
                        if( desableAll )
                        {
                            save.setEnabled(false);
                            update.setEnabled(false);
                            indexWasChanged = false;
                        }
                    }
                    catch( Throwable t )
                    {
                    }
            });
            dataCollectionSelector.setSelectedIndex(0);
            top.add(dataCollectionSelector);

            save.setEnabled(false);
            update.setEnabled(false);
            indexes.getAccessibleContext().addPropertyChangeListener(arg0 -> {
                if( arg0 != null )
                    if( AccessibleContext.ACCESSIBLE_TEXT_PROPERTY.equalsIgnoreCase(arg0.getPropertyName()) )
                    {
                        if( !lock )
                        {
                            //System.out.println ( getDataCollection ( dataCollectionSelector.getSelectedIndex ( ) ) );
                            save.setEnabled(true);
                            update.setEnabled(true);
                            indexWasChanged = true;
                            currentIndexWasChanged = true;
                        }
                        //indexes.getAccessibleContext().removePropertyChangeListener(this);
                    }
            });
            selectDC(0);
        }
        else
        {
            top.add(getErrorPanel(messageBundle.getResourceString("LUCENE_EMPTY_DATABASE")));
        }

        return top;
    }

    protected JPanel getIndexesEditor()
    {
        JPanel editor = new JPanel(new BorderLayout());
        editor.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        JLabel text = new JLabel(messageBundle.getResourceString("LUCENE_PROPERTIES_LIST_NAME"));
        editor.add(text, BorderLayout.NORTH);

        properties = new JList<>();
        properties.setPreferredSize(new Dimension(140, properties.getPreferredScrollableViewportSize().height
                / properties.getVisibleRowCount() * 10));
        properties.repaint();
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(properties);
        //scrollPane.getViewport().setView(properties);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editor.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        indexes = new JEditorPane();
        indexes.setContentType("text/plain");
        indexes.setPreferredSize(new Dimension(140, indexes.getPreferredSize().height * 3));
        scrollPane = new JScrollPane(indexes);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        bottom.add(scrollPane, BorderLayout.CENTER);
        text = new JLabel(messageBundle.getResourceString("LUCENE_INDEXES"));
        bottom.add(text, BorderLayout.NORTH);
        editor.add(bottom, BorderLayout.SOUTH);


        JPanel editorEx = new JPanel();
        editorEx.setLayout(new BorderLayout());
        editorEx.add(editor, BorderLayout.CENTER);

        JButton addButton = new JButton(messageBundle.getResourceString("BUTTON_ADD"));
        addButton.setActionCommand("add");
        addButton.addActionListener(e -> {
            if( "add".equals(e.getActionCommand()) )
            {
                ListSelectionModel model = properties.getSelectionModel();
                if( model == null )
                    return;

                if( model.isSelectionEmpty() )
                    return;

                ListModel<String> editorModel = properties.getModel();
                ArrayList<String> selectedItems = new ArrayList<>();
                for( int i = 0; i < editorModel.getSize(); i++ )
                {
                    if( model.isSelectedIndex(i) )
                        selectedItems.add(editorModel.getElementAt(i));
                }

                if( !selectedItems.isEmpty() )
                {
                    indexes.setText( LuceneUtils.indexedFields( indexes.getText() )
                        .append( selectedItems ).distinct().joining( ";" ) );
                }
            }
        });
        JPanel addPanel = new JPanel();
        addPanel.setLayout(new BorderLayout());
        addPanel.add(addButton, BorderLayout.EAST);
        editorEx.add(addPanel, BorderLayout.NORTH);

        return editorEx;
    }


}
