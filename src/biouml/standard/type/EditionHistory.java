package biouml.standard.type;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * General class defining editing history.
 */
public class EditionHistory implements PropertyChangeListener
{
    /**
     * @todo implement
     */
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {}

    ////////////////////////////////////////////////////////////////////////////
    //
    //

    public static class Edition
    {
        private String  date;
        public String   getDate()                   { return date; }
        public void     setDate(String date)        { this.date = date; }

        private String  author;
        public String   getAuthor()                 { return author; }
        public void     setAuthor(String author)    { this.author = author; }

        private String  type;
        public String   getType()                   { return type; }
        public void     setType(String type)        { this.type = type; }
    }
}
