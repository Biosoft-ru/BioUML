package biouml.plugins.lucene;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import biouml.model.Module;

/**
 *
 */
public class IndexEditorAction extends AbstractAction
{
    protected static final MessageBundle messageBundle = new MessageBundle();

    public static final String KEY = "Index Editor";
    public static final String DATA_COLLECTION = "dataCollection";
    public static final String LUCENE_FACADE = "luceneFacade";


    public IndexEditorAction()
    {
        super(KEY);
    }

    /**
     * Dialod which contain IndexEditor panel
     */
    public static class IndexEditorDialog extends JFrame
    {
        public IndexEditorDialog(LuceneQuerySystem luceneFacade, Module module)
        {
            super(messageBundle.getResourceString("LUCENE_INDEX_EDITOR_TITLE"));
            getContentPane().add(new IndexEditor(luceneFacade, module)
            {
                @Override
                protected void cancel()
                {
                    IndexEditorDialog.this.hide();
                }

                @Override
                protected boolean updateIndex() throws Exception
                {
                    if( super.indexWasChanged )
                    {
                        String[] dc = getChangedDC();
                        super.updateIndex();
                        RebuildIndexDialog rebuid = new RebuildIndexDialog(IndexEditorDialog.this, messageBundle
                                .getResourceString("LUCENE_REBUILD_INDEX_TITLE"), dc, luceneFacade, module);
                        rebuid.doModal();
                        return true;
                    }
                    return false;
                }
            });
        }

        public void centerWindow()
        {
            //Center the window
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = getSize();
            if( frameSize.height > screenSize.height )
                frameSize.height = screenSize.height;

            if( frameSize.width > screenSize.width )
                frameSize.width = screenSize.width;

            setLocation( ( screenSize.width - frameSize.width ) / 2, ( screenSize.height - frameSize.height ) / 2);
            setVisible(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        IndexEditorDialog indexEditor = new IndexEditorDialog((LuceneQuerySystem)getValue(LUCENE_FACADE), (Module)getValue(DATA_COLLECTION));
        indexEditor.pack();
        indexEditor.setResizable(false);
        indexEditor.centerWindow();
        putValue(LUCENE_FACADE, null);
        putValue(DATA_COLLECTION, null);
    }
}