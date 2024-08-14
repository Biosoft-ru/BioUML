package biouml.standard.state;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import biouml.model.Diagram;

public class StatesTab extends JPanel
{
    protected Diagram diagram;
    protected TabularPropertyInspector table;
    protected StateTabListener tabListener;

    public StatesTab(StateTabListener listener)
    {
        this.tabListener = listener;

        setLayout( new BorderLayout() );

        table = new TabularPropertyInspector();
        table.getTable().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        add( table, BorderLayout.CENTER );
        table.explore( new State[] {} );

        table.addListSelectionListener( event -> {
            if( event.getFirstIndex() != -1 )
            {
                Object model = table.getModelOfSelectedRow();
                if( model instanceof State )
                {
                    tabListener.setCurrentState( (State)model );
                }
            }
        } );
    }

    public void refresh(Diagram diagram)
    {
        this.diagram = diagram;
        table.explore( diagram.states().toArray() );
    }

    protected void changeState(State state)
    {
        Object model = table.getModelOfSelectedRow();
        if( model instanceof State && model != state )
        {
            refresh( diagram );
        }
        int i = (int)diagram.states().indexOf( state ).orElse( 0 );
        table.getTable().getSelectionModel().setSelectionInterval( i, i );
    }
}
