package biouml.workbench.graph;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Apply layout action. By default apply layout for all
 * selected (and only selected) nodes
 */
public class ExpertLayoutAction extends AbstractAction
{
    public static final String KEY = "Expert settings";

    public static final String VIEW_PANE = "View pane";

    public static final String LAYOUTER_VIEW_PART = "Layouter view part";
    
    public ExpertLayoutAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        LayouterViewPart layouterViewPart = (LayouterViewPart)getValue(ExpertLayoutAction.LAYOUTER_VIEW_PART);
        layouterViewPart.changeExpertMode();
    }

}
