package biouml.plugins.reactome.access;

import ru.biosoft.access.core.DataCollection;

import javax.annotation.Nonnull;

import biouml.plugins.reactome.imports.RSpecies;
import biouml.standard.type.access.TitleSqlIndex;

@SuppressWarnings ( "serial" )
public class ReactomePathwayTitleIndexNew extends TitleSqlIndex
{
    public ReactomePathwayTitleIndexNew(DataCollection dc, String indexName) throws Exception
    {
        super(dc, indexName);
    }

    @Override
    protected String makeQuery(DataCollection dc) throws Exception
    {
        String query = "SELECT db_id,_displayName FROM BioUML_diagrams";
        String speciesInnerId = getSpeciesInnerId( dc );
        if( !speciesInnerId.isEmpty() )
            query += " WHERE species=" + speciesInnerId;
        return query;
    }

    private @Nonnull String getSpeciesInnerId(DataCollection<?> dc)
    {
        String speciesStr = dc.getInfo().getProperty( RSpecies.REACTOME_SPECIES_PROPERTY );
        RSpecies rSpecies = RSpecies.fromString( speciesStr );
        return rSpecies == null ? "" : rSpecies.getInnerId() + "";
    }
}
