package biouml.workbench.resources;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.util.ListResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.KeyStroke;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.print.PrintAction;
import com.developmentontheedge.print.PrintPreviewAction;
import com.developmentontheedge.print.PrintSetupAction;

import biouml.workbench.BioUMLApplication;
import biouml.workbench.ImportElementAction;
import biouml.workbench.ImportImageDataElementAction;
import biouml.workbench.NewDataElementAction;
import biouml.workbench.PreferencesAction;
import biouml.workbench.RemoveDataElementAction;
import biouml.workbench.diagram.ClipboardPane;
import biouml.workbench.diagram.ConvertDiagramAction;
import biouml.workbench.diagram.FitToScreenAction;
import biouml.workbench.diagram.NewDiagramAction;
import biouml.workbench.diagram.SaveAsDocumentAction;
import biouml.workbench.diagram.ViewOptionsAction;
import biouml.workbench.module.ExportModuleAction;
import biouml.workbench.module.ModuleSetupAction;
import biouml.workbench.module.NewModuleAction;
import biouml.workbench.module.RemoveModuleAction;
import biouml.workbench.module.xml.EditModuleAction;
import biouml.workbench.module.xml.NewCompositeModuleAction;
import ru.biosoft.access.security.LoginAction;
import ru.biosoft.access.security.LogoutAction;
import ru.biosoft.access.security.RegisterAction;
import ru.biosoft.gui.CloseAllDocumentAction;
import ru.biosoft.gui.CloseDocumentAction;
import ru.biosoft.gui.ExportDocumentAction;
import ru.biosoft.gui.OpenPathAction;
import ru.biosoft.gui.RedoAction;
import ru.biosoft.gui.SaveDocumentAction;
import ru.biosoft.gui.UndoAction;
import ru.biosoft.gui.ZoomInAction;
import ru.biosoft.gui.ZoomOutAction;
import ru.biosoft.gui.setupwizard.OpenSetupWizardAction;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.workbench.AboutAction;
import ru.biosoft.workbench.HelpAction;

/**
 * Stores data for initialization of BioUMLEditor constant and resources.
 */
