package biouml.plugins.gtex.meos;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.ResultHandler;
import ru.biosoft.bsa.analysis.maos.SiteMutation;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.datatype.DataType;

public class GTEXResultHandler extends ResultHandler
{
    public GTEXResultHandler(Parameters parameters, Logger analysisLog)
    {
        super( parameters, analysisLog );
        showAlignment = false;
        showMotifLogo = false;
    }
    
    @Override
    protected void createOutputTable()
    {
        super.createOutputTable();
        ColumnModel cm = outputTable.getColumnModel();
        cm.addColumn( "Type", DataType.Text );
        cm.addColumn( "MutatedPosition", DataType.Text );
    }

    @Override
    protected List<Object> createOutTableRow(SiteMutation sm, Site s, SiteModel model)
    {
        List<Object> result = super.createOutTableRow( sm, s, model );
        GTEXSiteMutation gsm = (GTEXSiteMutation)sm;
        result.add( gsm.type );
        result.add( getMutatedPositions2(gsm) );
        return result;
    }

    private String getMutatedPositions(SiteMutation sm)
    {
        List<String> result = new ArrayList<>();
        if( sm.refPos == -1 || sm.altPos == -1 )
        {
            //Insertion or deletion of full site
            for(int i = 0; i < sm.model.getLength(); i++)
                result.add( String.valueOf(i+1) );
        }
        else
        {
            for( int i = 0; i < sm.model.getLength(); i++ )
            {
                byte ref = sm.refSeq.getLetterCodeAt( sm.refPos + i );
                byte alt = sm.altSeq.getLetterCodeAt( sm.altPos + i );
                if( ref != alt )
                    result.add( String.valueOf( i + 1 ) );
            }
        }
        return String.join( ",", result );
    }
    
    private String getMutatedPositions2(SiteMutation sm)
    {
        char[] res = new char[sm.model.getLength()];
        if( sm.refPos == -1 || sm.altPos == -1 )
        {
            //Insertion or deletion of full site
            for(int i = 0; i < sm.model.getLength(); i++)
                res[i] = '1';
        }
        else
        {
            for( int i = 0; i < sm.model.getLength(); i++ )
            {
                byte ref = sm.refSeq.getLetterCodeAt( sm.refPos + i );
                byte alt = sm.altSeq.getLetterCodeAt( sm.altPos + i );
                res[i] = ref == alt ? '0' : '1';
            }
        }
        return new String(res);
    }
}
