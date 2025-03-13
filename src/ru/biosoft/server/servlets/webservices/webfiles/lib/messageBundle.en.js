var resourcesEN = {

    repositoryNames: {},
    actionNames: {},

    infoTabInfo: "Info",
    infoTabSearch: "Search",
    infoTabDefault: "Default",

    /* Common section (prefix: common) */
    // Error messages
    commonErrorCannotConnect: "Connection to the server has failed. Please try again later. If the problem persists, please contact us at "+resourceVars.adminMail,
    commonErrorNoData: "Unknown error during query: server returned no data. Please try again and contact us at "+resourceVars.adminMail+" if problem still persists.",
    commonErrorQueryException: "Error during server query: {message}",
    commonErrorUnknownStatus: "Unknown error during query: server returned invalid status. Please try again and contact us at "+resourceVars.adminMail+" if problem still persists.",
    commonErrorInvalidQuery: "Client-server communication error occured. Server reported the following message:<br><br>{message}<br><br> Please try again and contact us at "+resourceVars.adminMail+" if problem still persists.",
    commonErrorInternalError: "{message}<br><br> Please try again and contact us at "+resourceVars.adminMail+" if problem still persists.",
    commonErrorSearchNoCollectionSelected: "Select data collection before search",
    commonErrorActionNoSelectionRequired: "Please don't select anything to complete the action.",    // this message shouldn't be seen by user at all
    commonErrorActionOneSelectedRowRequired: "Please select exactly one element (row) to complete the action.",
    commonErrorActionSelectedRowsRequired: "Please select at least one element (row) to complete the action.",
    commonErrorActionUnknown: "Number of selected items is not defined",    // this message shouldn't be seen by user at all
    commonErrorEmptyNameProhibited: "Please specify the name",
    commonErrorIncompatibleBrowserVersion: "The browser version {version} does not support some of the features required. Please, update the browser to newer version.",
    commonErrorIncompatibleBrowser: "Sorry, but your browser {version} is not fully supported by our system, thus some features may not work. Recommented browsers are: Firefox, Chrome, Safari, Opera.",
    commonErrorViewpartUnavailable: "This tab is unavailable",    // this message shouldn't be seen by user during normal operation
    commonErrorNoSaveACopy: "Save a copy option is not available for this document",
    commonErrorNoSave: "Save option is not available for this document",
    commonErrorNotSavedAs: "Document cannot be saved into {path}.<br>{message}",
    commonErrorSessionExpiredNoUnsaved: "Your session has expired. The most likely causes for this include temporary problems with Internet connection or server restart. Your session will be reinitialized. We are sorry for inconvenience.",
    commonErrorSessionExpiredUnsaved: "Your session has expired. The most likely causes for this include temporary problems with Internet connection or server restart. Your session will be reinitialized.<br><br>You had the following documents unsaved:<br>{documents}<br>These changes will be lost. We are sorry for inconvenience.", 
    commonErrorMissingJob: "Internal problem (requested background job not found). Please try again. If the problem persists, please contact us at "+resourceVars.adminMail,
    commonAddDiagramElementUnavailable: "A new element of the type '{type}' can not be created for this type of diagram. Please, use the database search first to find this element in one of the databases and add it to the diagram from the search results",
    commonProjectIsNotSelected: "Please select a project to continue. Project selection box is located on right-top corner of the window.",
    commonProjectIsNotSelectedNoBox: "Please, select a project to continue. Right click on any of available projects to set as current, or create a new project.",
    
    // Tooltips
    commonTooltipCloseDocument: "Close",
    commonTooltipEditElementInfo: "Edit",
    commonTooltipViewElementInfo: "View",
    
    // Buttons
    buttonYes: "Yes",
    buttonNo: "No",
    buttonCancel: "Cancel",
    
    // Messages
    commonLoading: "Loading...",
    commonWait: "Please wait...",
    commonAuth: "Authenticating...",
    commonConfirmElementsOverwrite: "The following elements already exist: {elements}Do you want to overwrite them?|Overwrite",
    commonConfirmDocumentRevert: "Do you want to revert current document to the latest saved version?|Revert",

    commonResearch: "Research",

    // Search tab
    commonSearchNoCollectionSelected: "<b>Select database to search in...</b>",
    commonSearchCollectionSelected: "Search in <b>{collection}</b>",
    
    // Common dialogs
    commonSessionExpiredTitle: "Session expired",
    commonErrorBoxTitle: "Error",
    commonMessageBoxTitle: "Message",
    commonConfirmBoxTitle: "Confirm",
    commonPlotBoxTitle: "Image",
    commonEditElementInfoTitle: "Edit",
    commonRemoveElementPrompt: "Do you really want to remove: <b>{name}</b>?|Remove",
    commonRemoveElementsPrompt: "Do you really want to remove <b>{count} elements</b>?|Remove all",
    commonDynamicActionPropertiesTitle: "Properties",
    commonSaved: "Document saved",
    commonNotSaved: "Document cannot be saved at current location.<br><br>Would you like to save it with different name?",
    commonNotSavedMessage: "Document cannot be saved in current location. Server reported the following message:<br>{message}<br><br>Would you like to save it with different name?",
    commonConfirmSaveOnClose: "Document {path} was changed. Would you like to save it before close?",
    commonConfirmCloseTemporary: "Document {path} is stored in a temporary folder, it will be removed upon the end of the current session. Would you like to save it permanently before closing?",
    
    // Progress bar messages
    commonProgressComplete: "Completed",
    commonProgressTerminatedByUser: "Terminated by user",
    commonProgressTerminatedByError: "Failed",
    
    // Chat messages
    jabberErrorConnection: "Cannot connect to chat server: chat will be unavailable. We're sorry for incovenience.",
    jabberErrorAuthentication: "Chat server doesn't accept given login & password: chat will be unavailable. We're sorry for incovenience.",
    jabberErrorCustom: "Error communicating to chat server: {message}. Chat will be unavailable. We're sorry for incovenience.",

    /* Menus (prefix: menu) */
    menuCloseTab: "Close tab",
    menuCloseOtherTabs: "Close other tabs",
    menuCloseAllTabs: "Close all tabs",
    menuShowInTree: "Show in tree",
    menuHideThisColumn: "Hide this column",
    menuShowColumn: "Show column {name}",
    menuRemoveFromUserToolbar: "Remove from toolbar",
    menuRemoveFromView: "Remove from view",
    
    /* Upload controls */
    uploadFromComputerTitle: "Computer",
    uploadFromComputerToolTip: "Select file from your computer",
    uploadFromWebTitle: "Web/FTP",
    uploadFromWebToolTip: "Type an HTTP or FTP URL to get the file from",
    uploadFromRepositoryTitle: "Repository",
    uploadFromRepositoryToolTip: "Import generic file which was previously uploaded into repository",
    uploadFromContentTitle: "Raw",
    uploadFromContentToolTip: "Type or paste file content to the text-area",
    
    /* Dialogs (prefix: dlg) */ 

    dlgButtonOk: "Ok",
    dlgButtonImport: "Import",
    dlgButtonImportAll: "Import all",
    dlgButtonStart: "Start",
    dlgButtonConfirm: "Confirm",
    dlgButtonChange: "Change",
    dlgButtonSave: "Save",
    dlgButtonView: "View",
    dlgButtonOpen: "Open",
    dlgButtonAdd: "Add",
    dlgButtonCreate: "Create",
    dlgButtonRemove: "Remove",
    dlgButtonYes: "Yes",
    dlgButtonNo: "No",
    dlgButtonClose: "Close",
    dlgButtonCancel: "Cancel",

    dlgOpenSaveErrorNotFound: "Element '{element}' not found",
    dlgOpenSaveErrorBadFolder: "Folder '{folder}' doesn't contain appropriate items.",    // displayed when drag-n-drop folder into multi-select control
    dlgOpenSaveErrorReadOnlyFolder: "Unable to save into selected folder.",
    dlgOpenSaveErrorCannotAcceptFolder: "Please select non-folder element.",
    dlgOpenSaveErrorIncompatibleFolder: "Selected collection doesn't accept required type. Please select proper collection.",
    dlgOpenSaveErrorInvalidType: "Element '{element}' has invalid type.",
    dlgOpenSaveErrorInvalidTypeVerbose: "Element '{element}' has invalid type.<br>Allowed type is <b>{type}</b>.",
    dlgOpenSaveErrorInvalidCharacters: "Name contains invalid symbols",
    dlgOpenSaveErrorInvalidCharactersVerbose: "Name contains invalid symbols.<br>Allowed characters are: the numbers 0 - 9,<br>the letters A - Z (both uppercase and lowercase),<br>and some common symbols such as @ # * and &.",
    
    dlgOpenSaveWarnBigFolder: "Folder is too big, thus not all elements might be added",    // displayed when drag-n-drop folder into multi-select control
    dlgOpenSaveConfirmOverwrite: "Element '{element}' already exists. Do you really want to overwrite it?|Overwrite",
    dlgOpenSaveAddButtonTitle: "Click here to add elements",    // floating tooltip for "+" button in multiselect mode
    dlgOpenSaveAddModeTitleText: ": add elements", // additional text for dialog title in multiselect mode when "+" button was pressed
    dlgOpenSaveButtonAuto: "Auto", // text on the button which resets path to default value
    dlgOpenSaveButtonUp: "Up", // One folder up button
    dlgOpenSaveButtonNewFolder: "+", // Create folder button
    dlgOpenSaveButtonNewFolderTitle: "Create new folder", // Create folder button floating text
    dlgOpenSaveLabelCollection: "Folder:",
    dlgOpenSaveLabelName: "Name:",
    dlgOpenSaveLabelType: "Type:",
    
    dlgCreateElementTitle: "Create data element",
    dlgCreateElementName: "Data element name",
    dlgCreateScriptTitle: "Create script",
    dlgCreateScriptDefaultName: "New script",
    dlgCreateFolderTitle: "Create folder",
    dlgCreateFolderName: "Folder name:",
    dlgCreateWorkflowTitle: "Specify the name for the new workflow",
    dlgCreateWorkflowDefaultName: "New workflow",
    dlgCreateResearchTitle: "Specify the name for the new research diagram",
    dlgCreateResearchDefaultName: "New research",
    dlgSaveAsTitle: "Save a copy",
    
    dlgEditViewOptionsTitle: "View options",
    
    dlgCreateDiagramTitle: "Create diagram",
    dlgCreateDiagramDatabase: "Database name:",
    dlgCreateDiagramName: "Diagram name:",
    dlgCreateDiagramType: "Diagram type:",
    dlgCreateDiagramTypeDescription: "Diagram type description:",
    dlgCreateDiagramNoTypes: "No diagram types available",
    dlgCreateDiagramDefaultName: "New diagram",
    
    dlgConvertDiagramTitle: "Convert diagram",
    dlgConvertDiagramSuccess: "Diagram was successfully converted",
    
    dlgCreateOptimizationTitle: "Create optimization document",
    dlgCreateOptimizationName: "Optimization",
    dlgCreateOptimizationDiagram: "Diagram",
    dlgCreateOptimizationErrorNoPath: "Please, select optimization path",
    dlgCreateOptimizationErrorNoDiagram: "Please, select diagram",
    dlgCreateOptimizationResultTitle: "Save optimization result",
    dlgCreateOptimizationResultDefaultName: "New result",
    
    dlgLoginTitle: "Login",
    dlgLoginPrompt: "Enter e-mail and password:",
    dlgLoginServer: "Server:",
    dlgLoginPlatform: "Platform:",
    dlgLoginUsername: "E-mail:",
    dlgLoginPassword: "Password:",
    dlgLoginForgotPassword: "Forgot your password?",
    dlgLoginButtonLogin: "Login",
    dlgLoginButtonAnonymous: "Demo",
    dlgLoginButtonRegister: "Register",
    
    dlgPlotEditorTitle: "Plot editor",
    dlgPlotEditorLoading: "Loading series table...",
    dlgPlotEditorPlotPath: "Plot path:",
    dlgPlotEditorAddSeriesTitle: "New series",
    dlgPlotEditorAddSeriesTable: "Simulation result/experiment table:",
    dlgPlotEditorAddSeriesX: "X variable",
    dlgPlotEditorAddSeriesY: "Y variable",
    dlgPlotEditorSeriesSave: "Save series changes and update plot",
    dlgPlotEditorEdit: "Edit plot options",
    dlgPlotEditorTableNoPlots: "Plots not found for diagram <b>{diagram}</b>. Please, create plot to edit variables.",
    
    dlgImportTitle: "Import file",
    dlgImportErrorNoCollection: "Please select a folder to import to",
    dlgImportErrorNoImportersForCollection: "Import into the selected folder is not possible. Please choose another folder to import your data.",
    dlgImportErrorUploadFailed: "Upload failed:",

    dlgImportErrorImportFailed:"Import failed.<br><br>Please contact "+resourceVars.adminMail+" to help you with your import.<br><br>", 
    dlgImportEmptyOptions: "No options applicable for the selected format",
    dlgImportFile: "Import file from:",
    dlgImportTargetFolder: "Target folder",
    dlgImportFormat: "Format:",
    dlgImportDetectedFormat: "(detected)",
    dlgImportUploadingFile: "Uploading file <b>{file}</b>...",
    dlgImportFileUploaded: "File <b>{file}</b> is uploaded.<br>Please verify format and options and press 'Import' to proceed.",
    dlgImportImportingFile: "Importing file <b>{file}</b>...",
    dlgImportSelectFromRepo: "Select from repository",
    dlgImportTypeContent: "Type file content",
    dlgImportUploadURL: "Upload from FTP",
    
    dlgExportTitle: "Export document",
    dlgExportFormat: "Format:",
    dlgExportPropertiesTitle: "Export properties",
    
    dlgColumnExpressionTitle: "Set column expression",
    
    dlgNewNodeTitle: "Create new node",
    dlgNewNodeSelectKernel: "Select element kernel",
    
    dlgNewEdgeTitle: "Create new edge",
    dlgNewEdgeInput: "Input node",
    dlgNewEdgeOutput: "Output node",
    
    dlgNewPortTitle: "Create port",
    dlgNewPortPrompt: "Create port for element",
    
    dlgUploadURLTitle : "Upload from URL",
    dlgUploadURLPrompt : "ftp://user:pass@ftp.somehost.org/file.txt",
    
    dlgTypeContentTitle: "Type file content",
    dlgTypeContentName: "Name: ",
    dlgTypeContentDefaultName: "data.txt",
    dlgTypeContentPrompt: "Type file content here",
    dlgTypeContentErrorNoName: "Please, type file name", 
    
    dlgConfirmTitle : "Please, confirm changes",
    
    dlgChangePassOldPass: "Old password:",
    dlgChangePassNewPass: "New password:",
    dlgChangeQuotaTitle: "Change quota for {project}",
    
    dlgCreatePrjTitle : "Create new project",
    dlgCreatePrjCreated : "Project {name} successfully created",
    dlgCreatePrjName: "Project name:",
    dlgCreatePrjDescription: "Description:",
    dlgCreatePrjDescriptionPlaceholder: "Type project description here",
    
    dlgRemovePrjTitle: "Remove project",
    dlgRemovePrjConfirmation: "Yes, I really want to remove project <b>{name}</b> with all data.",
    dlgRemovePrjNotConfirmed: "You didn't check the confirmation checkbox",
    dlgRemovePrjRemoved: "Project {name} removed",
    
    dlgSearchPrjTitle : "Search for Project",
    
    dlgRenamePrjTitle: "Rename Project",
    dlgRenamePrjName: "New project name:",
    dlgRenamePrjPrompt: "Note, project title will be shown in the repository tree, but all complete paths will contain project name.",
    
    dlgEnterCommentTitle: "Enter comment",
    dlgEnterCommentName: "Version comment (Ctrl+Enter to finish):",
    
    dlgEditPrjProperties: "Project properties for {project}",
    dlgEditPrjPropertiesGeneral: "Select current project and edit properties",
    
    dlgCreateWorkTitle: "Create new work",
    dlgEditWorkTitle: "Edit work",
    
    dlgTableValueTitle: "Value",
    
    dlgEditJavaScriptTitle: "JavaScript editor",
    dlgEditJavaScriptHint: "Press Ctrl+Enter to save",
    
    dlgCreateReactionComponentAlreadyExist: "Reaction already contains component '{name}' with role '{role}'. "+
    "<br>You can use stoichiometric coefficient to indicate " +
    "<br>how much molecules of the same specie is involved in reation.",
    
    dlgProjectsSize: "The disk space currently taken up by your projects in the system is {sizeTotal}",

    // Tree
    treeLoading: "Loading...",
    treeLoadNextItems: "Load next {itemsToLoad} ({itemsNotLoaded} not loaded)",
    treeLoadLastItems: "Load last {itemsToLoad}",
    
    /* Viewparts (tabs below the document) (prefix: vp) */
    
    vpScriptTitle: "Script",
    vpScriptScriptContext: "Language: ",
    vpScriptButtonExecute: "Execute",
    
    vpDescriptionTitle: "My description",
    vpDescriptionNothingSelected: "Select tree element to see description",
    vpDescriptionButtonEdit: "Edit",
    vpDescriptionButtonSave: "Save",
    vpDescriptionCollectionTitle: "Data collection: <b>{collection}</b>",
    vpDescriptionPrompt: "<span style='color:gray'>Put your comment here - press Edit button above</span>",
    vpDescriptionNotAvailable: "<span style='color:gray'>User description is not available</span>",
    
    vpSearchTitle: "Search result",
    vpSearchErrorCannotLoad: "Can't load the resulting table",
    vpSearchErrorNoSelection: "Select search result to add",
    vpSearchSearching: "Searching...",
    vpSearchNotFound: "Nothing found. Please change the search parameters and try again.",
    vpSearchNotAvailable: "Search is not available for this data collection.",
    vpSearchNoResults: "No results to save",
    vpSearchButtonAdd: "Add to diagram",
    vpSearchButtonFullMode: "Full mode",
    vpSearchButtonExport: "Export search results",
    vpSearchButtonSaveTable: "Save search results table into repository",
    vpSearchButtonCopyClipboard: "Add selected elements to clipboard",
    vpSearchNewDiagramTitle: "Specify new diagram name",
    vpSearchSaveTableDialogTitle: "Save table as...",
    vpSearchHideViewpart: "Close search results",
    
    vpScriptOutputTitle: "Output",
    
    vpModelParametersLoading: "Loading {type} table...",
    vpModelButtonAdd: "Add selected {type} to the fitting set",
    vpModelButtonAddVariable: "Add variable",
    vpModelButtonRemove: "Remove selected {type} from the fitting set",
    vpModelButtonRemoveVariables: "Remove selected unused variables from model",
    vpModelButtonSave: "Save",
    vpModelButtonHighlightOn: "Highlight diagram nodes containing selected {type}",
    vpModelButtonHighlightOff: "Clear diagram highlight",
    vpModelButtonDetectTypes: "Detect {type} types",
    vpModelButtonAddToPlot: "Add selected {type} to plot",
    vpModelErrorRemoveVars: "{quantity} selected variables are used in the model and can not be removed. Please, delete them from model first.",
    vpModelSaveAutomaticallyOne: "Table for {type} is modified. Do you want to save it?",
    vpModelSaveAutomatically: "Tables for {type} are modified. Do you want to save them?",
    
    vpClipboardTitle: "Clipboard",
    vpClipboardErrorAlreadyAdded: "Element <b>{name}</b> is already saved to clipboard.",
    vpClipboardErrorNoDiagramToPaste: "You should open diagram to add elements",
    vpClipboardErrorNoRowSelected: "Select clipboard row to add",
    vpClipboardHeaderPath: "Path",
    vpClipboardHeaderName: "Name",
    vpClipboardHeaderType: "Type",
    vpClipboardButtonCopy: "Add element to clipboard",
    vpClipboardButtonPaste: "Insert element into diagram",
    vpClipboardButtonRemove: "Remove element from clipboard",
    
    vpGraphSearchTitle: "Graph search",
    vpGraphSearchErrorAlreadyAdded: "Element is already in the list.",
    vpGraphSearchErrorNoInput: "No input elements were selected. Please, check '{action}' for the input elements in the elements table",
    vpGraphSearchErrorNoList: "No elements in the elements table. Please, drag-and-drop appropriate element from the tree, or select element on a diagram and use 'Add element' button",
    vpGraphSearchErrorNoElementSelected: "No elements were selected. Please, open diagram and select element to add",
    vpGraphSearchErrorNoDiagram: "Please, open diagram to add elements of use 'Add to new diagram' button",
    vpGraphSearchErrorNoResult: "No neighbors reached with the current depth and direction, please alter the options",
    vpGraphSearchErrorNoDatabase : "Sorry. No graph search can be done for this element. There is no respective network database currently available.",
    vpGraphSearchErrorGeneral : "The graph search cannot be performed with the current settings. Try to change search settings and run the search again.",
    vpGraphSearchLoadingTable: "Loading table...",
    vpGraphSearchLoadingProperties: "Loading properties...",
    vpGraphSearchHeaderAdd: "Add",
    vpGraphSearchHeaderUse: "Use",
    vpGraphSearchHeaderDatabase: "Database",
    vpGraphSearchHeaderID: "ID",
    vpGraphSearchHeaderTitle: "Title",
    vpGraphSearchHeaderType: "Type",
    vpGraphSearchHeaderLinkedFrom: "Linked from",
    vpGraphSearchButtonAdd: "Add element",
    vpGraphSearchButtonStart: "Start search",
    vpGraphSearchButtonClear: "Clear elements pane",
    vpGraphSearchButtonPaste: "Add to current diagram",
    vpGraphSearchButtonPasteToNew: "Add to a new diagram",
    vpGraphSearchNewDiagramTitle: "Specify new diagram name",
    
    vpSQLTitle: "SQL Editor",
    vpSQLSelectTable: "--Select table to view columns--",
    vpSQLHistory: "History:",
    vpSQLTables: "Tables:",
    vpSQLColumns: "Columns:",
    vpSQLButtonRun: "Run query",
    vpSQLButtonExplain: "Explain plan",
    vpSQLButtonClear: "Clear query",
    vpSQLButtonReload: "Reload tables",
    
    vpTasksTitle: "Tasks",
    vpTasksButtonStop: "Stop selected tasks",
    vpTasksButtonRemove: "Remove selected tasks",
    
    vpSimulationTitle: "Simulation",
    vpSimulationButtonSave: "Save simulator options",
    vpSimulationButtonSimulate: "Simulate",
    vpSimulationButtonSaveResult: "Save simulation result",
    vpSimulationResultTitle: "Simulation result",
    vpSimulationResultComplete: "Completed",
    vpSimulationResultSaved: "Simulation result saved",
    
    vpPlot: "Plot",
    
    vpUnits: "Units",
    
    vpFbc: "Flux Balance",
    vpFbcShowTable: "Show table",
    vpFbcCalculate: "Calculate",
    vpFbcExport: "Save current table",
    vpFbcSelectorDialogTitle: "Select optimization options",
    vpFbcSelectorType: "Function type",
    vpFbcSelectorSolver: "Solver",
    
    // Table-specific viewparts
    vpTableFilterTitle: "Filters",
    vpTableFilterButtonApply: "Apply filters",
    vpTableFilterButtonClear: "Remove filters",
    vpTableFilterButtonExport: "Export filtered table",
    vpTableFilterSaveDialogTitle: "Specify name of the export result table",
    
    vpTableDetailsTitle: "Details",
    
    vpTableStructureTitle: "Structure",
    
    vpColumnsTitle: "Columns",
    vpColumnsButtonRefresh: "Recalculate document",
    vpColumnsButtonAdd: "Add column",
    vpColumnsButtonRemove: "Remove selected columns",
    vpColumnsButtonConvert: "Convert selected columns to values",
    vpColumnsConfirmRemove: "Do you really want to remove selected column(s) with all data?|Remove",
    vpColumnsConfirmConvert: "Do you want to convert selected column(s) to values?|Convert",
    vpColumnsNoSelection: "Please select at least one column",
    
    vpGenomeBrowserTitle: "Genome browser",
    
    vpSiteColorsTitle: "Site colors",
    
    // Genome-browser specific view parts
    vpSitesTitle: "Sites",
    vpSitesTrackPrompt: "Track:",
    vpSitesNoTracks: "Please add tracks to genome browser to see the sites",
    
    vpTracksTitle: "Tracks",
    vpTracksTrackOptions: "Options",
    
    // Diagram-specific viewparts
    vpOverviewTitle: "Overview",
    
    vpHemodynamicsTitle: "Hemodynamics",
    vpHemodynamicsProperties: "Properties",
    vpHemodynamicsVesselsTable: "Table of vessels",
    vpHemodynamicsButtonRun: "Run hemodynamics",
    vpHemodynamicsButtonStop: "Stop hemodynamics",
    vpHemodynamicsButtonSave: "Save changes",
    vpHemodynamicsButtonOpenPlot: "Open plot dialog to visualize simulation results",
    vpHemodynamicsPlotWindowTitle: "Hemodynamics graphics",
    
    vpAgentSimulationTitle: "Agent Simulation",
    vpAgentSimulationButtonRun: "Simulate",
    vpAgentSimulationButtonStop: "Stop simulation",
    vpAgentSimulationButtonSave: "Save",
    vpAgentSimulationPlotWindowTitle: "Simulation plot {plotnames}",
    
    vpLayoutTitle: "Layout",
    vpLayoutPrompt: "<b>Layouter:</b> ",
    vpLayoutErrorCannotLoadLayouters: "Cannot load layouters list",
    vpLayoutButtonRun: "Prepare layout",
    vpLayoutButtonStop: "Stop layout",
    vpLayoutButtonApply: "Apply layout",
    vpLayoutButtonSave: "Save layout",
    vpLayoutProcessing: "Layouting in process... It may take a few minutes.",
    vpLayoutErrorLayouting: "Cannot layout diagram. Try to change options or select another layouter.",
    
    vpWorkflowTitle: "Workflow",
    vpWorkflowButtonRun: "Run workflow",
    vpWorkflowButtonStop: "Stop workflow",
    vpWorkflowButtonBind: "Bind property to variable",
    vpWorkflowButtonReturn: "Return to 'run workflow' mode",
    vpWorkflowParametersDialogTitle: "Workflow parameters",
    vpWorkflowParametersDialogSaveResearch: "Save research diagram",
    vpWorkflowComplete: "Workflow execution complete",
    vpWorkflowFailed: "Workflow failed: {error}",
    
    vpJournalTitle: "History",
    vpJournalButtonPaste: "Paste selected element(s) to diagram",
    vpJournalButtonRemove: "Remove selected element(s) from journal",
    vpJournalButtonRemoveAll: "Remove all records from journal",
    vpJournalConfirmRemoveAll: "Do you really want to clear the journal?|Clear",
    
    vpDiagramTableColumns: "Table columns",
    
    vpDiagramFilterTitle: "Expression mapping",
    vpDiagramFilterQuickMappingHint: "To map expression values on the diagram, drag and drop the corresponding table from the tree area over the diagram. You can then adjust parameters in the form that will be displayed.",
    vpDiagramFilterNoMapping: "(none)",
    vpDiagramFilterMappingPrompt: "Mapping: ",
    vpDiagramFilterButtonNew: "Create new mapping",
    vpDiagramFilterButtonRemove: "Remove mapping",
    vpDiagramFilterNewDialogTitle: "Add mapping",
    vpDiagramFilterNewDialogPrompt: "Mapping name:",
    vpDiagramFilterConfirmRemove: "Do you really want to remove mapping '{filter}'?|Remove",
    
    vpVersionTitle: "Version history",
    vpVersionCommentTitle: "<b>Comment:</b>",
    vpVersionCurrentItem: "(current)",
    
    vpOptimizationTitle: "Optimization",
    vpOptimizationMethod: "Method:",
    vpOptimizationButtonStart: "Start optimization process",
    vpOptimizationButtonStop: "Stop optimization process",
    vpOptimizationButtonPlot: "Open plot dialog to visualize simulation results",
    vpOptimizationButtonDiagram: "Open optimization diagram",
    vpOptimizationErrorLoading: "Can not load method parameters",
    vpOptimizationInfoObjective: "Objective function: ",
    vpOptimizationInfoPenalty: "Penalty function: ",
    vpOptimizationInfoEvaluations: "Evaluations: ",
    vpOptimizationButtonSetInitial: "Set initial values",
    vpOptimizationDialogSetInitial: "Select a table with initial values",
    vpOptMethod: "Method",
    vpOptExperiments: "Experiments",
    vpOptConstraints: "Constraints",
    vpOptVariables: "Variables",
    vpOptEntities: "Entities",
    vpOptSimulation: "Simulation",

    vpExperimentsSaved: "Experiment {expname} saved",
        
    vpTabLogsTitle: "Logs",
    vpTabLogsUnavailable: "Log is unavailable",
    vpTabLogsRefresh: "Refresh",
    
    vpSettings: "Settings", 
    vpSettingsDialogTitle: "Adjust viewparts visibility",
    vpSettingsTitle: "Viewpart name",
    vpSettingsChecked: "Visible",
    
    /* Analysis document (prefix: an) */
    anLoading: "Loading properties...",
    anButtonRun: "Run",
    anButtonStop: "Cancel",
    anButtonScript: "Generate script",
    anButtonExpertMode: "Show expert options &gt;&gt;",
    anButtonSimpleMode: "&lt;&lt; Hide expert options",
    anErrorNoScript: "Analysis doesn't provide any scripts to launch it",
    anDialogScriptsTitle: "Scripts",
    anDialogScriptsButtonPaste: "Paste to console",
    
    anGalaxyDataSourceHint: "Select output folder and click on the 'link' to visit external database. There you can use 'Export to Galaxy' option to import data into the selected folder.",
    
    /* Diagram document (prefix: dgr) */
    dgrMenuRemove: "Remove",
    dgrMenuEdgeStraighten: "Straighten edge",
    dgrMenuEdgeRemove: "Remove edge",
    dgrMenuVertexAdd: "Add vertex",
    dgrMenuVertexRemove: "Remove vertex",
    dgrMenuVertexLinear: "Line",
    dgrMenuVertexQuadric: "Quadric",
    dgrMenuVertexCubic: "Cubic",
    dgrMenuAlignTop: "Align top",
    dgrMenuAlignBottom: "Align bottom",
    dgrMenuAlignLeft: "Align left",
    dgrMenuAlignRight: "Align right",
    dgrMenuSetFixed: "Fix node",
    dgrMenuSetUnFixed: "Unfix node",
    dgrMenuSetEdgeFixed: "Manual layout",
    dgrMenuSetEdgeUnFixed: "Auto layout",
    dgrMenuEdit: "Edit",
    dgrMenuCopy: "Copy",
    
    dgrButtonHistory: "History",
    dgrButtonUsers: "Users",
    dgrHistoryPanelTitle: "History",
    dgrUsersPanelTitle: "Users",
    dgrChatPanelTitle: "Chat",
    dgrHistoryPanelApply: "Revert document to the selected version",
    dgrHistoryPanelRestore: "Return to the latest version",
    
    /* Workflow document (not workflow diagram!) (prefix: wf) */
    wfLoading: "Loading workflow properties...",
    wfButtonRun: "Run workflow",
    wfButtonStop: "Cancel",
    wfButtonEdit: "Edit workflow",
    
    /* Genome browser (prefix: gb) */
    gbSequencePrompt: "Chromosome:",
    gbSequenceSelectTitle: "Select a sequence (chromosome)",
    gbPositionPrompt: "Position:",
    gbPositionButton: "Set",
    gbFindPrompt: "Find:",
    gbFindTitle: "Find site by ID",
    gbFindButton: "Go",
    gbErrorWrongSequenceSet: "Selected sequence doesn't belong to the current sequence set (genome)",
    gbErrorInvalidSequence: "Sequence (chromosome) not found: {sequence}",
    gbErrorTrackAlreadyPresent: "Track {path} already present",
    gbErrorNoTracksToExport: "No tracks to export",
    gbErrorSiteNotFound: "Site not found",
    gbProjectDialogTitle: "Add tracks to genome browser",
    gbExportDialogTitle: "Export track",
    gbExportDialogTrack: "Track:",
    gbExportDialogFormat: "Format:",
    gbExportDialogRangeDisplayed: "Displayed range",
    gbExportDialogRangeWhole: "Whole sequence",
    gbExportDialogRangeCustom: "Custom range",
    gbExportDialogProgress: "Preparing file...",
    gbNextSite: "Scroll to the next site",
    gbPreviousSite: "Scroll to the previous site",
    gbSaveProjectDialogTitle: "Save view",
    
    /* Table document (prefix: tbl) */
    tblButtonEdit: "Edit",
    tblButtonApplyEdit: "Apply",
    tblButtonCancelEdit: "Cancel",
    tblButtonSelectAll: "Select all",
    tblButtonSelectPage: "Select page",
    tblButtonChangeView: "Change view",
    tblFilterMessage: "Filter: {filter}",
    tblExpressionHint: "Note:<br/>"+
        "Expression should be written in JavaScript language. You can refer to the row values using column name as the identifier. "+
        "Non-word characters in the column names are replaced with underscore character.<br><br>"+
        "Expression example: <b>Score > 0.5 && Group_number == 1</b><br><br>"+
        "You can also use predefined templates for easy creation of the filtering expressions.",
    tblExpressionPrompt: "Expression in JavaScript language:",
    tblExpressionColumnList: "Columns (double-click to paste):",
    tblExpressionTemplatePrompt: "Template to construct the filtering expression:",
    tblExpressionSelectTemplate: "- Select template -",
    tblExpressionTemplatesColumn:
        [["Log2 of column", "Math.log($1)/Math.LN2", "C:Column"],
         ["Log10 of column", "Math.log($1)/Math.LN10", "C:Column"],
         ["Fraction of two columns", "$1/$2", "C:Numerator", "C:Denominator"]],
    tblExpressionTemplatesFilter:
        [["Above threshold", "$1 > $2", "C:Column", "N:Threshold"],
         ["Below threshold", "$1 < $2", "C:Column", "N:Threshold"],
         ["Equals to value", "$1 == $2", "C:Column", "N:Value"],
         ["Inside range", "$1 > $2 && $1 < $3", "C:Column", "N:Lower bound", "N:Upper bound"],
         ["Outside range", "$1 < $2 || $1 > $3", "C:Column", "N:Lower bound", "N:Upper bound"],
         ["Contains sub-string", "any($1,function(e){return e.indexOf($2)>=0})", "C:Column", "S:Sub-string"],
         ["Starts with sub-string", "any($1,function(e){return e.indexOf($2)==0})", "C:Column", "S:Sub-string"],
         ["Contains sub-string (case-insensitive)", "any($1,function(e){return e.toLowerCase().indexOf($2.toLowerCase()) >= 0})", "C:Column", "S:Sub-string"],
         ["Starts with sub-string (case-insensitive)", "any($1,function(e){return e.toLowerCase().indexOf($2.toLowerCase()) == 0})", "C:Column", "S:Sub-string"]
         ],
    
    end: ""
};

