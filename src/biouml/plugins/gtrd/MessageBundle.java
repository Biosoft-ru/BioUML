package biouml.plugins.gtrd;

import java.util.ListResourceBundle;

import ru.biosoft.bsa.BSAMessageBundle;

public class MessageBundle extends ListResourceBundle
{
    public MessageBundle()
    {
        setParent(new BSAMessageBundle());
    }

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            {"PN_REFERENCES", "References"},
            {"PD_REFERENCES", "References to data used for matrix construction"},
            {"PN_UNIPROT", "Uniprot IDs"},
            {"PD_UNIPROT", "Uniprot IDs for matching transcription factor"},
            {"PN_CLASS_REFERENCES", "Classes"},
            {"PD_CLASS_REFERENCES", "Classification elements"}
        };
    }

}
