
package biouml.plugins.simulation.plot;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;

import biouml.standard.simulation.plot.Plot;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

/**
 * @author anna
 *
 */
public class PlotTableViewPart extends ViewPartSupport
{
    private TablePane tablePane;
    
    public PlotTableViewPart()
    {
        tablePane = new TablePane(500,300);
        JScrollPane scrollPane = new JScrollPane(tablePane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof Plot )
            return true;
        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        if( model instanceof Plot )
        {
            tablePane.setPlot((Plot)model);
            tablePane.updateContents();
        }
    }
}
