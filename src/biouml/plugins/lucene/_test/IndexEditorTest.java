package biouml.plugins.lucene._test;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import biouml.model.Module;
import biouml.plugins.lucene.IndexEditor;
import biouml.plugins.lucene.LuceneQuerySystem;
import biouml.plugins.lucene.RebuildIndexDialog;

public class IndexEditorTest extends JFrame
{
    
    public IndexEditorTest(LuceneQuerySystem luceneFacade, Module module)
    {
        super("Index Editor");
        getContentPane().add(new IndexEditor(luceneFacade, module)
            {
                @Override
                protected void cancel()
                {
                    IndexEditorTest.this.hide();
                }
                                    
                @Override
                protected boolean updateIndex() throws Exception
                {
                    if (super.indexWasChanged)
                    {
                        String[] dc = getChangedDC();
                        super.updateIndex();
                        RebuildIndexDialog rebuid = new RebuildIndexDialog(IndexEditorTest.this, "Rebuld Index", dc, luceneFacade, module);
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
        if (frameSize.height > screenSize.height)
            frameSize.height = screenSize.height;

        if (frameSize.width > screenSize.width)
            frameSize.width = screenSize.width;

        setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        setVisible(true);
    }
            
    @Override
    protected void processWindowEvent(WindowEvent e)
    {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
            System.exit(0);
    }
    
}