public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents = {
            ////////////////////////////////////////////////////////////////////////
            // beanInfo messages
            //

            // Preferences info
            {"PREFERENCES_DIALOG_TITLE", "$ApplicationName$"+" workbench preferences"},
            
            // Setup wizard info
            {"SETUP_WIZARD_TITLE", "$ApplicationName$"+" setup wizard"},

            {"PN_REPOSITORY_PATH", "Repository path"}, // pending - remove
            {"PD_REPOSITORY_PATH", "Repository path."}, // pending - remove
            // pending - remove
            {"PN_CHECKDATABASESONSTARTUP", "Check new databases on startup"}, // pending - remove
            {"PD_CHECKDATABASESONSTARTUP", "Check for not active databases on startup."}, // pending - remove

            // Clipboard, DataElementInfo
            {"PN_DIAGRAM_ELEMENT_INFO_KERNEL_NAME", "Name"},
            {"PD_DIAGRAM_ELEMENT_INFO_KERNEL_NAME", "Name (ID) of diagram element kernel."},

            {"PN_DIAGRAM_ELEMENT_INFO_KERNEL_TITLE", "Title"},
            {"PD_DIAGRAM_ELEMENT_INFO_KERNEL_TITLE", "Title of diagram element kernel."},

            {"PN_DIAGRAM_ELEMENT_INFO_KERNEL_TYPE", "Type"},
            {"PD_DIAGRAM_ELEMENT_INFO_KERNEL_TYPE", "Type of diagram element kernel."},

            {"PN_DIAGRAM_ELEMENT_INFO_DATABASE_NAME", "Database"},
            {"PD_DIAGRAM_ELEMENT_INFO_DATABASE_NAME",
                    "Database for data element kernel." + "<br>Data exchange between different database is not supported yet."},

            ////////////////////////////////////////////////////////////////////////
            // Actions
            //

            // New Diagram action
            {NewDiagramAction.KEY + Action.SMALL_ICON, "new.gif"},
            {NewDiagramAction.KEY + Action.NAME, "New diagram"},
            {NewDiagramAction.KEY + Action.SHORT_DESCRIPTION, "New diagram"},
            {NewDiagramAction.KEY + Action.LONG_DESCRIPTION, "New diagram"},
            {NewDiagramAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_N},
            {NewDiagramAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK)},
            {NewDiagramAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-new-diagram"},

            // New Module action
            {NewModuleAction.KEY + Action.SMALL_ICON, "newModule.gif"},
            {NewModuleAction.KEY + Action.NAME, "New simple database"},
            {NewModuleAction.KEY + Action.SHORT_DESCRIPTION, "New simple database"},
            {NewModuleAction.KEY + Action.LONG_DESCRIPTION, "New simple database"},
            {NewModuleAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_M},
            {NewModuleAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.CTRL_MASK)},
            {NewModuleAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-new-database"},

            // New composite database action
            {NewCompositeModuleAction.KEY + Action.SMALL_ICON, "newModule.gif"},
            {NewCompositeModuleAction.KEY + Action.NAME, "New composite database"},
            {NewCompositeModuleAction.KEY + Action.SHORT_DESCRIPTION, "New composite database"},
            {NewCompositeModuleAction.KEY + Action.LONG_DESCRIPTION, "New composite database"},
            {NewCompositeModuleAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-new-composite-database"},

            // Login to BioUML action
            {LoginAction.KEY + Action.SMALL_ICON, "login.gif"},
            {LoginAction.KEY + Action.NAME, "Login..."},
            {LoginAction.KEY + Action.SHORT_DESCRIPTION, "Login to "+"$ApplicationName$"},
            {LoginAction.KEY + Action.LONG_DESCRIPTION, "Login to "+"$ApplicationName$"},
            {LoginAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-login"},

            // Register new user action
            {RegisterAction.KEY + Action.SMALL_ICON, "register.gif"},
            {RegisterAction.KEY + Action.NAME, "Register..."},
            {RegisterAction.KEY + Action.SHORT_DESCRIPTION, "Register new user"},
            {RegisterAction.KEY + Action.LONG_DESCRIPTION, "Register new user"},
            {RegisterAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-register"},

            // Logout action
            {LogoutAction.KEY + Action.SMALL_ICON, "logout.gif"},
            {LogoutAction.KEY + Action.NAME, "Logout"},
            {LogoutAction.KEY + Action.SHORT_DESCRIPTION, "Logout"},
            {LogoutAction.KEY + Action.LONG_DESCRIPTION, "Logout"},
            {LogoutAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-logout"},

            // Open setup wizard action
            {OpenSetupWizardAction.KEY + Action.SMALL_ICON, "logout.gif"},
            {OpenSetupWizardAction.KEY + Action.NAME, "Setup wizard..."},
            {OpenSetupWizardAction.KEY + Action.SHORT_DESCRIPTION, "Setup wizard"},
            {OpenSetupWizardAction.KEY + Action.LONG_DESCRIPTION, "Open setup wizard"},
            {OpenSetupWizardAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-setup"},

            // New Data Element action
            {NewDataElementAction.KEY + Action.SMALL_ICON, "new.gif"},
            {NewDataElementAction.KEY + Action.NAME, "New data element"},
            {NewDataElementAction.KEY + Action.SHORT_DESCRIPTION, "Creates new data element for selected data collection"},
            {NewDataElementAction.KEY + Action.LONG_DESCRIPTION, "New data element"},
            //        { NewDataElementAction.KEY      + Action.MNEMONIC_KEY         , "N"},
            //        { NewDataElementAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK) },
            {NewDataElementAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-new-data-element"},

            // Remove Data Element action
            {RemoveDataElementAction.KEY + Action.SMALL_ICON, "remove.gif"},
            {RemoveDataElementAction.KEY + Action.NAME, "Remove"},
            {RemoveDataElementAction.KEY + Action.SHORT_DESCRIPTION, "Removes specified data element"},
            {RemoveDataElementAction.KEY + Action.LONG_DESCRIPTION, "Remove"},
            {RemoveDataElementAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_DELETE},
            {RemoveDataElementAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Event.CTRL_MASK)},
            {RemoveDataElementAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-rm-de"},

            // Import Image Data Element action
            {ImportImageDataElementAction.KEY + Action.SMALL_ICON, "importDiagram.gif"},
            {ImportImageDataElementAction.KEY + Action.NAME, "Import"},
            {ImportImageDataElementAction.KEY + Action.SHORT_DESCRIPTION, "Imports specified images to the collection"},
            {ImportImageDataElementAction.KEY + Action.LONG_DESCRIPTION, "Import images"},
            {ImportImageDataElementAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_I},
            {ImportImageDataElementAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK)},
            {ImportImageDataElementAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-import-image"},


            // GridOptions action
            {ViewOptionsAction.KEY + Action.SMALL_ICON, "viewOptions.gif"},
            {ViewOptionsAction.KEY + Action.NAME, "Diagram view options..."},
            {ViewOptionsAction.KEY + Action.SHORT_DESCRIPTION, "Open diagram view options dialog."},
            {ViewOptionsAction.KEY + Action.LONG_DESCRIPTION, "Open diagram view options dialog."},
            {ViewOptionsAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-viewoptions-diagram"},

            // ConvertDiagram action
            {ConvertDiagramAction.KEY + Action.SMALL_ICON, "convert.gif"},
            {ConvertDiagramAction.KEY + Action.NAME, "Convert diagram"},
            {ConvertDiagramAction.KEY + Action.SHORT_DESCRIPTION, "Converts diagram from one type to another."},
            {ConvertDiagramAction.KEY + Action.LONG_DESCRIPTION, "Converts diagram from one type to another."},
            {ConvertDiagramAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_T},
            {ConvertDiagramAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK)},
            {ConvertDiagramAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-convert-diagram"},

            // ImportElement action
            {ImportElementAction.KEY + Action.SMALL_ICON, "importDiagram.gif"},
            {ImportElementAction.KEY + Action.NAME, "Import element"},
            {ImportElementAction.KEY + Action.SHORT_DESCRIPTION, "Imports element from external file."},
            {ImportElementAction.KEY + Action.LONG_DESCRIPTION, "Imports element from external file."},
            {ImportElementAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_I},
            {ImportElementAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK)},
            {ImportElementAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-import-element"},

             // Open action
            {OpenPathAction.KEY + Action.SMALL_ICON, "open.gif"}, {OpenPathAction.KEY + Action.NAME, "Open path"},
            {OpenPathAction.KEY + Action.SHORT_DESCRIPTION, "Open path"}, {OpenPathAction.KEY + Action.LONG_DESCRIPTION, "Open path"},
            {OpenPathAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_O},
            {OpenPathAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_O, Event.CTRL_MASK )},
            {OpenPathAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-open-document"},
            
            // Save action
            {SaveDocumentAction.KEY + Action.SMALL_ICON, "save.gif"},
            {SaveDocumentAction.KEY + Action.NAME, "Save document"},
            {SaveDocumentAction.KEY + Action.SHORT_DESCRIPTION, "Save document"},
            {SaveDocumentAction.KEY + Action.LONG_DESCRIPTION, "Save document"},
            {SaveDocumentAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_S},
            {SaveDocumentAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK)},
            {SaveDocumentAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-save-document"},

            // Save As action
            {SaveAsDocumentAction.KEY + Action.SMALL_ICON, "saveAs.gif"},
            {SaveAsDocumentAction.KEY + Action.NAME, "Save document as..."},
            {SaveAsDocumentAction.KEY + Action.SHORT_DESCRIPTION, "Save document as"},
            {SaveAsDocumentAction.KEY + Action.LONG_DESCRIPTION, "Save document as"},
            //        { SaveAsDocumentAction.KEY    + Action.MNEMONIC_KEY         , "V"},
            //        { SaveAsDocumentAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK) },
            {SaveAsDocumentAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-save-document-as"},

            // ExportDocument action
            {ExportDocumentAction.KEY + Action.SMALL_ICON, "exportDiagram.gif"},
            {ExportDocumentAction.KEY + Action.NAME, "Export document"},
            {ExportDocumentAction.KEY + Action.SHORT_DESCRIPTION, "Exports document in specified format."},
            {ExportDocumentAction.KEY + Action.LONG_DESCRIPTION, "Exports document in specified format."},
            {ExportDocumentAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_E},
            {ExportDocumentAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK)},
            {ExportDocumentAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-export-document"},

            // Close action
            {CloseDocumentAction.KEY + Action.SMALL_ICON, "close.gif"},
            {CloseDocumentAction.KEY + Action.NAME, "Close document"},
            {CloseDocumentAction.KEY + Action.SHORT_DESCRIPTION, "Close document"},
            {CloseDocumentAction.KEY + Action.LONG_DESCRIPTION, "Close document"},
            {CloseDocumentAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_C},
            {CloseDocumentAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK)},
            {CloseDocumentAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-close-document"},

            // Close all action
            {CloseAllDocumentAction.KEY + Action.SMALL_ICON, "closeall.gif"},
            {CloseAllDocumentAction.KEY + Action.NAME, "Close all documents"},
            {CloseAllDocumentAction.KEY + Action.SHORT_DESCRIPTION, "Close all documents"},
            {CloseAllDocumentAction.KEY + Action.LONG_DESCRIPTION, "Close all documents"},
            {CloseAllDocumentAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_C},
            {CloseAllDocumentAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK | Event.SHIFT_MASK)},
            {CloseAllDocumentAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-close-all-document"},

            // Select action
            {"Select" + Action.SMALL_ICON, "arrow.gif"},
            {"Select" + Action.NAME, "Select"},
            {"Select" + Action.SHORT_DESCRIPTION, "Select an element of diagram"},
            {"Select" + Action.LONG_DESCRIPTION, "Select an element of diagram"},
            //        { "Select"      + Action.MNEMONIC_KEY         , "S"},
            //        { "Select"      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK | Event.ALT_MASK) },
            {"Select" + Action.ACTION_COMMAND_KEY, "cmd-select"},

            // Zoom In action
            {ZoomInAction.KEY + Action.SMALL_ICON, "zoomin.gif"},
            {ZoomInAction.KEY + Action.NAME, "Zoom in"},
            {ZoomInAction.KEY + Action.SHORT_DESCRIPTION, "Zoom in"},
            {ZoomInAction.KEY + Action.LONG_DESCRIPTION, "Zoom in"},
            {ZoomInAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_PERIOD},
            {ZoomInAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, Event.CTRL_MASK)},
            {ZoomInAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-zoom-in"},

            // Zoom Out action
            {ZoomOutAction.KEY + Action.SMALL_ICON, "zoomout.gif"},
            {ZoomOutAction.KEY + Action.NAME, "Zoom out"},
            {ZoomOutAction.KEY + Action.SHORT_DESCRIPTION, "Zoom out"},
            {ZoomOutAction.KEY + Action.LONG_DESCRIPTION, "Zoom out"},
            {ZoomOutAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_COMMA},
            {ZoomOutAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Event.CTRL_MASK)},
            {ZoomOutAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-zoom-out"},
            
            {FitToScreenAction.KEY + Action.SMALL_ICON, "fittoscreen.png"}, 
            {FitToScreenAction.KEY + Action.NAME, "Fit to screen"},
            {FitToScreenAction.KEY + Action.SHORT_DESCRIPTION, "Scale diagram to fit screen size"},
            {FitToScreenAction.KEY + Action.LONG_DESCRIPTION, "Scale diagram to fit screen size"},

            // Undo action
            {UndoAction.KEY + Action.SMALL_ICON, "undo.gif"},
            {UndoAction.KEY + Action.NAME, "Undo"},
            {UndoAction.KEY + Action.SHORT_DESCRIPTION, "Undo"},
            {UndoAction.KEY + Action.LONG_DESCRIPTION, "Undo"},
            {UndoAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_Z},
            {UndoAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK)},
            {UndoAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-undo"},

            // Redo action
            {RedoAction.KEY + Action.SMALL_ICON, "redo.gif"},
            {RedoAction.KEY + Action.NAME, "Redo"},
            {RedoAction.KEY + Action.SHORT_DESCRIPTION, "Redo"},
            {RedoAction.KEY + Action.LONG_DESCRIPTION, "Redo"},
            {RedoAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_Y},
            {RedoAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK)},
            {RedoAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-redo"},
            
            // Print action
            {PrintAction.KEY + Action.SMALL_ICON, "print.gif"},
            {PrintAction.KEY + Action.NAME, "Print"},
            {PrintAction.KEY + Action.SHORT_DESCRIPTION, "Print document"},
            {PrintAction.KEY + Action.LONG_DESCRIPTION, "Print document"},
            //        { PrintAction.KEY      + Action.MNEMONIC_KEY         , "P"},
            //        { PrintAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK) },
            {PrintAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-print"},

            // Print Preview action
            {PrintPreviewAction.KEY + Action.SMALL_ICON, "printPreview.gif"},
            {PrintPreviewAction.KEY + Action.NAME, "Print preview"},
            {PrintPreviewAction.KEY + Action.SHORT_DESCRIPTION, "Print preview"},
            {PrintPreviewAction.KEY + Action.LONG_DESCRIPTION, "Print preview"},
            //        { PrintPreviewAction.KEY      + Action.MNEMONIC_KEY         , "V"},
            //        { PrintPreviewAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK) },
            {PrintPreviewAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-print-preview"},

            // Print Setup action
            {PrintSetupAction.KEY + Action.SMALL_ICON, "printSetup.gif"},
            {PrintSetupAction.KEY + Action.NAME, "Print setup"},
            {PrintSetupAction.KEY + Action.SHORT_DESCRIPTION, "Print setup"},
            {PrintSetupAction.KEY + Action.LONG_DESCRIPTION, "Print setup"},
            //        { PrintSetupAction.KEY      + Action.MNEMONIC_KEY         , "I"},
            //        { PrintSetupAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK | Event.ALT_MASK) },
            {PrintSetupAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-print-setup"},

            // Toggle Repository action
            {BioUMLApplication.REPOSITORY_PANE_NAME + Action.SMALL_ICON, "toggleRepository.gif"},
            {BioUMLApplication.REPOSITORY_PANE_NAME + Action.NAME, "Repository"},
            {BioUMLApplication.REPOSITORY_PANE_NAME + Action.SHORT_DESCRIPTION, "Repository panel"},
            {BioUMLApplication.REPOSITORY_PANE_NAME + Action.LONG_DESCRIPTION, "Toggle Repository panel"},
            //        { BioUMLApplication.REPOSITORY_PANE_NAME      + Action.MNEMONIC_KEY         , "R"},
            //        { BioUMLApplication.REPOSITORY_PANE_NAME      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK | Event.ALT_MASK) },
            {BioUMLApplication.REPOSITORY_PANE_NAME + Action.ACTION_COMMAND_KEY, "cmd-toggle-repository"},

            // Toggle Diagram action
            {BioUMLApplication.DOCUMENT_PANE_NAME + Action.SMALL_ICON, "toggleDiagram.gif"},
            {BioUMLApplication.DOCUMENT_PANE_NAME + Action.NAME, "Document"},
            {BioUMLApplication.DOCUMENT_PANE_NAME + Action.SHORT_DESCRIPTION, "Document panel"},
            {BioUMLApplication.DOCUMENT_PANE_NAME + Action.LONG_DESCRIPTION, "Toggle Document panel"},
            //        { BioUMLApplication.DOCUMENT_PANE_NAME      + Action.MNEMONIC_KEY         , "D"},
            //        { BioUMLApplication.DOCUMENT_PANE_NAME      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK | Event.ALT_MASK) },
            {BioUMLApplication.DOCUMENT_PANE_NAME + Action.ACTION_COMMAND_KEY, "cmd-toggle-document"},

            // Toggle Explorer pane action
            {BioUMLApplication.EXPLORER_PANE_NAME + Action.SMALL_ICON, "toggleDescription.gif"},
            {BioUMLApplication.EXPLORER_PANE_NAME + Action.NAME, "Properties"},
            {BioUMLApplication.EXPLORER_PANE_NAME + Action.SHORT_DESCRIPTION, "Properties panel"},
            {BioUMLApplication.EXPLORER_PANE_NAME + Action.LONG_DESCRIPTION, "Toggle properties panel"},
            //        { BioUMLApplication.EXPLORER_PANE_NAME     + Action.MNEMONIC_KEY         , "D"},
            //        { BioUMLApplication.EXPLORER_PANE_NAME     + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK | Event.ALT_MASK) },
            {BioUMLApplication.EXPLORER_PANE_NAME + Action.ACTION_COMMAND_KEY, "cmd-toggle-diagram"},

            // Toggle Editor pane action
            {BioUMLApplication.EDITOR_PANE_NAME + Action.SMALL_ICON, "toggleSettings.gif"},
            {BioUMLApplication.EDITOR_PANE_NAME + Action.NAME, "Diagram editors"},
            {BioUMLApplication.EDITOR_PANE_NAME + Action.SHORT_DESCRIPTION, "Diagram editors and settings panel"},
            {BioUMLApplication.EDITOR_PANE_NAME + Action.LONG_DESCRIPTION, "Toggle editors panel"},
            //        { BioUMLApplication.EDITOR_PANE_NAME     + Action.MNEMONIC_KEY         , "D"},
            //        { BioUMLApplication.EDITOR_PANE_NAME     + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK | Event.ALT_MASK) },
            {BioUMLApplication.EDITOR_PANE_NAME + Action.ACTION_COMMAND_KEY, "cmd-toggle-diagram"},

            // Preferences action
            {PreferencesAction.KEY + Action.SMALL_ICON, "preferences.gif"},
            {PreferencesAction.KEY + Action.NAME, "Preferences"},
            {PreferencesAction.KEY + Action.SHORT_DESCRIPTION, "Preferences"},
            {PreferencesAction.KEY + Action.LONG_DESCRIPTION, "Preferences of application"},
            //        { PreferencesAction.KEY      + Action.MNEMONIC_KEY         , "P"},
            //        { PreferencesAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK | Event.ALT_MASK) },
            {PreferencesAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-preferences"},

            // About action
            {AboutAction.KEY + Action.SMALL_ICON, "about.gif"},
            {AboutAction.KEY + Action.NAME, "About"},
            {AboutAction.KEY + Action.SHORT_DESCRIPTION, "About"},
            {AboutAction.KEY + Action.LONG_DESCRIPTION, "Info about application"},
            //        { AboutAction.KEY      + Action.MNEMONIC_KEY         , "A"},
            //        { AboutAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK | Event.ALT_MASK) },
            {AboutAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-about"},

            // Help action
            {HelpAction.KEY + Action.SMALL_ICON, "help.gif"},
            {HelpAction.KEY + Action.NAME, "Help"},
            {HelpAction.KEY + Action.SHORT_DESCRIPTION, "Help"},
            {HelpAction.KEY + Action.LONG_DESCRIPTION, "Help system"},
            {HelpAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-help"},

            {ModuleSetupAction.KEY + Action.SMALL_ICON, "importModule.gif"},
            {ModuleSetupAction.KEY + Action.NAME, "Import database"},
            {ModuleSetupAction.KEY + Action.SHORT_DESCRIPTION, "Import database"},
            {ModuleSetupAction.KEY + Action.LONG_DESCRIPTION, "Import database"},
            //        { ModuleSetupAction.KEY      + Action.MNEMONIC_KEY         , "S"},
            //        { ModuleSetupAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK | Event.ALT_MASK) },
            {ModuleSetupAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-database-import"},

            // Remove database action
            {RemoveModuleAction.KEY + Action.SMALL_ICON, "removeModule.gif"},
            {RemoveModuleAction.KEY + Action.NAME, "Remove database"},
            {RemoveModuleAction.KEY + Action.SHORT_DESCRIPTION, "Remove database"},
            {RemoveModuleAction.KEY + Action.LONG_DESCRIPTION, "Remove database"},
            //        { RemoveModuleAction.KEY    + Action.MNEMONIC_KEY         , "R"},
            //        { RemoveModuleAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK) },
            {RemoveModuleAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-database-remove"},

            // export database action
            {ExportModuleAction.KEY + Action.SMALL_ICON, "exportModule.gif"},
            {ExportModuleAction.KEY + Action.NAME, "Export database"},
            {ExportModuleAction.KEY + Action.SHORT_DESCRIPTION, "Export database"},
            {ExportModuleAction.KEY + Action.LONG_DESCRIPTION, "Export database"},
            //        { ExportModuleAction.KEY    + Action.MNEMONIC_KEY         , "E"},
            //        { ExportModuleAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK) },
            {ExportModuleAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-database-export"},

            // edit database action
            {EditModuleAction.KEY + Action.SMALL_ICON, "editModule.gif"},
            {EditModuleAction.KEY + Action.NAME, "Edit database"},
            {EditModuleAction.KEY + Action.SHORT_DESCRIPTION, "Edit database"},
            {EditModuleAction.KEY + Action.LONG_DESCRIPTION, "Edit database properties"},
            {EditModuleAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-database-edit"},

            ////////////////////////////////////////////////////////////////////////
            // Clipboard
            //

            {ClipboardPane.COPY + Action.SMALL_ICON, "copy.gif"},
            {ClipboardPane.COPY + Action.NAME, "Copy"},
            {ClipboardPane.COPY + Action.SHORT_DESCRIPTION, "Copy selected diagram elements into clipboard pane."},
            {ClipboardPane.COPY + Action.LONG_DESCRIPTION, "Copy selected diagram elements into clipboard pane."},
            {ClipboardPane.COPY + Action.ACTION_COMMAND_KEY, "cmd-clipboard-copy"},

            {ClipboardPane.PASTE + Action.SMALL_ICON, "paste.gif"},
            {ClipboardPane.PASTE + Action.NAME, "Paste"},
            {ClipboardPane.PASTE + Action.SHORT_DESCRIPTION, "Copy selected items from clipboard into selected compartment on the diagram."},
            {ClipboardPane.PASTE + Action.LONG_DESCRIPTION, "Copy selected items from clipboard into selected compartment on the diagram."},
            {ClipboardPane.PASTE + Action.ACTION_COMMAND_KEY, "cmd-clipboard-paste"},

            {ClipboardPane.DELETE + Action.SMALL_ICON, "remove2.gif"},
            {ClipboardPane.DELETE + Action.NAME, "Remove"},
            {ClipboardPane.DELETE + Action.SHORT_DESCRIPTION, "Remove selected items from clipboard."},
            {ClipboardPane.DELETE + Action.LONG_DESCRIPTION, "Remove selected items from clipboard."},
            {ClipboardPane.DELETE + Action.ACTION_COMMAND_KEY, "cmd-clipboard-delete"},

            ////////////////////////////////////////////////////////////////////////
            // DIALOGS
            //

            {"ABOUT_LOGO_TAB_NAME", "Info"},
            {"ABOUT_CONTACTS_TAB_NAME", "Contacts"},
            {"ABOUT_CONTACTS_HTML", "about_contacts.html"},
            {"ABOUT_WEB_PAGES_TAB_NAME", "Web Pages"},
            {"ABOUT_WEB_PAGES_HTML", "about_web_pages.html"},
            {"ABOUT_NOTES_TAB_NAME", "Notes"},
            {"ABOUT_NOTES_HTML", "about_notes.html"},
            {"ABOUT_OK_BUTTON", "    OK    "},

            {"PIC_EXTENSIONS", "bmp gif jpg jpeg png"},
            {"PIC_EXTENSIONS_TITLE", "All picture formats (*.bmp, *.png, *.jpg, *.jpeg, *.gif"},
            {"PIC_EXISTS", "Picture file with the same name already exists in collection"},

            {"NEW_DATABASE_DIALOG_TITLE", "New Database"},
            {"NEW_DATABASE_DIALOG_TYPE", "Database type"},
            {"NEW_DATABASE_DIALOG_NAME", "Database name"},
            {"NEW_DATABASE_DIALOG_DESCRIPTION", "Database type description:"},
            {"NEW_DATABASE_DESCRIPTION_ABSENTS", "There is no description for this database type."},

            {"NEW_DATABASE_ERROR_TITLE", "New database error"},
            {"NEW_DATABASE_ERROR", "<html>Cannot create new database {0} with type {1}." + "<br>Error: {2}.</html>"},
            {"NEW_DATABASE_EXTENSION_ABSENTS", "There is no plug-ins that can create new database."},
            {"NEW_DATABASE_EXTENSION_ERROR_TYPE",
                    "Database type extension should implement biouml.model.ModuleType interface," + "\n  provider={0}, extension={1}"},
            {"NEW_DATABASE_EXTENSION_WARN",
                    "Database type extension cannot create new emty database and will be ignored," + "\n  provider={0}, extension={1}"},
            {"NEW_DATABASE_EXTENSION_ERROR", "Cannot load database type extension point," + "\n  extension={0}, error: {1}"},

            {"LOAD_DATABASE_DIALOG_TITLE", "Load Database"},
            {"LOAD_DATABASE_DIALOG_SERVER", "Server URL"},
            {"LOAD_DATABASE_USERNAME", "Username"},
            {"LOAD_DATABASE_PASSWORD", "Password"},
            {"LOAD_DATABASE_DIALOG_FIND", "Find databases"},
            {"LOAD_DATABASE_DIALOG_DBINFO", "Get database info"},
            {"LOAD_DATABASE_DIALOG_LOAD", "Install"},
            {"LOAD_DATABASE_DIALOG_HELP", "Help"},
            {"LOAD_DATABASE_DIALOG_INFO", "Messages:"},
            {"LOAD_DATABASE_DIALOG_INSTALL", "Install"},
            {"LOAD_DATABASE_DIALOG_CLIENT_NAME", "Client database name"},
            {"LOAD_DATABASE_DIALOG_SERVER_NAME", "Server database name"},
            {"LOAD_DATABASE_DIALOG_AVAILABILITY","Availability"},
            {"LOAD_DATABASE_DIALOG_ACCESSTYPE","Access type"},
            {
                    "LOAD_DATABASE_DIALOG_INFO_TEXT",
                    "This dialog allows you to install database located on "+"$ApplicationName$"+" server.\n"
                            + "Corresponding database will be shown in \"Databases\" section of repository tree.\n"
                            + "To install database:\n" + "\t1) fill Server address, Server port\n" + "\t2) press 'Find databases' button\n"
                            + "\t3) available databases will be shown \"Available databases\" table\n"
                            + "\t4) select databases to be installed\n" + "\t5) press \"Ok\" button\n"},
            {"LOAD_DATABASES_TABLE_TITLE", "Available databases:"},
            {"LOAD_DATABASE_DIALOG_EXTERNAL_DATABASE_CONFIRM_TITLE", "Need external databases"},
            {"LOAD_DATABASE_DIALOG_EXTERNAL_DATABASE_CONFIRM_TEXT", "For correct work of this database you should also load databases: {0}"},

            {"LOAD_DATABASES_TABLE_INSTALL_SELECTED_TITLE", "Confirm installation"},
            {"LOAD_DATABASES_TABLE_INSTALL_SELECTED_MESSAGE", "Do you want to install selected databases?"},
            ////////////////////////////////////////////////////////////////////////
            // Diagram dialogs
            //

            // common constants
            {"DIALOG_FILE", "File:"},
            {"DIALOG_FORMAT", "Format: "},
            {"DIALOG_INFO", "Messages:"},
            {"DIALOG_CLOSE", "Close"},
            {"DIALOG_CANCEL", "Cancel"},

            {"DIAGRAM_DIALOG_FILE", "File:"},
            {"DIAGRAM_DIALOG_ELEMENT", "Element name:"},
            {"DIAGRAM_DIALOG_FORMAT", "Format: "},
            {"DIAGRAM_DIALOG_INFO", "Messages:"},
            {"DIAGRAM_DIALOG_CLOSE", "Close"},
            {"DIAGRAM_DIALOG_DIAGRAM", "Diagram:"},

            {"NEW_DIAGRAM_DIALOG_TITLE", "New diagram"},
            {"NEW_DIAGRAM_TYPE_DESCRIPTION", "Diagram type description:"},
            {"NEW_DIAGRAM_NO_DATABASES", "There is no available mutable databases"},
            {"NEW_DIAGRAM_ALREADY_EXIST", "The element with name {0} already exist. Do you want to overwrite it?"},

            {"RADIO_DATABASE_DIAGRAM_TYPE", "Database Diagrams"},
            {"RADIO_XML_DIAGRAM_TYPE", "XML Diagrams"},

            {"CONVERT_DIAGRAM_DIALOG_TITLE", "Diagram type converter"},
            {"CONVERT_DIAGRAM_DIALOG_TYPE", "Type: "},
            {"CONVERT_DIAGRAM_DIALOG_NEW_TYPE", "Convert to: "},
            {"CONVERT_DIAGRAM_DIALOG_NEW_NAME", "New name: "},
            {"CONVERT_DIAGRAM_DIALOG_CONVERT", "Convert"},

            {"CONVERT_DIAGRAM_DIALOG_NO_CONVERSIONS", "There is no possible conversions for this diagram type. "},
            {"CONVERT_DIAGRAM_DIALOG_NO_FURTHER_CONVERSIONS", "Further conversion is not available for this diagram type."},
            {"CONVERT_DIAGRAM_DIALOG_START", "Diagram type conversion:\n  diagram {0}({1})\n  {2} -> {3}"},
            {"CONVERT_DIAGRAM_DIALOG_SUCCESS", "Conversion was completed successfully.\n"},
            {
                    "CONVERT_DIAGRAM_DIALOG_ILLEGAL",
                    "Some diagram nodes or edges have unsuitable kernel type."
                            + "\n  If you would like to convert the diagram to the specified type "
                            + "\n  you should delete following diagram elements:{0}\n\n"},

            {"EXPORT_ELEMENT_DIALOG_TITLE", "Export element"},
            {"EXPORT_ELEMENT_DIALOG_EXPORT", "Export"},
            {"EXPORT_ELEMENT_DIALOG_NO_EXPORTERS", "There is no suitable formats to export this element."},
            {
                    "EXPORT_ELEMENT_DIALOG_NO_EXPORTER",
                    "Ops, the element cannot be exported in this format.\n" + "format={0}.\n"
                            + "Please report to info@biouml.org about this error."},
            {"EXPORT_ELEMENT_DIALOG_SUCCESS", "Element {0} was successfully exported in {1} format.\n" + "File: {2}\ntime: {3} ms."},
            {"EXPORT_ELEMENT_DIALOG_ERROR", "Could not export element {0} in format {1}, error: {2}."},
            {"EXPORT_ELEMENT_DIALOG_PREFERENCES_DIR_PN", "Export dialog, directory"},
            {"EXPORT_ELEMENT_DIALOG_PREFERENCES_DIR_PD", "Default directory for file chooser specify file to export element."},

            //Import table data collection properties
            {"IMPORT_PN_FIRST_COLUMN_AS_ID", "Use first column as ID"},
            {"IMPORT_PD_FIRST_COLUMN_AS_ID", "Use first column as ID"},
            
            {"IMPORT_ELEMENT_DIALOG_TITLE", "Import element"},
            {"IMPORT_ELEMENT_DIALOG_IMPORT", "Import"},
            {"IMPORT_ELEMENT_DIALOG_PREFERENCES_DIR_PN", "Import dialog, directory"},
            {"IMPORT_ELEMENT_DIALOG_PREFERENCES_DIR_PD", "Default directory for file chooser to specify file for import."},
            {"IMPORT_ELEMENT_IMPORTING_FILE", "Importing file {0}"},
            {"IMPORT_ELEMENT_IMPORT_STARTED", "Import started"},
            {"IMPORT_ELEMENT_COMPLETE", "Import completed (total time: {0} ms)\nSuccessfully imported files: {1}\nNot imported files: {2}"},
            {"IMPORT_ELEMENT_IMPORT_CANCELLED", "Import cancelled"},
            {"IMPORT_ELEMENT_FAILED", "Import failed: {0}"},
            {"IMPORT_ELEMENT_DIALOG_SUCCESS", "Element was successfully imported as {0} format.\n" + "File: {1}\ntime: {2} ms"},
            {
                    "IMPORT_ELEMENT_DIALOG_INFO",
                    "To import element please specify file name and format."
                            + "\nYou could use format autodetect that will try to recognize " + "\nthe file format automatically."
//                            + "\nElement name field is optional, if it is not filled"
//                            + "\nthen element name will be extracted from file.
                            +"\n\n\n"},

            // errors and messages
            {"FILE_NAME_NOT_SPECIFIED", "File name is not specified."},
            {"INVALID_FILE_NAME", "File of the name '{0}' is not found or not readable."},
            {"DIAGRAM_NAME_NOT_SPECIFIED", "Diagram name is not specified."},

            {
                    "NO_IMPORTERS_AVAILABLE",
                    "Format autodetect: could not detect file {0} format, " + "\n  possibly this format is not supported."
                            + "\n  You can try to select format manually from Format combobox " + "\n  and try to import this file again."},
            {
                    "MORE_THAN_ONE_IMPORTER_AVAILABLE",
                    "Format autodetect: could not detect file {0} format," + "\n  several formats are suitable: {1}"
                            + "\n  Please select format manually from Format combobox." + "\n  and try to import this file again."},
            {"ERROR_IMPORTING_ELEMENT", "Error occurred when importing element: " + "file={0}, format={1}, error:{2}."},
            {"ERROR_IMPORTING_EXCEPTION", "Exception occured while importing element: {0}."},
            {"IMPORTER_CHOSEN", "Format autodetect: format {1}, file {0}."},

            ////////////////////////////////////////////////////////////////////////
            // Miscelaneous
            //

            {"PROXY_ERROR_MESSAGE", "Cannot connect to remote server. Check proxy settings in Preferences dialog."},

            // general messages
            {"BIOUMLEDITOR_TITLE", "$ApplicationName$"+" Editor (spike)"},

            // Panes names
            {"PANE_REPOSITORY", "Repository"},

            {"MENU_FILE", "File"},
            {"MENU_DATABASE", "Database"},
            {"MENU_DIAGRAM", "Diagram"},
            {"MENU_SERVICES", "Services"},
            {"MENU_HELP", "Help"},

            {"CLOSE_CONFIRM_MESSAGE", "Do you want to save the document \"{0}\"?"},
            {"CLOSE_CONFIRM_MESSAGE_2", "This document is not mutable. Do you want to close \"{0}\"?"},
            {"CLOSE_CONFIRM_TITLE", "Close confirmation"},
            {"CLOSE_CONFIRM_TASKS", "Do you want to break executable tasks?"},

            {"INFO_TITLE", "Info"},
            {"CANNOT_MOVE_NODE_INTO_PARENT", "Node \"{0}\" cannot be moved into \"{1}\""},
            {"MESSAGE_NODE_ALREADY_EXIST", "Node \"{0}\" already exists in compartment \"{1}\""},
            {"MESSAGE_ELEMENT_ALREADY_EXIST", "Element \"{0}\" already exists in \"{1}\""},

            // dialog buttons
            {"BUTTON_START", "Start"}, {"BUTTON_CLOSE", "Close"},
            {"BUTTON_NEW", "New"},
            {"BUTTON_OK", "OK"},
            {"BUTTON_YES", "Yes"},
            {"BUTTON_No", "No"},
            {"BUTTON_CANCEL", "Cancel"},
            {"BUTTON_CONTINUE", "Continue"},
            {"BUTTON_SAVE", "Save"},

            {"MODEL_SETTINGS_PANE_SEARCH_LINKED", "Search linked"},
            {"MODEL_SETTINGS_PANE_VARIABLES", "Variables"},
            {"MODEL_SETTINGS_PANE_CONSTANTS", "Constants"},
            {"MODEL_SETTINGS_PANE_START", "Start"},
            {"MODEL_SETTINGS_PANE_MESSAGES", "Messages"},

            {"DIAGRAM", "Diagram"},
            {"DATABASE", "Database"},
            {"DIAGRAM_TYPE", "Diagram type"},
            {"DIAGRAM_NAME", "Diagram name"},

            //{ "MSG_DIAGRAM_CANNOT_BE_USED_FOR_MODELING", "This diagram cannot be used for modeling"},
            {"NEW_DATA_ELEMENT_DIALOG_TITLE", "Data Element"}, {"NEW_DATA_ELEMENT_DIALOG_ID_NAME", "Data element name"},


            {"BMD_FILE_DESCRIPTION", "$ApplicationName$"+" Database Distributive (*{0}, *{1}, *{2})"},

            {"DATABASE_EXPORT_DIALOG_TITLE", "Export Database"},

            {"SELECT_DATABASE", "Select database"}, {"NEW_NAME", "New name"}, {"VERSION", "Version"},
            {"EXISTED_VERSION", "Existed version"}, {"NEW_VERSION", "New version"},
            {"DESCRIPTION", "Description"},
            {"FILE_LOCATION", "File location"},
            {"LICENSE", "License"},
            {"NAME", "Name"},
            {"ACCEPT", "Accept"},

            {"MESSAGE_DATABASE_EXIST", "Database {0} already exists."},
            {"REPLACE_EXISTED_VERSION", "Do you want to replace the existed version by new one?"},

            {"MEMBRANE", "membrane"},
            {"CYTOPLASM", "cytoplasm"},
            {"NUCLEUS", "nucleus"},

            {"EXPORTING", "Exporting"},
            {"IMPORTING", "Importing"},

            //--- Database import -------------------------------------------

            {"DATABASES_ACTIVATING_DIALOG_TITLE", "Not active databases"},
            {"DATABASES_ACTIVATING_DIALOG_NOT_ACTIVE_DATABASES_DETECTED", "$ApplicationName$"+" detects not active databases."},
            {"DATABASES_ACTIVATING_DIALOG_SELECT_DATABASES", "Please select databases to be activated."},

            {"DATABASE_IMPORT_DIRECTORY_PN", "Database import directory"},
            {"DATABASE_IMPORT_DIRECTORY_PD", "Please select directory to import database from."},

            {"DO_NOT_SHOW_DIALOG_AGAIN", "Do not show this dialog again"},

            {"DATABASE_REPLACE_DIALOG_TITLE", "Replace Database"}, {"DATABASE_INFO_DIALOG_TITLE", "Database Info"},

            {"CONFIRM_REMOVE_DATABASE", "Are you sure want to remove database \"{0}\"?"},
            {"CONFIRM_REMOVE_DIAGRAM", "Are you sure want to remove diagram \"{0}\"?"},
            {"CONFIRM_REMOVE_ELEMENT", "Are you sure want to remove element \"{0}\" from collection \"{1}\"?"},

            {"ERROR_CANNOT_REMOVE", "Cannot remove element \"{0}\" from immutable collection \"{1}\"?"},

            {"FINISHED_SUCCESS", "Finished successfully"}, {"FINISHED_ERROR", "Finished with errors"}, {"ERROR", "Error"},

            {"SAVE_DIAGRAM_AS_IMAGE", "Save diagram as image"}, {"IMAGE_FILE", "Image file ({0})"},
            {"SAVE_DIAGRAM_IMAGE_ERROR", "<html>Cannot generate image, error: <br>{1}Save diagram as image</html>"},

            {"GENERATE_HTML_DIALOG_TITLE", "Generate HTML"},

            {"SCOPE", "Scope"}, {"OUTPUT_FOLDER", "Output folder"}, {"SELECT_OUTPUT_FOLDER", "Select Output Folder"},
            {"TEMPLATE", "Template"}, {"SELECT_TEMPLATE", "Select Template"}, {"REFERENCES", "References"},

            {"TEMPLATE_FILE_DESCRIPTION", "HTML generator template (*{0})"},

            {"GENERATING_HTML", "Generating HTML"}, {"GENERATING_HTML_FOR_DATABASE", "Generating HTML for {0} database"},
            {"GENERATING_HTML_FOR_DIAGRAM", "Generating HTML for {0} diagram"}, {"CREATING_IMAGE_SUCCESS", "Creating {0} image - success"},
            {"APPLAYING_TEMPLATE", "Applying template - success"},
            {"HTML_GENERATED_SUCCESSFULLY", "HTML files generated successfully"},

            //--- Journal toolbar constants
            {"JOURNAL_USE", "Use journal "},
            {"JOURNAL_NAME", " name "},
            
            {"PERSPECTIVE_PROMPT", "Perspective:"},
            
            {"COMMENT_INPUT", "Type comment for new version:"},
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
            return TextUtil2.calculateTemplate(getString(key), Application.getPreferences().getValue("Global"), true);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
