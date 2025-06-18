package ru.biosoft.access.sql;

import ru.biosoft.exception.InternalException;
import ru.biosoft.util.TextUtil2;

/**
 * This class helps to build SQL query using templates
 * Query object can represent both template (string with placeholders) and the ready-to-execute query (when all placeholders are substituted)
 * Query object is immutable and thread-safe
 * Usage example:
 * SqlUtil.queryInt(connection, new Query("SELECT * FROM $table$ WHERE name=$id$").name("table", myTable).str("id", id));
 * $table$, $id$ are placeholders which can be substituted. The same placeholder can appear several times in the query
 * @author lan
 */
public class Query
{
    private transient String[] lexemes;

    /**
     * Creates new Query on specified template
     * @param template
     */
    public Query(String template)
    {
        this.lexemes = TextUtil2.split(template, '$');
    }

    /**
     * Substitute given placeholder with SQL name (table name, column name, etc.)
     * @param name placeholder to substitute
     * @param value SQL name
     * @return new Query with substituted name
     */
    public Query name(String name, String value)
    {
        return subst(name, SqlUtil.quoteIdentifier(value));
    }

    /**
     * Substitute given placeholder with string literal
     * @param name placeholder to substitute
     * @param value string literal
     * @return new Query with substituted string literal
     */
    public Query str(String name, String value)
    {
        return subst(name, SqlUtil.quoteString(value));
    }

    /**
     * Substitute given placeholder with integer number
     * @param name placeholder to substitute
     * @param value number
     * @return new Query with substituted number
     */
    public Query num(String name, int value)
    {
        return subst(name, String.valueOf(value));
    }

    /**
     * Substitute given placeholder with double number
     * @param name placeholder to substitute
     * @param value number
     * @return new Query with substituted number
     */
    public Query num(String name, double value)
    {
        return subst(name, String.valueOf(value));
    }

    /**
     * Substitute given placeholder with provided string as is (without any escaping). Use with caution!
     * @param name placeholder to substitute
     * @param value string to replace the given placeholder
     * @return new Query with substituted string literal
     */
    public Query raw(String name, String value)
    {
        return subst(name, value);
    }

    /**
     * Substitute the first placeholder with SQL name (table name, column name, etc.)
     * @param value SQL name
     * @return new Query with substituted name
     */
    public Query name(String value)
    {
        return subst(SqlUtil.quoteIdentifier(value));
    }

    /**
     * Substitute the first placeholder with string literal
     * @param value string literal
     * @return new Query with substituted string literal
     */
    public Query str(String value)
    {
        return subst(SqlUtil.quoteString(value));
    }

    /**
     * Substitute the first placeholder with provided string as is (without any escaping). Use with caution!
     * @param value string to replace the first placeholder
     * @return new Query with substituted string literal
     */
    public Query raw(String value)
    {
        return subst(value);
    }

    /**
     * Substitute the first placeholder with integer number
     * @param value number
     * @return new Query with substituted number
     */
    public Query num(int value)
    {
        return subst(String.valueOf(value));
    }
    
    public Query num(long value)
    {
        return subst(String.valueOf(value));
    }

    /**
     * Substitute the first placeholder with double number
     * @param value number
     * @return new Query with substituted number
     */
    public Query num(double value)
    {
        return subst(String.valueOf(value));
    }

    /**
     * Checks whether query is fully compiled (i.e. all placeholders are substituted)
     * @return true if query is fully compiled
     */
    public boolean isCompiled()
    {
        return lexemes.length == 1;
    }

    /**
     * Safely get compiled query only
     * @return query as string if it's compiled
     * @throws InternalException if the query is not compiled
     */
    public String get() throws InternalException
    {
        if(!isCompiled())
            throw new InternalException("Attempt to use not fully substituted query template: "+String.join("$", lexemes));
        return lexemes[0];
    }

    /**
     * For fully-compiled query returns the query
     * Otherwise returns "(template) "+template
     */
    @Override
    public String toString()
    {
        if(isCompiled())
            return lexemes[0];
        return "(template) "+String.join("$", lexemes);
    }

    private Query(String[] lexemes)
    {
        this.lexemes = lexemes;
    }

    private Query subst(String name, String value)
    {
        String[] newLexemes = new String[lexemes.length];
        int j=0;
        String curLexeme = null;
        for(int i=0; i<lexemes.length; i++)
        {
            if(i % 2 == 0)
            {
                curLexeme = curLexeme == null ? lexemes[i] : curLexeme.concat(lexemes[i]);
            } else
            {
                if(lexemes[i].equals(name))
                {
                    curLexeme = curLexeme == null ? value : curLexeme.concat(value);
                } else
                {
                    newLexemes[j++] = curLexeme;
                    newLexemes[j++] = lexemes[i];
                    curLexeme = null;
                }
            }
        }
        if(curLexeme != null)
            newLexemes[j++] = curLexeme;
        if(j == lexemes.length)
            return this;
        String[] tmpLexemes = new String[j];
        System.arraycopy(newLexemes, 0, tmpLexemes, 0, j);
        newLexemes = tmpLexemes;
        return new Query(newLexemes);
    }

    private Query subst(String value)
    {
        if(lexemes.length <= 1)
            return this;
        if(lexemes.length == 2)
            return new Query(new String[] {lexemes[0].concat( value )});
        String[] newLexemes = new String[lexemes.length-2];
        newLexemes[0] = lexemes[0]+value+lexemes[2];
        System.arraycopy(lexemes, 3, newLexemes, 1, lexemes.length-3);
        return new Query(newLexemes);
    }

    private static final Query Q_DESCRIBE = new Query("DESCRIBE $table$");
    private static final Query Q_COUNT = new Query("SELECT COUNT(*) FROM $table$");
    private static final Query Q_ALL = new Query("SELECT * FROM $table$");
    private static final Query Q_SHOW_STATUS = new Query("SHOW TABLE STATUS LIKE $table$");
    private static final Query Q_BY_CONDITION = new Query("SELECT * FROM $table$ WHERE $field$ = $value$");
    private static final Query Q_FIELD = new Query("SELECT $field$ FROM $table$");
    private static final Query Q_SORTED_FIELD = new Query("SELECT $field$ FROM $table$ ORDER BY 1");

    // static methods for common queries

    /**
     * @return compiled Query to describe given table
     */
    public static Query describe(String table)
    {
        return Q_DESCRIBE.name("table", table);
    }

    /**
     * @return compiled Query to count all table rows
     */
    public static Query count(String table)
    {
        return Q_COUNT.name("table", table);
    }

    /**
     * @return compiled Query to select all table rows
     */
    public static Query all(String table)
    {
        return Q_ALL.name("table", table);
    }

    /**
     * @return compiled Query to get table status
     */
    public static Query tableStatus(String table)
    {
        return Q_SHOW_STATUS.str("table", table);
    }

    /**
     * @return compiled Query to select rows for which given field equals to given value
     */
    public static Query byCondition(String table, String field, String value)
    {
        return Q_BY_CONDITION.name("table", table).name("field", field).str("value", value);
    }

    /**
     * @return compiled Query to select given field unconditionally
     */
    public static Query field(String table, String field)
    {
        return Q_FIELD.name("table", table).name("field", field);
    }

    /**
     * @return compiled Query to select given field unconditionally and sort by it
     */
    public static Query sortedField(String table, String field)
    {
        return Q_SORTED_FIELD.name("table", table).name("field", field);
    }
}
