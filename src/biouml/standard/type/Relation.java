package biouml.standard.type;

/**
 * Intermediary class to generalize what is common for all relations/edges
 * as well as to define common constants.
 */
public interface Relation extends Base
{
    /**
     * Possible particiations
     */
    public static final String PARTICIPATION_DIRECT     = "direct";
    public static final String PARTICIPATION_INDIRECT   = "indirect";
    public static final String PARTICIPATION_UNKNOWN    = "unknown";
    public static final String[] participationTypes = {Relation.PARTICIPATION_DIRECT, Relation.PARTICIPATION_INDIRECT, Relation.PARTICIPATION_UNKNOWN};
    
    public String getParticipation();
}

