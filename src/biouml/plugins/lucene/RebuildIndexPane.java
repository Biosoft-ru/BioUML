package biouml.plugins.lucene;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.swing.PropertyInspector;

@SuppressWarnings ( "serial" )
public class RebuildIndexPane extends JPanel
{
    protected MessageBundle messageBundle = new MessageBundle ( );

    private String[] dc = null;

    private LuceneQuerySystem luceneFacade;

    private PropertyInspector dcInspector = null;
    
    private boolean readOnly = false;

    public RebuildIndexPane ( String[] dc,
            LuceneQuerySystem luceneFacade, boolean readOnly ) throws Exception
    {
        dcInspector = new PropertyInspector ( );
        reinit ( dc, luceneFacade, readOnly );
    }
 
    public PropertyInspector reinit ( String[] dc, LuceneQuerySystem luceneFacade, boolean readOnly )
    {
        this.dc = dc;
        this.readOnly = readOnly;
        this.luceneFacade = luceneFacade;

        // for (int i = 0; i < dc.length; i++)
        // System.out.println ( dc[i] );

        JPanel panel = new JPanel ( );
        panel.setLayout ( new BorderLayout ( ) );
        panel.setBorder ( new EmptyBorder ( 10, 10, 10, 10 ) );

        fullDCInspector ( );
        panel.add ( dcInspector, BorderLayout.CENTER );

        if ( ! readOnly )
        {
            JLabel message = new JLabel ( messageBundle.getResourceString ( "LUCENE_DIALOG_REBUILD_INDEX_QUESTION" ) );
            panel.add ( message, BorderLayout.NORTH );
        }
        
        add ( panel );
        
        return dcInspector;
    }

    private void fullDCInspector ( )
    {
        if ( luceneFacade != null )
        {
            List<String> names = luceneFacade.getCollectionsNamesWithIndexes ( );
            if ( names != null && names.size ( ) > 0 )
            {
                DataCollectionList dpss = new DataCollectionList ( );
                for( String name : names )
                {
                    boolean need = false;
                    if ( dc != null )
                    {
                        for ( int i = 0; i < dc.length; i++ )
                            if ( dc[i] != null && name.equals ( dc[i] ) )
                            {
                                dc[i] = null;
                                need = true;
                                break;
                            }
                    }
                    else
                        need = true;
                    dpss.add(new DynamicProperty(name, Boolean.class, need));
                }
                if ( dc != null )
                {
                    for( String element : dc )
                    {
                        if ( element != null )
                        {
                            DynamicProperty dp = new DynamicProperty ( element, Boolean.class, true );
                            dp.setReadOnly ( readOnly );
                            dpss.add ( dp );
                        }
                    }
                }
                dcInspector.explore ( dpss );
            }
        }
    }
    
    public List<String> getUserChoose ( )
    {
        Object obj = dcInspector.getBean ( );
        List<String> list = new ArrayList<> ( );
        if ( obj instanceof DynamicPropertySet )
        {
            DynamicPropertySet dps = ( DynamicPropertySet ) obj;
            for ( DynamicProperty dp : dps )
            {
                if ( ( ( Boolean ) dp.getValue ( ) ).booleanValue ( ) )
                    list.add ( dp.getName ( ) );
            }
        }
        return list;
    }
        
}
