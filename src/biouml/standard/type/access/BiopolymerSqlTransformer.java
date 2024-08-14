package biouml.standard.type.access;

import java.sql.Statement;

import biouml.standard.type.Biopolymer;

public abstract class BiopolymerSqlTransformer<T extends Biopolymer> extends MoleculeSqlTransformer<T>
{
    @Override
    public void addInsertCommands(Statement statement, T biopolymer) throws Exception
    {
        super.addInsertCommands(statement, biopolymer);
        statement.addBatch(
            "UPDATE " + table + " SET " +
            " completeName=" + validateValue(biopolymer.getCompleteName()) +
            " ,speciesId=" + validateValue(biopolymer.getSpecies()) +
            " WHERE id=" + validateValue(biopolymer.getName()));
    }
}
