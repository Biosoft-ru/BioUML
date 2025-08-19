package ru.biosoft.access.history.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.history.HistoryDataCollection;
import ru.biosoft.access.history.HistoryElement;
import ru.biosoft.access.history.HistoryElementBeanInfo;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.access.history.MessageBundle;
import ru.biosoft.gui.Document;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.action.ActionInitializer;

@SuppressWarnings ( "serial" )
public class HistoryPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(HistoryPane.class.getName());

    public static final String RESTORE = "restore";

    protected static final MessageBundle messageBundle = new MessageBundle();

    protected TabularPropertyInspector historyTable;

    protected RestoreAction restoreAction = new RestoreAction(RESTORE);
    protected Action[] actions;

    protected DataElement de;
    protected Document document;

    protected HistoryElement selectedElement = null;
    protected HistoryElement appliedElement = null;

    protected DataElement currentVersion = null;

    public HistoryPane()
    {
        setLayout(new BorderLayout());

        historyTable = new TabularPropertyInspector();
        JScrollPane scrollPane = new JScrollPane(historyTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        historyTable.setSortEnabled(false);
        historyTable.getTable().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        historyTable.addListSelectionListener(new ListSelectionListener()
        {
            protected int[] currentRows = null;//is used to decline second call of the same event

            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                int[] rows = historyTable.getTable().getSelectedRows();
                if( currentRows != null && currentRows.length == rows.length )
                {
                    boolean theSame = true;
                    for( int i = 0; i < currentRows.length; i++ )
                    {
                        if( currentRows[i] != rows[i] )
                            theSame = false;
                    }
                    if(theSame)
                        return;
                }
                currentRows = rows;
                if( rows.length == 1 )
                {
                    HistoryElement element = (HistoryElement)historyTable.getModelForRow(rows[0]);
                    if( element instanceof HistoryElementStub )
                    {
                        selectedElement = null;
                        restoreDataElement();
                    }
                    else
                    {
                        selectedElement = element;
                        applyHistory(selectedElement);
                    }
                }
                else if(rows.length > 1)
                {
                    HistoryElement from = (HistoryElement)historyTable.getModelForRow(rows[rows.length-1]);
                    HistoryElement to = (HistoryElement)historyTable.getModelForRow(rows[0]);
                    applyHistoryChanges(from, to);
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }
    /**
     * Display history for document
     */
    public void explore(DataElement de, Document document)
    {
        restoreDataElement();

        this.de = de;
        this.document = document;

        List<HistoryElement> historyElements = new ArrayList<>();
        HistoryDataCollection history = HistoryFacade.getHistoryCollection(de);
        if( history != null )
        {
            try
            {
                HistoryElementStub currentHistory = new HistoryElementStub();
                historyElements.add(currentHistory);
                for( String id : history.getHistoryElementNames(DataElementPath.create(de), 0) )
                {
                    HistoryElement he = (HistoryElement)history.get(id);
                    if( he != null )
                    {
                        if( he.getVersion() >= currentHistory.getVersion() )
                            currentHistory.setVersion(he.getVersion() + 1);
                        historyElements.add(he);
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot get history for element: " + de.getName(), e);
            }
        }
        historyTable.explore(historyElements.iterator());
        selectedElement = null;
    }
    /**
     * Get action list
     */
    public Action[] getActions()
    {
        if( actions == null )
        {
            new ActionInitializer(messageBundle.getClass()).initAction(restoreAction, RESTORE);

            actions = new Action[] {restoreAction};
        }
        return actions;
    }

    protected DataElement getVersion(HistoryElement historyElement)
    {
        return historyElement instanceof HistoryElementStub?de:HistoryFacade.getVersion(de, historyElement.getVersion());
    }

    protected void applyHistory(HistoryElement historyElement)
    {
        appliedElement = historyElement;
        try
        {
            currentVersion = getVersion(historyElement);
            document.setModel(currentVersion);
            document.update();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot apply history element", e);
        }
    }

    protected void applyHistoryChanges(HistoryElement from, HistoryElement to)
    {
        DataElement first = getVersion(from);
        DataElement second = getVersion(to);
        currentVersion = HistoryFacade.getDiffElement(first, second);
        document.setModel(currentVersion);
        document.update();
    }

    protected void restoreDataElement()
    {
        if( currentVersion != null )
        {
            document.setModel(de);
            document.update();
            appliedElement = null;
            currentVersion = null;
        }
    }

    public static class HistoryElementStub extends HistoryElement
    {
        public HistoryElementStub()
        {
            super(null, "-current-");
            setVersion(-1);
        }

        @Override
        public String getTimestampFormated()
        {
            return "current";
        }
    }

    public static class HistoryElementStubBeanInfo extends HistoryElementBeanInfo
    {

    }

    //
    // Action classes
    //

    class RestoreAction extends AbstractAction
    {
        public RestoreAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            historyTable.getTable().setRowSelectionInterval(0, 0);
            restoreDataElement();
        }
    }
}
