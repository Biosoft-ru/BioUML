package biouml.plugins.simulation;

import com.developmentontheedge.beans.Option;

import ru.biosoft.util.bean.JSONBean;

/**
 *Base interface for simulator options<br>
 *All solvers who extends class Simulator should have parameters class which implements this interface
 */
public class Options extends Option implements JSONBean
{
}
