package ru.biosoft.util;

/**
 * This class allows you to store text in the buffer which is limited by the number of rows
 * @author lan
 */
public class LimitedTextBuffer
{
    private int maxRows;
    private StringBuilder sb = new StringBuilder();
    private int nRows = 0;
    private int cutPos = 0;
    
    public LimitedTextBuffer(int maxRows)
    {
        this.maxRows = maxRows;
    }

    /**
     * Add new message to the buffer
     * Class is optimized assuming that number of rows in message << maxRows (ideally one row)
     * @param message to add
     */
    public synchronized void add(String message)
    {
        int newRows = 0;
        // Count '\n' in the new message
        for( int i = message.lastIndexOf('\n', message.length() - 1); i >= 0; i = message.lastIndexOf('\n', i - 1) )
        {
            newRows++;
            // If new message is longer than MAX_MESSAGE_ROWS we ignore the old one completely
            if( newRows > maxRows )
            {
                sb = new StringBuilder(message.substring(i + 1));
                nRows = maxRows;
                cutPos = 0;
                return;
            }
        }
        nRows += newRows;
        if( nRows > maxRows )
        {
            while( true )
            {
                if( sb.charAt(cutPos++) == '\n' )
                {
                    nRows--;
                    if( nRows == maxRows )
                        break;
                }
            }
        }
        sb.append(message);
    }
    
    /**
     * Return last maxRows rows from the buffer (older rows are lost)
     */
    @Override
    public synchronized String toString()
    {
        String message = sb.substring(cutPos);
        sb.delete(0, cutPos);
        cutPos = 0;
        return message;
    }
}
