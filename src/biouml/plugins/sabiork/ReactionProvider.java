package biouml.plugins.sabiork;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eml.sdbv.sabioclient.GetActivatorsSpeciesIDs;
import org.eml.sdbv.sabioclient.GetAllReactionIDs;
import org.eml.sdbv.sabioclient.GetCatalystsSpeciesIDs;
import org.eml.sdbv.sabioclient.GetCofactorsSpeciesIDs;
import org.eml.sdbv.sabioclient.GetCompoundIDFromSpeciesID;
import org.eml.sdbv.sabioclient.GetCompoundName;
import org.eml.sdbv.sabioclient.GetEnzymeProtein;
import org.eml.sdbv.sabioclient.GetInhibitorsSpeciesIDs;
import org.eml.sdbv.sabioclient.GetKEGGReactionID;
import org.eml.sdbv.sabioclient.GetKinLawIDsNotNull;
import org.eml.sdbv.sabioclient.GetKineticLaw;
import org.eml.sdbv.sabioclient.GetParametersXML;
import org.eml.sdbv.sabioclient.GetProductsSpeciesIDs;
import org.eml.sdbv.sabioclient.GetReactionInstanceIDs;
import org.eml.sdbv.sabioclient.GetSubstratesSpeciesIDs;
import org.eml.sdbv.sabioclient.GetUnknownModifiersSpeciesIDs;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Module;
import biouml.model.dynamics.Variable;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class ReactionProvider extends ServiceProvider
{
    @Override
    public Reaction getDataElement(DataCollection<?> parent, String name) throws Exception
    {
        int reactionId = Integer.parseInt(name);
        Reaction reaction = new Reaction(parent, name);

        parseKineticLaw(reaction, reactionId);

        String proteinName = getSabiokrPort().getEnzymeProtein(new GetEnzymeProtein(reactionId)).get_return();
        if( proteinName != null && proteinName.length() > 0 )
        {
            Protein protein = (Protein)SabiorkUtility.getDataCollection(parent, SabiorkUtility.PROTEIN_DC).get(proteinName);
            if( protein != null )
            {
                String srName = protein.getName() + " as " + SpecieReference.MODIFIER;
                SpecieReference specieReference = new SpecieReference(reaction, srName, SpecieReference.MODIFIER);
                String specieLink = CollectionFactory.getRelativeName(protein, Module.getModule(parent));
                specieReference.setSpecie(specieLink);
                reaction.setSpecieReferences(new SpecieReference[] {specieReference});
            }
        }

        int[] substrates = getSabiokrPort().getSubstratesSpeciesIDs(new GetSubstratesSpeciesIDs(reactionId));
        parseSpecieReferences(reaction, substrates, SpecieReference.REACTANT, "substrate");

        int[] products = getSabiokrPort().getProductsSpeciesIDs(new GetProductsSpeciesIDs(reactionId));
        parseSpecieReferences(reaction, products, SpecieReference.PRODUCT, "product");

        int[] activators = getSabiokrPort().getActivatorsSpeciesIDs(new GetActivatorsSpeciesIDs(reactionId));
        parseSpecieReferences(reaction, activators, SpecieReference.MODIFIER, "activator");

        int[] inhibitors = getSabiokrPort().getInhibitorsSpeciesIDs(new GetInhibitorsSpeciesIDs(reactionId));
        parseSpecieReferences(reaction, inhibitors, SpecieReference.MODIFIER, "inhibitor");

        int[] catalysts = getSabiokrPort().getCatalystsSpeciesIDs(new GetCatalystsSpeciesIDs(reactionId));
        parseSpecieReferences(reaction, catalysts, SpecieReference.MODIFIER, "catalyst");

        int[] cofactors = getSabiokrPort().getCofactorsSpeciesIDs(new GetCofactorsSpeciesIDs(reactionId));
        parseSpecieReferences(reaction, cofactors, SpecieReference.MODIFIER, "cofactor");

        int[] unknownModifiers = getSabiokrPort().getUnknownModifiersSpeciesIDs(new GetUnknownModifiersSpeciesIDs(reactionId));
        parseSpecieReferences(reaction, unknownModifiers, SpecieReference.MODIFIER, "unknown modifier");

        if( reaction.getSpecieReferences().length == 0 )
        {
            log.warning("Reaction " + name + " has no specie references");
        }

        String keggId = getSabiokrPort().getKEGGReactionID(new GetKEGGReactionID(reactionId)).get_return();
        if( keggId != null )
        {
            keggId = keggId.trim();
            if( !keggId.isEmpty() )
            {
                DatabaseReference dr = new DatabaseReference("KEGG/reaction", keggId);
                reaction.setDatabaseReferences(new DatabaseReference[] {dr});
            }
        }

        return reaction;
    }

    @Override
    public List<String> getNameList() throws RemoteException
    {
        GetAllReactionIDs gr = new GetAllReactionIDs();
        int[] reactionUDs = getSabiokrPort().getAllReactionIDs(gr);

        List<String> result = new ArrayList<>();
        for( int id : reactionUDs )
        {
            result.add(Integer.toString(id));
        }
        return result;
    }

    protected void parseKineticLaw(Reaction reaction, int reactionId) throws Exception
    {
        int[] results = getSabiokrPort().getReactionInstanceIDs(new GetReactionInstanceIDs(reactionId));
        if( results.length > 0 )
        {
            int[] instances = null;
            for( int instanceId : results )
            {
                instances = getSabiokrPort().getKinLawIDsNotNull(new GetKinLawIDsNotNull(instanceId));
                if( instances.length > 0 )
                {
                    break;
                }
            }
            if( instances != null && instances.length > 0 )
            {
                int kinLawId = instances[0];

                String kineticLaw = getSabiokrPort().getKineticLaw(new GetKineticLaw(kinLawId)).get_return();
                if( kineticLaw == null )
                    return;

                String parameters = getSabiokrPort().getParametersXML(new GetParametersXML(kinLawId)).get_return();

                KineticLawProcessor klp = new KineticLawProcessor(kineticLaw);
                klp.parseParameters(parameters);

                KineticLaw kineticLawObject = new KineticLaw();
                kineticLawObject.setFormula(klp.getKineticLaw());
                reaction.setKineticLaw(kineticLawObject);

                reaction.getAttributes().add(new DynamicProperty("parameters", Variable[].class, klp.getParameters()));
                reaction.getAttributes().add(new DynamicProperty("variables", Variable[].class, klp.getVariables()));
            }
        }
    }
    protected void parseSpecieReferences(Reaction reaction, int[] ids, String role, String comment) throws Exception
    {
        if( ids == null || ids.length == 0 )
            return;

        List<SpecieReference> speciereferences = new ArrayList<>();
        for( SpecieReference sr : reaction.getSpecieReferences() )
        {
            speciereferences.add( sr );
        }

        for( int id : ids )
        {
            if( id == 0 )
                continue;


            int substanceId = getSabiokrPort().getCompoundIDFromSpeciesID(new GetCompoundIDFromSpeciesID(id)).get_return();
            if( substanceId == 0 )
                continue;

            String substanceName = getSabiokrPort().getCompoundName(new GetCompoundName(substanceId)).get_return();

            Substance substance = (Substance)SabiorkUtility.getDataCollection(reaction, SabiorkUtility.SUBSTANCE_DC).get(substanceName);

            if( substance != null )
            {
                String srName = substance.getName();
                if( !srName.contains(" as ") )
                {
                    srName += " as " + role;
                }
                SpecieReference specieReference = new SpecieReference(reaction, srName, role);
                String specieLink = CollectionFactory.getRelativeName(substance, Module.getModule(reaction));
                specieReference.setSpecie(specieLink);
                specieReference.setComment(comment);
                speciereferences.add(specieReference);
            }
        }

        reaction.setSpecieReferences(speciereferences.toArray(new SpecieReference[speciereferences.size()]));
    }
}
