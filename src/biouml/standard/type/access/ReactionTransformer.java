package biouml.standard.type.access;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.Entry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.util.TextUtil2;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

/**
 * @pending refine
 */
public class ReactionTransformer extends BeanInfoEntryTransformer<Reaction>
{
    protected BeanInfoEntryTransformer kineticLawTransformer;
    protected BeanInfoEntryTransformer<SpecieReference> specieReferenceTransformer;


    public ReactionTransformer()
    {}

    @Override
    public void init(DataCollection primaryCollection, DataCollection transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);
        initCommands(getOutputType());

        kineticLawTransformer = new BeanInfoEntryTransformer();
        kineticLawTransformer.initCommands(KineticLaw.class);

        specieReferenceTransformer = new BeanInfoEntryTransformer<>();
        specieReferenceTransformer.initCommands(SpecieReference.class);
    }

    protected String readSection(BufferedReader reader) throws Exception
    {
        StringBuffer buf =  new StringBuffer();

        String line;
        while( (line = reader.readLine()) != null )
        {
            if( line.startsWith("XX") )
                break;

            buf.append(line);
            buf.append(endl);
        }

        return buf.toString();
    }

    /** Read Reaction object from Entry object. */
    @Override
    synchronized public Reaction transformInput(Entry input) throws Exception
    {
        Reader entryReader = input.getReader();
        BufferedReader reader = null;
        if( entryReader instanceof BufferedReader )
            reader = (BufferedReader)entryReader;
        else
            reader = new BufferedReader(entryReader);

        String str = readSection(reader);
        Reaction reaction = new Reaction(getTransformedCollection(), input.getName());
        readObject(reaction, new StringReader(str));

        str = readSection(reader);
        KineticLaw kineticLaw = new KineticLaw(reaction);
        kineticLawTransformer.readObject(kineticLaw, new StringReader(str));
        reaction.setKineticLaw(kineticLaw);

        while( (str = readSection(reader)) != null )
        {
            String name = TextUtil2.getField(str, "NM");
            if( name == null )
                break;

            SpecieReference specieReference = new SpecieReference(reaction, name);
            int index = name.indexOf(": ");
            if( index > 0 )
                specieReference.setTitle(name.substring(index + 2));

            specieReferenceTransformer.readObject(specieReference, new StringReader(str));
            reaction.put(specieReference);
        }

        return reaction;
    }

    /** Write Reaction object to Entry object. */
    @Override
    public Entry transformOutput(Reaction input) throws Exception
    {
        processedObject = input;
        String startTag = getStartTag();
        StringWriter data = new StringWriter();
        TagCommand command = commands.get(startTag);
        if (command == null)
            data.write(startTag + "  " + input.getName() + endl);
        else
            data.write(command.getTaggedValue(input.getName()));

        writeObject(input, data);
        data.write("XX" + endl);

        Reaction reaction = input;
        kineticLawTransformer.writeObject(reaction.getKineticLaw(), data);
        data.write("XX" + endl);

        for(SpecieReference specieReference : reaction)
        {
            data.write("NM  " + specieReference.getName() + endl);
            specieReferenceTransformer.writeObject(specieReference, data);
            data.write("XX" + endl);
        }

        data.write(getEndTag() + endl);
        return new Entry(input.getOrigin(), input.getName(), data.toString());
    }

}
