package biouml.plugins.expression;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.JSONBean;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class ExpressionFilterProperties extends Option implements JSONBean
{
    private DataElementPath table;
    private boolean useOutsideFill = true, useInsideFill = false, usePval = false, useFlux = false;
    private OutsideFillProperties outsideOptions;
    private InsideFillProperties insideOptions;
    private PvalProperties pvalOptions;
    private FluxProperties fluxOptions;
    private boolean loading = true;

    public ExpressionFilterProperties()
    {
        setOutsideOptions( new OutsideFillProperties() );
        setInsideOptions( new InsideFillProperties() );
        setPvalOptions( new PvalProperties() );
        setFluxOptions( new FluxProperties() );
    }

    @PropertyName("Table")
    @PropertyDescription("Table to use expression values from")
    public DataElementPath getTable()
    {
        return table;
    }
    public void setTable(DataElementPath table)
    {
        if(table != null && this.table != null && table.equals(this.table)) return;
        Object oldValue = this.table;
        this.table = table;
        firePropertyChange( "table", oldValue, table );
        if( loading )
            return;
        if(table != null)
        {
            DataElement de = table.optDataElement();
            if(de instanceof TableDataCollection)
            {
                ColumnModel columnModel = ((TableDataCollection)de).getColumnModel();
                String colName = "LogFoldChange";
                TableColumn logFCcolumn = null;
                try
                {
                    logFCcolumn = columnModel.getColumn(colName);
                }
                catch( Exception e )
                {
                }
                getOutsideOptions().setColumns(new String[] {});
                getInsideOptions().setColumn(ColumnNameSelector.NONE_COLUMN);
                if(logFCcolumn != null && logFCcolumn.getType().isNumeric())
                {
                    if(useOutsideFill)
                    {
                        getOutsideOptions().setColumns(new String[] {colName});
                    } else if(useInsideFill)
                    {
                        getInsideOptions().setColumn(colName);
                    }
                } else
                {
                    if(useOutsideFill)
                    {
                        String[] columns = columnModel.stream().filter( c -> c.getType().isNumeric() ).map( TableColumn::getName )
                                .toArray( String[]::new );
                        getOutsideOptions().setColumns( columns );
                    } else if(useInsideFill)
                    {
                        columnModel.stream().findAny( c -> c.getType().isNumeric() )
                                .ifPresent( c -> getInsideOptions().setColumn( c.getName() ) );
                    }
                }
            }
        }
    }
    
    @PropertyName("Outside filling options")
    @PropertyDescription("Outside filling options")
    public OutsideFillProperties getOutsideOptions()
    {
        return outsideOptions;
    }
    public void setOutsideOptions(OutsideFillProperties outsideOptions)
    {
        Object oldValue = this.outsideOptions;
        this.outsideOptions = outsideOptions;
        this.outsideOptions.setParent(this);
        firePropertyChange("outsideOptions", oldValue, outsideOptions);
    }
    
    public boolean isOutsideOptionsHidden()
    {
        return !useOutsideFill;
    }
    
    @PropertyName("Inside filling options")
    @PropertyDescription("Inside filling options")
    public InsideFillProperties getInsideOptions()
    {
        return insideOptions;
    }
    public void setInsideOptions(InsideFillProperties insideOptions)
    {
        Object oldValue = this.insideOptions;
        this.insideOptions = insideOptions;
        this.insideOptions.setParent(this);
        firePropertyChange("insideOptions", oldValue, insideOptions);
    }
    
    public boolean isInsideOptionsHidden()
    {
        return !useInsideFill;
    }
    
    @PropertyName("P-value stars options")
    @PropertyDescription("*** options")
    public PvalProperties getPvalOptions()
    {
        return pvalOptions;
    }
    public void setPvalOptions(PvalProperties pvalOptions)
    {
        Object oldValue = this.pvalOptions;
        this.pvalOptions = pvalOptions;
        this.pvalOptions.setParent(this);
        firePropertyChange("pvalOptions", oldValue, pvalOptions);
    }
    
    public boolean isPvalOptionsHidden()
    {
        return !usePval;
    }

    @PropertyName("Flux options")
    @PropertyDescription("Flux options")
    public FluxProperties getFluxOptions()
    {
        return fluxOptions;
    }
    public void setFluxOptions(FluxProperties fluxOptions)
    {
        Object oldValue = this.fluxOptions;
        this.fluxOptions = fluxOptions;
        this.fluxOptions.setParent(this);
        firePropertyChange("fluxOptions", oldValue, fluxOptions);
    }
    
    public boolean isFluxOptionsHidden()
    {
        return !useFlux;
    }
    
    @PropertyName("Use outside fill")
    @PropertyDescription("When checked, outside fill will be used")
    public boolean isUseOutsideFill()
    {
        return useOutsideFill;
    }

    public void setUseOutsideFill(boolean useOutsideFill)
    {
        Object oldValue = this.useOutsideFill;
        this.useOutsideFill = useOutsideFill;
        firePropertyChange( "*", oldValue, useOutsideFill );
    }

    @PropertyName("Use inside fill")
    @PropertyDescription("When checked, inside fill will be used")
    public boolean isUseInsideFill()
    {
        return useInsideFill;
    }

    public void setUseInsideFill(boolean useInsideFill)
    {
        Object oldValue = this.useInsideFill;
        this.useInsideFill = useInsideFill;
        firePropertyChange( "*", oldValue, useInsideFill );
    }

    @PropertyName("Use p-value stars")
    @PropertyDescription("When checked, a number of stars will be added to matched elements depending on p-value")
    public boolean isUsePval()
    {
        return usePval;
    }

    public void setUsePval(boolean usePval)
    {
        Object oldValue = this.usePval;
        this.usePval = usePval;
        firePropertyChange( "*", oldValue, usePval );
    }

    @PropertyName("Use flux edges")
    @PropertyDescription("When checked, a reaction edges thickness will be changed based on reaction speed data")
    public boolean isUseFlux()
    {
        return useFlux;
    }

    public void setUseFlux(boolean useFlux)
    {
        Object oldValue = this.useFlux;
        this.useFlux = useFlux;
        firePropertyChange( "*", oldValue, this.useFlux );
    }

    public void setLoading(boolean loading)
    {
        this.loading = loading;
        outsideOptions.setLoading( loading );
        insideOptions.setLoading( loading );
        fluxOptions.setLoading( loading );
    }
}
