package biouml.plugins.kegg;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.Repository;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.plugins.kegg.type.Glycan;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Relation;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

/**
 * Definition of KEGG Pathways module.
 *
 * The module defines:
 * <ul>
 *   <li>KEGG pathways data types</li>
 *   <li>mapping of KEGG data types into database.
 *   <li>DiagramViewBuilder and SemanticController specific for KEGG pathways database.</li>
 * </ul>
 *
 * @todo implement
 */
public class KeggPathwayModuleType extends DataElementSupport implements ModuleType
{
    public static final String VERSION = "1.0";

    public KeggPathwayModuleType()
    {
        super("KEGG pathways", null);
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[] {KeggPathwayDiagramType.class};
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        return null;
    }

    /** @pending optimise (for example throw map(class, category)) */
    @Override
    public String getCategory(Class<? extends DataElement> c)
    {
        if( Substance.class.isAssignableFrom(c) )
            return Module.DATA + "/compound";
        if( Protein.class.isAssignableFrom(c) )
            return Module.DATA + "/enzyme";
        if( Reaction.class.isAssignableFrom(c) )
            return Module.DATA + "/reaction";
        if( Glycan.class.isAssignableFrom(c) )
            return Module.DATA + "/glycan";
        if( Relation.class.isAssignableFrom(c) )
            return Module.DATA + "/relation";
        if( Stub.Note.class.isAssignableFrom(c) )
            return null;

        throw new IllegalArgumentException("Unknown kernel class in KEGG categoriser: " + c.getName());
    }

    @Override
    public boolean canCreateEmptyModule()
    {
        return false;
    }

    /** @todo implement */
    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        throw new UnsupportedOperationException("Creation of KEGG module is not implemented.");
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    public boolean isCategorySupported()
    {
        return true;
    }
}
