package ru.biosoft.gui.setupwizard;

import org.eclipse.core.runtime.IConfigurationElement;
import ru.biosoft.util.ExtensionRegistrySupport;

/**
 * Registry for ru.biosoft.workbench.wizardPage extension point
 * 
 * @author tolstyh
 */
public class WizardPageRegistry extends ExtensionRegistrySupport<WizardPageInfo>
{
    private static final WizardPageRegistry instance = new WizardPageRegistry();

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String POSITION = "position";
    public static final String PAGE = "page";
    
    private WizardPageRegistry()
    {
        super("ru.biosoft.workbench.wizardPage", NAME);
    }

    public static WizardPageInfo[] getWizardPages()
    {
        return instance.stream().sorted().toArray( WizardPageInfo[]::new );
    }

    @Override
    protected WizardPageInfo loadElement(IConfigurationElement element, String name) throws Exception
    {
        String description = element.getAttribute(DESCRIPTION);
        int position = getIntAttribute(element, POSITION, 100);
        WizardPage page = getClassAttribute(element, PAGE, WizardPage.class).newInstance();
        return new WizardPageInfo(name, description, position, page);
    }
}
