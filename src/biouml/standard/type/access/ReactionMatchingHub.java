package biouml.standard.type.access;

import java.util.Arrays;
import java.util.Properties;
import biouml.standard.type.Reaction;
import one.util.streamex.StreamEx;

public class ReactionMatchingHub extends StandardMatchingHub
{
    public ReactionMatchingHub(Properties properties)
    {
        super( properties );
    }

    @Override
    protected MatchingData createMatchingData()
    {
        MatchingData data = new MatchingData();
        StreamEx.of( hubCollection.getDataCollection().stream() ).select( Reaction.class ).mapToEntry( ref -> ref.getSpecieReferences() )
                .nonNullValues()
                .flatMapValues( Arrays::stream ).filterValues( spRef -> !spRef.isReactantOrProduct() )
                .forKeyValue( (de, spRef) -> data.accept( de.getName(), spRef.getSpecieName() ) );
        return data;
    }
}
