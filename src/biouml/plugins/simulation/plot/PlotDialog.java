package biouml.plugins.simulation.plot;

import java.util.ResourceBundle;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import biouml.plugins.simulation.resources.MessageBundle;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class PlotDialog extends OkCancelDialog
{
    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected JTabbedPane jTabPanel = new JTabbedPane();
    protected PlotEditorPane editor;
    protected PlotPane chart;
    protected TablePane table;
    
    public PlotDialog(JFrame frame, Plot plot)
    {
        super(frame, "");
        init(new PlotEx(plot));
    }
    
    public PlotDialog(SimulationResult result)
    {
        super(Application.getApplicationFrame(), "");
        init(new PlotEx(result));
    }

    protected void init(PlotEx plotEx)
    {
        setTitle(messageBundle.getResourceString("PLOT_DIALOG_TITLE"));

        editor = new PlotEditorPane(plotEx);
        chart = new PlotPane(700, 500);
        table = new TablePane(700, 500);

        jTabPanel.addTab(messageBundle.getResourceString("PLOT_DESCRIPTION_TAB_TITLE"), editor);
        jTabPanel.addTab(messageBundle.getResourceString("PLOT_PLOT_TAB_TITLE"), chart);
        jTabPanel.addTab(messageBundle.getResourceString("PLOT_TABLE_TAB_TITLE"), table);

        jTabPanel.addChangeListener(e -> {
            if( jTabPanel.getSelectedIndex() == 1 )
            {
                updateChart();
            }
            if( jTabPanel.getSelectedIndex() == 2 )
            {
                updateTable();
            }
        });

        getContentPane().add(jTabPanel);

        updateChart();
        updateTable();
    }

    public void chooseTab(int tabNumber)
    {
        try
        {
            jTabPanel.setSelectedIndex(tabNumber);
        }
        catch( Exception e )
        {
        }
    }

    public void updateChart()
    {
        chart.setPlot(editor.plotEx.getPlot());
        //chart.setResultCollection(editor.plotEx.getSimulationResultCollection());
        //chart.setExperimentCollection(editor.plotEx.getExperimentCollection());
        chart.redrawChart();
    }

    public void updateTable()
    {
        table.setPlot(editor.plotEx.getPlot());
        //table.setResultCollection(editor.plotEx.getSimulationResultCollection());
        //table.setExperimentCollection(editor.plotEx.getExperimentCollection());
        table.updateContents();
    }

    @Override
    public void okPressed()
    {
        editor.plotEx.savePlot();
    }
}
