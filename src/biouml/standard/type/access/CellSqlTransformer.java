package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.Cell;

public class CellSqlTransformer extends ConceptSqlTransformer<Cell>
{
    @Override
    public boolean init(SqlDataCollection<Cell> owner)
    {
        table = "cells";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<Cell> getTemplateClass()
    {
        return Cell.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, completeName, speciesId "
            + "FROM " + table;
    }

    @Override
    protected Cell createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Cell cell = new Cell(owner, resultSet.getString(1));
        cell.setCompleteName(resultSet.getString(6));

        // Cell specific fields
        cell.setSpecies(resultSet.getString(7));

        return cell;
    }

    @Override
    protected String getSpecificFields(Cell cell)
    {
        return ", completeName, speciesId";
    }

    @Override
    protected String[] getSpecificValues(Cell cell)
    {
        return new String[] { cell.getCompleteName(), cell.getSpecies() };
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", "synonyms", table};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `cells` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'compartment-cell'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `speciesID` varchar(50) default NULL," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_cells_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
