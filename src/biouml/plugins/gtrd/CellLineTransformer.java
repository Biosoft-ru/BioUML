package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import biouml.standard.type.Species;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;

public class CellLineTransformer extends SqlTransformerSupport<CellLine>
{
    @Override
    public boolean init(SqlDataCollection<CellLine> owner)
    {
        this.table = "cells";
        return super.init( owner );
    }
    
    @Override
    public Class<CellLine> getTemplateClass()
    {
        return CellLine.class;
    }
    
//    private static final String COLUMNS = "id,title,species,cellosaurus_id,cell_ontology_id,exp_factor_ontology_id,"
//    		+ "uberon_id,source,source_id,cell_type_id,brenda_id";
    private static final String COLUMNS = "id,title,species,cellosaurus_id,cell_ontology_id,exp_factor_ontology_id,uberon_id";
    
    @Override
    public String getSelectQuery()
    {
        return "SELECT " + COLUMNS + " FROM cells";
    }
    
    @Override
    public String getElementQuery(String name)
    {
        int id = -1;
        try
        {
            id = Integer.parseInt( name );
        } catch(NumberFormatException e)
        {
        }
        return "SELECT " + COLUMNS + " FROM cells WHERE id=" + id;
    }

    @Override
    public CellLine create(ResultSet resultSet, Connection connection) throws Exception
    {
        int id = resultSet.getInt( 1 );
        String title = resultSet.getString( 2 );
        Species species = Species.getSpecies( resultSet.getString( 3 ) );
        CellLine result = new CellLine( String.valueOf(id), title, species, owner );
        result.setCellosaurusId( resultSet.getString( 4 ) );
        result.setCellOntologyId( resultSet.getString( 5 ) );
        result.setExpFactorOntologyId( resultSet.getString( 6 ) );
        result.setUberonId( resultSet.getString( 7 ) );
//        result.setSource( resultSet.getString( 8 ) );
//        result.setSourceId( resultSet.getString( 9 ) );
//        result.setCellTypeId( resultSet.getString( 10 ) == null ? null : resultSet.getString( 10 ).split(";") );
//        result.setBrendaId( resultSet.getString( 11 ) );
        return result;
    }
    
    @Override
    public void addInsertCommands(Statement statement, CellLine de) throws Exception
    {
    }

}
