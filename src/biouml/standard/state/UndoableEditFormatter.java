
package biouml.standard.state;

import javax.swing.undo.UndoableEdit;

/**
 * Interface for formatter to generate nice UndoableEdit string representation
 * @author anna
 *
 */
public interface UndoableEditFormatter
{
    public boolean isSignificant(UndoableEdit edit);
    public String getPresentationName(UndoableEdit edit);
}
