package ru.biosoft.bsastats;

import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.JSONBean;

/**
 * Aligns adapter to the sequence tail
 * 
 * Read:       RRRRRRRRRRRRRRRRRRRRRRR----------
 * Adapter:    ------------------AAAAAAAAAAAAAAAA
 * 
 * @author ivan
 *
 */
public abstract class AdapterAligner extends OptionEx implements JSONBean
{
    public abstract String getName();
    /**
     * @return position in sequence where adapter starts or sequence.length if no match found
     */
    public abstract AdapterMatch alignAdapter(byte[] adapter, byte[] sequence);
}
