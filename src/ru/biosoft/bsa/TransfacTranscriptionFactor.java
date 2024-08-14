package ru.biosoft.bsa;

import java.util.ArrayList;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.bsa.classification.ClassificationUnit;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Publication;

/**
 *
 * @pending possibly all properties should be read only and
 * @pending display name - is it really necessary?
 */
@PropertyName("factor")
public class TransfacTranscriptionFactor extends TranscriptionFactor
{
    private DataElementPath classificationRoot;
    
    public TransfacTranscriptionFactor(String name, DataCollection origin, String displayName, DataElementPath classificationRoot,
            String taxonPath, String DNABindingDomainPath, String generalClassPath, String positiveTissueSpecificity,
            String negativeTissueSpecificity)
    {
        super(name, origin, displayName, ReferenceTypeRegistry.getReferenceType(TransfacProteinType.class), null);
        this.classificationRoot         = classificationRoot;
        this.taxonPath                  = taxonPath;
        this.DNABindingDomainPath       = DNABindingDomainPath;
        this.generalClassPath           = generalClassPath;
        this.positiveTissueSpecificity  = positiveTissueSpecificity;
        this.negativeTissueSpecificity  = negativeTissueSpecificity;
    }
    
    @Override
    public String getSpeciesName()
    {
        try
        {
            ClassificationUnit taxon = getTaxon();
            if ( taxon == null )
                return null;
            if ( taxon.getDescription() != null )
                return taxon.getDescription();
            else
                return taxon.getName();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public TransfacTranscriptionFactor(
        String name,
        DataCollection origin,
        String displayName,
        String taxonCompleteName,
        String DNABindingDomainCompleteName,
        String generalClassCompleteName,
        String positiveTissueSpecificity,
        String negativeTissueSpecificity
        )
    {
        this(name, origin, displayName, DataElementPath.EMPTY_PATH, "/" + taxonCompleteName, "/" + DNABindingDomainCompleteName, "/"
                + generalClassCompleteName, positiveTissueSpecificity, negativeTissueSpecificity);
    }


    @Override
    public int hashCode()
    {
        return DataElementPath.create(this).hashCode();
    }

    //----- taxon ----------------------------------------------------/
    private String taxonPath;

    public DataElementPath getTaxonPath()
    {
        return taxonPath == null ? null : classificationRoot.getRelativePath(taxonPath);
    }

    private ClassificationUnit taxon;
    public ClassificationUnit getTaxon()
    {
        if (taxon == null && taxonPath != null)
            taxon = classificationRoot.getRelativePath(taxonPath).getDataElement(ClassificationUnit.class);

        return taxon;
    }

    //----- Classification -------------------------------------------/
    private String generalClassPath;
    public DataElementPath getGeneralClassPath()
    {
        return generalClassPath == null ? null : classificationRoot.getRelativePath(generalClassPath);
    }

    private ClassificationUnit generalClass;
    public ClassificationUnit getGeneralClass()
    {
        if (generalClass == null && generalClassPath != null)
            generalClass = classificationRoot.getRelativePath(generalClassPath).getDataElement(ClassificationUnit.class);
        return generalClass;
    }

    //----- DNA binding domain ---------------------------------------/
    private String DNABindingDomainPath;
    public DataElementPath getDNABindingDomainPath()
    {
        return DNABindingDomainPath == null ? null : classificationRoot.getRelativePath(DNABindingDomainPath);
    }

    private ClassificationUnit DNABindingDomain;
    public ClassificationUnit getDNABindingDomain()
    {
        if (DNABindingDomain == null && DNABindingDomainPath != null)
            DNABindingDomain = classificationRoot.getRelativePath(DNABindingDomainPath).getDataElement(ClassificationUnit.class);

        return DNABindingDomain;
    }
    private String synonyms;
    public String getSynonyms()
    {
        return synonyms;
    }
    public void setSynonyms(String synonyms)
    {
        this.synonyms=synonyms;
    }
    private ArrayList<Publication> publications = new ArrayList<>();
    public Publication[] getPublications()
    {
        return publications.toArray(new Publication[publications.size()]);
    }
    public void addPublication(Publication p)
    {
        publications.add(p);
    }
    public void setPublications(ArrayList<Publication> publications)
    {
        this.publications=publications;
    }
    //----- Positive & negative tissue specifity --------------------------------/
    
    private String negativeTissueSpecificity;
    public String getNegativeTissueSpecificity()
    {
        return negativeTissueSpecificity;
    }

    private String positiveTissueSpecificity;
    public String getPositiveTissueSpecificity()
    {
        return positiveTissueSpecificity;
    }

    public DataElementPath getClassificationRoot()
    {
        return classificationRoot;
    }
    
    private DatabaseReference[] references;
    public DatabaseReference[] getDatabaseReferences()
    {
        return references;
    }
    
    public void setDatabaseReferences(DatabaseReference[] references)
    {
        this.references = references;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}