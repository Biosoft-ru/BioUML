package biouml.standard.type.access;

import java.sql.Statement;

import biouml.standard.type.Molecule;

public abstract class MoleculeSqlTransformer<T extends Molecule> extends ConceptSqlTransformer<T>
{
    protected void addStructureReferences(Statement statement, T molecule) throws Exception
    {
        String[] refs = molecule.getStructureReferences();
        if( refs != null)
        {
            for( String ref : refs )
            {
                statement.addBatch(
                    "INSERT INTO structure2molecule (moleculeId, structureId, comment)" + "VALUES(" +
                    validateValue(molecule.getName()) + ", " + validateValue(ref.trim()) + ", " + validateValue(molecule.getComment()) + ")");
            }
        }
    }

    protected void removeStructureReferences(Statement statement, String name) throws Exception
    {
        statement.addBatch("DELETE FROM structure2molecule WHERE moleculeId=" + validateValue(name));
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals("structure2molecule") )
        {
            return "CREATE TABLE `structure2molecule` (" +
                    getIDFieldFormat("moleculeID") + "," +
                    getIDFieldFormat("structureID") + "," +
                    "  `comment` varchar(100) default NULL," +
                    "  `attributes` text,"+
                    "  KEY `IDX_STRUCTURES_2_MOLECULE_MOL_ID` (`moleculeID`)," +
                    "  KEY `IDX_STRUCTURES_2_MOLECULE_STRUCT_ID` (`structureID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
