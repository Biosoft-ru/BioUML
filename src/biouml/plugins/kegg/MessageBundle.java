package biouml.plugins.kegg;

import java.util.ListResourceBundle;

/**
 *
 * @pending enhance short descriptions.
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            // Molecule
            {"CN_MOLECULE",   "Molecule"},
            {"CD_MOLECULE",   "Molecule."},
        
            {"CN_PATHWAY_DIAGRAM", "KEGG pathway diagram"},
            {"CD_PATHWAY_DIAGRAM", "KEGG pathway diagram"},
        
            {"PN_TYPE"               , "Type"},
            {"PD_TYPE"               , "Type."},
            {"PN_IDENTIFIER"         , "Identifier"},
            {"PD_IDENTIFIER"         , "Identifier."},
            {"PN_DATE"               , "Date"},
            {"PD_DATE"               , "Date."},
            {"PN_MOLECULETYPE"       , "Molecule type"},
            {"PD_MOLECULETYPE"       , "Molecule type."},
            {"PN_UPSTREAMREACTIONS"  , "Upstream reactions"},
            {"PD_UPSTREAMREACTIONS"  , "Upstream reactions."},
            {"PN_DOWNSTREAMREACTIONS", "Downstream reactions"},
            {"PD_DOWNSTREAMREACTIONS", "Downstream reactions."},
            {"PN_CATALYZEDREACTIONS" , "Catalyzed reactions"},
            {"PD_CATALYZEDREACTIONS" , "Catalyzed reactions."},
            {"PN_INHIBITEDREACTIONS" , "Inhibited reactions"},
            {"PD_INHIBITEDREACTIONS" , "Inhibited reactions."},
        
            // Reaction
            {"CN_REACTION",   "Reaction"},
            {"CD_REACTION",   "Reaction."},
        
            {"PN_ACTIONTYPE"          , "Action type"},
            {"PD_ACTIONTYPE"          , "Action type."},
            {"PN_ACTIONMECHANISM"     , "Action mechanism"},
            {"PD_ACTIONMECHANISM"     , "Action mechanism."},
            {"PN_COMMENT"             , "Comment"},
            {"PD_COMMENT"             , "Comment."},
            {"PN_UPSTREAMMOLECULES"   , "Upstream molecules"},
            {"PD_UPSTREAMMOLECULES"   , "Upstream molecules."},
            {"PN_DOWNSTREAMMOLECULES" , "Downstream molecules"},
            {"PD_DOWNSTREAMMOLECULES" , "Downstream molecules."},
            {"PN_CATALYZEDMOLECULES"  , "Catalyzed molecules"},
            {"PD_CATALYZEDMOLECULES"  , "Catalyzed molecules."},
            {"PN_INHIBITEDMOLECULES"  , "Inhibited molecules"},
            {"PD_INHIBITEDMOLECULES"  , "Inhibited molecules."},
        
            {"IMPORTING", "Importing"},
            {"IMPORTING_ITEM", "Importing {0}..."},
            {"IMPORTING_ITEM_SUCCESS", "Importing {0} - success"},
            {"CREATING_ITEM", "Creating {0}..."},
            {"CREATING_ITEM_SUCCESS", "Creating {0} - success"},
            {"CREATING_DATABASE", "Creating {0} database..."},
            {"CREATING_DATABASE_SUCCESS", "Creating {0} database - success"},
            {"CREATING_DATABASE_TYPE", "Creating {0} database type..."},
            {"CREATING_DATABASE_TYPE_SUCCESS", "Creating {0} database type - success"},
        
            {"CONVERTING_DIAGRAMS", "Converting diagrams..."},
            {"CONVERTING_DIAGRAMS_SUCCESS", "Converting diagrams finished - success"},
            {"CONVERTING_DIAGRAMS_ERROR",  "Converting diagrams finished - {0} error(s)"},
        };
    }
}