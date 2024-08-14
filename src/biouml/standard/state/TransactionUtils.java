package biouml.standard.state;

import javax.swing.undo.UndoableEdit;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.undo.Transaction;

/**
 * @author anna
 *
 */
public class TransactionUtils
{
    public static StreamEx<UndoableEdit> editsFlat(UndoableEdit edit)
    {
        return StreamEx.ofTree( edit, Transaction.class, t -> t.getEdits().stream() );
    }
    
    public static String getTransactionDescription(Transaction t, UndoableEditFormatter formatter)
    {
        return editsFlat(t).filter( formatter::isSignificant )
            .map( formatter::getPresentationName ).nonNull().joining( "\n" );
    }
}
