package biouml.standard.state;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import biouml.model.Diagram;

@SuppressWarnings ( "serial" )
public class ChangesTab extends JPanel
{
    protected Diagram diagram;
    protected TabularPropertyInspector changesTable;
    protected JComboBox<String> statesComboBox;
    protected StateTabListener tabListener;

    protected boolean selectionEnabled = false;

    public ChangesTab(StateTabListener listener)
    {
        this.tabListener = listener;

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        JLabel label1 = new JLabel("States:");
        add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 10,
                0, 10), 0, 0));

        statesComboBox = new JComboBox<>();
        add(statesComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        changesTable = new TabularPropertyInspector();
        add(changesTable, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 0), 0, 0));

        statesComboBox.addActionListener(e -> {
            if( selectionEnabled )
            {
                String stateName = (String)statesComboBox.getSelectedItem();
                State selectedState = diagram.getState( stateName );
                tabListener.setCurrentState(selectedState);
            }
        });
    }

    protected void changeState(State state)
    {
        if( state != null )
        {
            List<UndoableEdit> elements = state.getStateUndoManager().getEdits();
            changesTable.explore(elements.toArray(new UndoableEdit[elements.size()]));

            if( ! ( (String)statesComboBox.getSelectedItem() ).equals(state.getName()) )
            {
                statesComboBox.setSelectedItem(state.getName());
            }
        }
        else
        {
            String selectedItem = (String)statesComboBox.getSelectedItem();
            if( selectedItem != null )
            {
                statesComboBox.setSelectedItem(selectedItem);
            }
        }
    }
    public void refresh(Diagram diagram)
    {
        this.diagram = diagram;
        selectionEnabled = false;
        statesComboBox.removeAllItems();
        changesTable.explore(new Object[] {});
        diagram.states().map( State::getName ).forEach( statesComboBox::addItem );
        selectionEnabled = true;
    }

    /**
     * @return the changesTable
     */
    public TabularPropertyInspector getChangesTable()
    {
        return changesTable;
    }
}
