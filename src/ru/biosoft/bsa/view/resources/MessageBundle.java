package ru.biosoft.bsa.view.resources;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import java.util.logging.Logger;



/**
 * <code>de.biobase.matchpro.resources.MessageBundle</code>
 * stores data for initialization of all MatchPro specific resources.
 *
 * <p>This resources include:
 * <ul>
 * <li> components (display name, short description)</li>
 * <li> properties (display name, short description)</li>
 * <li> actions (display name, short and long description, hot key, ican file name)</li>
 * <li> dialog, pane and tab tiltles</li>
 * <li> other specific resources</li>
 * </ul>
 *
 * The following conventions are used for key name generation:
 *
 * <p><i>for XXX component</i>
 * <ul>
 *   <li><code>CN_XXX</code> - component display name</li>
 *   <li><code>CD_XXX</code> - component short description</li>
 * </ul>
 *
 * <p><i>for YYY property of XXX component</i>
 * <ul>
 *   <li><code>PN_YYY</code> - property display name
 *   <li><code>PD_YYY</code> - property short description</li>
 * </ul>
 * <i>Note.</i> <code>PN_XXX_YYY</code> - can be used if several components
 * has different proprties with the same name
 *
 * <p><i>for ZZZ action</i>
 * <ul>
 *   <li><code>ACTION_ZZZ + ActionInitializerConstants.nameSuffix</code>
 *       - action display name</li>
 *   <li><code>ACTION_ZZZ + ActionInitializerConstants.shortDesriptionSuffix</code>
 *       - action short description (tooltip)</li>
 *   <li><code>ACTION_ZZZ + ActionInitializerConstants.longDesriptionSuffix</code>
 *       - action detailed description</li>
 *   <li><code>ACTION_ZZZ + ActionInitializerConstants.mnemonicSuffix</code>
 *       - action hot key</li>
 *   <li><code>ACTION_ZZZ + ActionInitializerConstants.smallIconSuffix</code>
 *       - icon file name</li>
 *   <li><code>ACTION_ZZZ + ActionInitializerConstants.actionCommandSuffix</code>
 *       action command string</li>
 * </ul>
 *
 * @todo describe conventions for dialog, pane, tab tiltles and other specific resources
 */
public class MessageBundle extends ListResourceBundle //implements ActionInitializerConstants
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        //globals
        { "ASSERT_INDEX_OUT_OF_RANGE",                  "Index out of range"                     },
        //SequenceView
        { "ASSERT_SEQUENCE_POSITION",                   "Position is outside of the sequence: position=" },
        //MapViewPane
        { "VIEW_CREATING_TIME",                         "Total view creating time : "            },
        //TableMapViewPane
        { "UNION_CREATING_ERROR",                       "Cannot create data collection union"    },
        { "DATA_COLLECTION_CREATING_ERROR",             "Data collection cannot be null."        },

        { "TOP_SITE_TABLE"         ,                    "Sites shown above the sequence"         },
        { "BOTTOM_SITE_TABLE"      ,                    "Sites shown below the sequence"         },
    };

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        } catch (Throwable t)
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}// end of class MessagesBundle
