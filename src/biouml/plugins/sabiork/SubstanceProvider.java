package biouml.plugins.sabiork;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import one.util.streamex.EntryStream;

import org.eml.sdbv.sabioclient.GetAllCompoundIDs;
import org.eml.sdbv.sabioclient.GetCHEBIID;
import org.eml.sdbv.sabioclient.GetCompoundID;
import org.eml.sdbv.sabioclient.GetKEGGID;

import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Substance;
import ru.biosoft.access.core.DataCollection;

public class SubstanceProvider extends ServiceProvider
{
    @Override
    public Substance getDataElement(DataCollection<?> parent, String name) throws RemoteException
    {
        Substance substance = new Substance(parent, name);
        substance.setTitle(name);

        int id = getSabiokrPort().getCompoundID(new GetCompoundID(name)).get_return();

        DatabaseReference[] refs = EntryStream.of( 
                    "KEGG/compound", getSabiokrPort().getKEGGID(new GetKEGGID(id)),
                    "ChEBI", getSabiokrPort().getCHEBIID(new GetCHEBIID(id)) )
                .nonNullValues()
                .flatMapValues( Arrays::stream )
                .mapKeyValue( DatabaseReference::new )
                .toArray( DatabaseReference[]::new );
        substance.setDatabaseReferences(refs);
        return substance;
    }

    @Override
    public List<String> getNameList() throws RemoteException
    {
        GetAllCompoundIDs gc = new GetAllCompoundIDs();
        int[] substanceIDs = getSabiokrPort().getAllCompoundIDs(gc);

        List<String> result = new ArrayList<>();
        for( int id : substanceIDs )
        {
            result.add(Integer.toString(id));
        }
        return result;
    }
}
