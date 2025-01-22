import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.standard.StandardElasticContact;

public class Contact extends StandardElasticContact
{
    public void execute(Cell pMe, Phenotype phenoMe, Cell pOther, Phenotype phenoOther, double dt)
    {
        // spring-like adhesion 
        super.execute( pMe, phenoMe, pOther, phenoOther, dt );

        // juxtacrine 
        if( pMe.state.numberAttachedCells() > 0 )
        {
            double head_me = pMe.customData.get( "head" );
            double head_other = pOther.customData.get( "head" );

            // avoid double-counting transfer: 
            // Only do the high . low transfers
            // One cell of each pair will satisfy this. 

            // make the transfer 
            if( head_me > head_other )
            {
                double amount_to_transfer = dt * pMe.customData.get( "transfer_rate" ) * ( head_me - head_other );
                //                    double head = ;
                pMe.customData.set( "head", pMe.customData.get( "head" ) - amount_to_transfer );
                pOther.customData.set( "head", pOther.customData.get( "head" ) + amount_to_transfer );
                //                    pMe.customData["head"] -= amount_to_transfer;
                //			#pragma omp critical
                //                    {
                //                        pOther.customData["head"] += amount_to_transfer;
                //                    }
            }
        }

    }
}