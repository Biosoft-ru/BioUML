package biouml.plugins.lucene;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import biouml.model.Module;

public class LuceneSearchAction extends AbstractAction
{
    
    public static final String KEY = "Text Search";
    public static final String LUCENE_FACADE = "luceneFacade";
    public static final String DATA_COLLECTION = "dataCollection";
    
    public LuceneSearchAction ( )
    {
        super ( KEY );
    }
    
    @Override
    public void actionPerformed ( ActionEvent e )
    {
        MessageBundle messageBundle = new MessageBundle ( );
        LuceneSearchView luceneSearch;
        try
        {
            luceneSearch = new LuceneSearchView (
                    messageBundle.getResourceString ( "LUCENE_SEARCH_TITLE" ),
                    ( LuceneQuerySystem ) getValue ( LUCENE_FACADE ),
                    ( Module ) getValue ( DATA_COLLECTION ),
                    LuceneQuerySystem.DEFAULT_FORMATTER );
            luceneSearch.show ( );
        }
        catch ( Exception e1 )
        {
        }

        putValue ( DATA_COLLECTION, null );
        putValue ( LUCENE_FACADE, null );
    }
    
}
