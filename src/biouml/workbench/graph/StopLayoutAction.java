package biouml.workbench.graph;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import ru.biosoft.graph.LayoutJobControl;

/**
 * Apply layout action. By default apply layout for all
 * selected (and only selected) nodes
 */
public class StopLayoutAction extends AbstractAction
{
    public static final String KEY = "Stop layout";

    public static final String LAYOUTER_VIEW_PART = "Layouter view part";

    public StopLayoutAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        LayouterViewPart layouterViewPart = (LayouterViewPart)getValue(PrepareLayoutAction.LAYOUTER_VIEW_PART);
        LayoutJobControl jobControl = layouterViewPart.getJobControl();
        jobControl.terminate();
        layouterViewPart.jobTerminated(null);
    }

}
