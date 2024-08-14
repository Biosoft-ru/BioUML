package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.SqlTransformerSupport;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.Unit;

public class UnitSqlTransformer extends SqlTransformerSupport<Unit>
{
    public UnitSqlTransformer()
    {
        table = "units";
    }

    @Override
    public Class<Unit> getTemplateClass()
    {
        return Unit.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, name, comment FROM " + table;
    }


    @Override
    public Unit create(ResultSet resultSet, Connection connection) throws Exception
    {
        Unit unit = new Unit(owner, resultSet.getString(1));

        // common fields
        unit.setTitle(resultSet.getString(2));
        unit.setComment(resultSet.getString(3));

        // retrieve base units
        try (Statement statement = connection.createStatement();
                ResultSet baseUnitsSet = statement.executeQuery(
                        "SELECT baseUnit.type, baseUnit.scale, baseUnit.exponent, baseUnit.multiplier FROM baseUnits baseUnit "
                                + "WHERE baseUnit.unitId=" + validateValue( unit.getName() ) ))
        {
            List<BaseUnit> baseUnits = new ArrayList<>();
            while( baseUnitsSet.next() )
            {
                BaseUnit baseUnit = new BaseUnit();
                baseUnit.setType( baseUnitsSet.getString( 1 ) );
                baseUnit.setScale( Integer.parseInt( baseUnitsSet.getString( 2 ) ) );
                baseUnit.setExponent( Integer.parseInt( baseUnitsSet.getString( 3 ) ) );
                baseUnit.setMultiplier( Double.parseDouble( baseUnitsSet.getString( 4 ) ) );

                baseUnits.add( baseUnit );
            }
            if( baseUnits.size() > 0 )
                unit.setBaseUnits( baseUnits.toArray( new BaseUnit[baseUnits.size()] ) );
        }
        return unit;
    }

    @Override
    public void addInsertCommands(Statement statement, Unit unit) throws Exception
    {
        StringBuffer result = new StringBuffer("INSERT INTO " + table + " (id, name, comment) VALUES(");

        result.append(validateValue(unit.getName()));
        result.append(", " + validateValue(unit.getTitle()));
        result.append(", " + validateValue(unit.getComment()));
        result.append(")");

        statement.addBatch(result.toString());

        addInsertBaseUnitsCommands(statement, unit);
    }

    protected void addInsertBaseUnitsCommands(Statement statement, Unit unit) throws Exception
    {
        //insert into baseUnits
        if( unit.getBaseUnits() != null )
        {
            for( BaseUnit baseUnit : unit.getBaseUnits() )
            {
                StringBuffer buf = new StringBuffer("INSERT INTO baseUnits (unitId, type, scale, exponent, multiplier) VALUES(");
                buf.append("'" + unit.getName());
                buf.append("', '" + baseUnit.getType());
                buf.append("', '" + baseUnit.getScale());
                buf.append("', '" + baseUnit.getExponent());
                buf.append("', '" + baseUnit.getMultiplier());
                buf.append("')");

                statement.addBatch(buf.toString());
            }
        }
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        statement.addBatch("DELETE FROM " + table + " WHERE " + idField + "=" + validateValue(name));
        statement.addBatch("DELETE FROM baseUnits WHERE unitId=" + validateValue(name));
    }

    @Override
    public String[] getUsedTables()
    {
        return new String[] {table, "baseUnits"};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `units` ("
                    + "  `ID` varchar(50) NOT NULL default '',"
                    + "  `name` varchar(50) default NULL,"
                    + "  `comment` varchar(250) default NULL,"
                    + "  UNIQUE KEY `IDX_UNIQUE_units_ID` (`ID`)"
                    + ") ENGINE=MyISAM";
        }
        if( tableName.equals("baseUnits") )
        {
            return "CREATE TABLE `baseUnits` ("
                    + "  `ID` bigint(20) unsigned NOT NULL auto_increment,"
                    + "  `unitID` varchar(50) default NULL,"
                    + "  `type` varchar(50) default " + validateValue(Unit.getBaseUnitsList().get(0)) + ","
                    + "  `exponent` int(10) NOT NULL default '1',"
                    + "  `scale` int(10) NOT NULL default '0',"
                    + "  `multiplier` double NOT NULL default '1',"
                    + "  UNIQUE KEY `IDX_UNIQUE_baseUnits_ID` (`ID`)"
                    + ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
