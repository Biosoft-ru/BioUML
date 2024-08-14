package biouml.plugins.sabiork;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eml.sdbv.sabioclient.GetAllEnzymes;

import biouml.standard.type.Protein;

import ru.biosoft.access.core.DataCollection;

/**
 * {@link ServiceProvider} for proteins
 */
public class ProteinProvider extends ServiceProvider
{
    @Override
    public Protein getDataElement(DataCollection<?> parent, String name) throws RemoteException
    {
        return new Protein(parent, name);
    }

    @Override
    public List<String> getNameList() throws RemoteException
    {
        GetAllEnzymes gp = new GetAllEnzymes();
        String[] proteinNames = getSabiokrPort().getAllEnzymes(gp);

        List<String> result = new ArrayList<>();
        for( String name : proteinNames )
        {
            result.add(name);
        }
        return result;
    }
}
