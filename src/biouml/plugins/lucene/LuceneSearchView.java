package biouml.plugins.lucene;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.ExplorerPane;
import ru.biosoft.util.ApplicationUtils;
import biouml.model.Module;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;

/**
 * Search view for lucene search engine
 */
public class LuceneSearchView extends JFrame
{
    protected Logger log = Logger.getLogger( LuceneSearchView.class.getName() );

    protected TabularPropertyInspector tabularInspector = null;
    protected ExplorerPane explorerPane = null;

    public LuceneSearchView ( String title,
            LuceneQuerySystem luceneFacade,
            Module module,
            Formatter formatter ) throws Exception
    {
        super ( title );

        tabularInspector = new TabularPropertyInspector ( );
        explorerPane = new ExplorerPane ( );

        LuceneSearchPane settingsPane = createSettingsPane ( module, luceneFacade, formatter );

        JSplitPane verticalSplitter = new JSplitPane ( JSplitPane.VERTICAL_SPLIT, settingsPane, explorerPane );

        verticalSplitter.setDividerSize ( 4 );

        JSplitPane horizontalSplitter = new JSplitPane ( JSplitPane.HORIZONTAL_SPLIT, tabularInspector, verticalSplitter );

        verticalSplitter.setDividerLocation(300);
        horizontalSplitter.setDividerLocation(700);
        verticalSplitter.setDividerSize(4);
        horizontalSplitter.setDividerSize(4);

        //settingsPane.setPreferredSize ( settingsPane.getPreferredSize ( ).width, verticalSplitter.getTopComponent ( ).getSize ( ).height );

        setContentPane ( horizontalSplitter );
    }

    @Override
    public void show ( )
    {
        pack ( );
        Dimension parentSize = null;
        ApplicationFrame applicationFrame = Application.getApplicationFrame ( );
        if ( applicationFrame != null )
        {
            parentSize = applicationFrame.getSize ( );
        }
        else
        {
            parentSize = Toolkit.getDefaultToolkit ( ).getScreenSize ( );
        }
        setSize ( parentSize.width - 40, parentSize.height - 40 );
        ApplicationUtils.moveToCenter ( this );
        super.show ( );
    }

    protected LuceneSearchPane createSettingsPane( Module module, LuceneQuerySystem luceneFacade, Formatter formatter ) throws Exception
    {
        LuceneSearchPane panel = new LuceneSearchPane ( this, luceneFacade, module, formatter, tabularInspector, explorerPane, log, false )
            {
                @Override
                protected void cancelPressed ( )
                {
                    LuceneSearchView.this.hide ( );
                }
                @Override
                protected void prepareSelectItem ( )
                {
                }
                @Override
                protected void selectItem ( ru.biosoft.access.core.DataElement de )
                {
                }
                @Override
                protected void afterSearch ( )
                {
                }
            };
        return panel;
    }

}