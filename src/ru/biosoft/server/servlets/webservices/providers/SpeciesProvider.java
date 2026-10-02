package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.standard.type.Species;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;

public class SpeciesProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();
        switch (action)
        {
        case "detect":
            processDetectSpecies( arguments, response );
            break;
        case "set":
            processSetSpecies( arguments, response );
            break;
        default:
            throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "action" );
        }

    }

    private void processSetSpecies(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        DataElementPath dePath = arguments.getDataElementPath();
        DataElement de = dePath.optDataElement();
        if( de == null )
            throw new WebException( "EX_QUERY_NO_ELEMENT", dePath );
        String speciesStr = arguments.get( "species" );
        setSpecies( de, speciesStr );
        response.sendString( "ok" );
    }

    public static void setSpecies(DataElement de, String speciesStr)
    {
        DataElementPath dePath = de.getCompletePath();
        DataCollection<?> parentDC = dePath.getParentCollection();
        Species species = speciesStr == null ? null : Species.getSpecies( speciesStr );
        if( species != null )
        {
            try
            {
                DataCollection<?> primaryParent = (DataCollection<?>) SecurityManager.runPrivileged( () -> {
                    return DataCollectionUtils.fetchPrimaryCollectionPrivileged( parentDC );
                } );
                if( primaryParent instanceof GenericDataCollection )
                {
                    ((GenericDataCollection) primaryParent).setChildProperty( de.getName(), DataCollectionUtils.SPECIES_PROPERTY, species.getLatinName() );
                }
                if( de instanceof DataCollection )
                {
                    ((DataCollection) de).getInfo().getProperties().setProperty( DataCollectionUtils.SPECIES_PROPERTY, species.getLatinName() );
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    private void processDetectSpecies(BiosoftWebRequest arguments, JSONResponse response) throws IOException, WebException
    {
        DataElementPath dePath = arguments.getDataElementPath();
        DataElement de = dePath.optDataElement();
        if( de == null )
            throw new WebException( "EX_QUERY_NO_ELEMENT", dePath );
        Species species = null;
        try
        {
            species = getSpecies( de );
        }
        catch (Exception e)
        {
            log.log( Level.SEVERE, "Can not get species for " + de.getName(), e );
        }
        JSONObject result = new JSONObject();
        JSONArray all = new JSONArray();
        Species.allSpecies().map( Species::getName ).forEach( sp -> all.put( sp ) );
        result.put( "current", species == null ? "" : species.getLatinName() );
        result.put( "all", all );
        response.sendJSON( result );
    }

    public static Species getSpecies(DataElement de) throws Exception
    {

        String speciesStr = null;
        if( de instanceof DataCollection )
        {
            speciesStr = ((DataCollection) de).getInfo().getProperty( DataCollectionUtils.SPECIES_PROPERTY );
        }
        else
        {
            DataCollection<?> parent = de.getCompletePath().getParentCollection();
            DataCollection<?> primaryParent = (DataCollection<?>) SecurityManager.runPrivileged( () -> {
                return DataCollectionUtils.fetchPrimaryCollectionPrivileged( parent );
            } );
            if( primaryParent instanceof GenericDataCollection )
            {
                GenericDataCollection genericParent = (GenericDataCollection) primaryParent;
                speciesStr = genericParent.getChildProperty( de.getName(), DataCollectionUtils.SPECIES_PROPERTY );
            }
        }
        Species species = speciesStr == null ? null : Species.getSpecies( speciesStr );
        return species;
    }

}
