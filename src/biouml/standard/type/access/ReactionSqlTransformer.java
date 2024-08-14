package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class ReactionSqlTransformer extends ReferrerSqlTransformer<Reaction>
{
    @Override
    public boolean init(SqlDataCollection<Reaction> owner)
    {
        table = "reactions";
        this.owner = owner;
        checkAttributesColumn(owner);
        checkAttributesColumn(owner, "reactionComponents");
        return true;
    }

    @Override
    public Class<Reaction> getTemplateClass()
    {
        return Reaction.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, " +
               " fast, reversible, " +
               " formula, timeUnits, substanceUnits, kineticLowComment " +
               "FROM " + table + "";
    }

    @Override
    protected Reaction createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Reaction reaction = new Reaction(owner, resultSet.getString(1));

        // Reaction specific fields
        reaction.setFast        ( resultSet.getBoolean(6) );
        reaction.setReversible  ( resultSet.getBoolean(7) );

        KineticLaw kineticLaw = new KineticLaw(reaction);
        reaction.setKineticLaw(kineticLaw);
        kineticLaw.setFormula        (resultSet.getString(8));
        kineticLaw.setTimeUnits      (resultSet.getString(9));
        kineticLaw.setSubstanceUnits (resultSet.getString(10));
        kineticLaw.setComment        (resultSet.getString(11));


        return reaction;
    }

    @Override
    public Reaction create(ResultSet resultSet, Connection connection) throws Exception
    {
        Reaction reaction = super.create(resultSet, connection);

        // retrieve specie references
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery( "SELECT id, specieId, participation, role, modifierAction, "
                        + "stoichiometry, comment, attributes FROM reactionComponents rc " + "WHERE rc.reactionID="
                        + validateValue( reaction.getName() ) ))
        {
            List<SpecieReference> refs = new ArrayList<>();
            while( rs.next() )
            {
                SpecieReference ref = new SpecieReference(reaction, rs.getString(1));

                ref.setSpecie         ( rs.getString(2) );
                ref.setParticipation  ( rs.getString(3) );
                ref.setRole           ( rs.getString(4) );
                ref.setModifierAction ( rs.getString(5) );
                ref.setStoichiometry  ( rs.getString(6) );
                ref.setComment        ( rs.getString(7) );
                loadAttributes(rs.getString(8), ref.getAttributes());

                refs.add(ref);
            }

            if( refs.size() > 0 )
                reaction.setSpecieReferences(refs.toArray(new SpecieReference[refs.size()]));
        }
        return reaction;
    }

    @Override
    protected String getSpecificFields(Reaction reaction)
    {
        String str = ", fast, reversible ";
        if( reaction.getKineticLaw() != null )
            str = str + ", formula, timeUnits, substanceUnits, kineticLowComment ";
        return str;
    }

    @Override
    protected String[] getSpecificValues(Reaction reaction)
    {
        if( reaction.getKineticLaw() == null )
            return new String[] { "" + reaction.isFast(),
                                  "" + reaction.isReversible()};
        KineticLaw law = reaction.getKineticLaw();
        return new String[] { "" + booleanToYesNo(reaction.isFast()),
                              "" + booleanToYesNo(reaction.isReversible()),
                              law.getFormula(),
                              law.getTimeUnits(),
                              law.getSubstanceUnits(),
                              law.getComment()
                              };
    }
    
    private String booleanToYesNo(boolean value)
    {
        if(value)
        {
            return "yes";
        }
        return "no";
    }

    ///////////////////////////////////////////////////////////////////////////
    // Read/write specie references
    //

    @Override
    public void addInsertCommands(Statement statement, Reaction reaction) throws Exception
    {
        super.addInsertCommands(statement, reaction);

        SpecieReference[] references = reaction.getSpecieReferences();
        for( SpecieReference ref : references )
        {
            String entityId = ref.getSpecieName();
            String participation = ref.getParticipation();
            String role = ref.getRole();
            String modifierAction = ref.getModifierAction();

            StringBuffer buf = new StringBuffer(
                    "INSERT INTO reactionComponents " +
                    "(id, reactionId, entityId, specieId");

            if( participation != null )
                buf.append( ", participation" );
            if( role != null )
                buf.append( ", role" );
            if( modifierAction != null )
                buf.append( ", modifierAction" );

            String attrStr = getAttributesString( ref.getAttributes() );
            if( attrStr != null )
                buf.append( ", attributes" );

            buf.append( ", stoichiometry, comment) VALUES(" );
            buf.append( validateValue( ref.getName() ) );
            buf.append( ", " + validateValue( reaction.getName() ) );
            buf.append( ", " + validateValue( entityId ) );
            buf.append( ", " + validateValue( ref.getSpecie() ) );

            if( participation != null )
                buf.append( ", " + validateValue( participation ) );
            if( role != null )
                buf.append( ", " + validateValue( role ) );
            if( modifierAction != null )
                buf.append( ", " + validateValue( modifierAction ) );
            if( attrStr != null )
                buf.append( ", " + validateValue( attrStr ) );
            else
                buf.append( "" );
            buf.append( ", " + validateValue( ref.getStoichiometry() ) );
            buf.append( ", " + validateValue( ref.getComment() ) );
            buf.append( ")" );

            statement.addBatch( buf.toString() );
        }
    }

    /**
     * Adds set of SQL commands to the statement to remove data element from the table.
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which DELETE statements will be generated.
     */
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        super.addDeleteCommands(statement, name);
        statement.addBatch("DELETE FROM reactionComponents WHERE reactionId=" + validateValue(name));
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", "reactionComponents", table};
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `reactions` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'relation-semantic'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `formula` varchar(250) default NULL," +
                    "  `timeUnits` varchar(50) default NULL," +
                    "  `substanceUnits` varchar(250) default NULL," +
                    "  `kineticLowComment` text," +
                    "  `fast` enum('yes','no') default 'no'," +
                    "  `reversible` enum('yes','no') default 'no'," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_reactions_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        if( tableName.equals("reactionComponents") )
        {
            return "CREATE TABLE `reactionComponents` (" +
                    "  `ID` varchar(100) NOT NULL default ''," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'relation-chemical'," +
                    getIDFieldFormat("reactionID") + "," +
                    getIDFieldFormat("entityID") + "," +
                    "  `specieID` varchar(200) default NULL," +
                    "  `participation` enum('direct','indirect','unknown') NOT NULL default 'direct'," +
                    "  `role` enum('reactant','product','modifier','other') NOT NULL default 'reactant'," +
                    "  `modifierAction` enum('catalyst','inhibitor','switch on','switch off') default NULL," +
                    "  `stoichiometry` varchar(100) default NULL," +
                    "  `denominator` int(11) default NULL," +
                    "  `comment` text," +
                    "  `attributes` text"+
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
