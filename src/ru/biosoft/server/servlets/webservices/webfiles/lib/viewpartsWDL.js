/*
 *  Initialize view parts for WDL
 */
function initWDLViewParts()
{
  if(viewPartsInitialized['wdl'])
    return;

  viewParts.push(new WDLDiagramViewPart());
  viewPartsInitialized['wdl'] = true;
}



function WDLDiagramViewPart()
{
    this.tabId = "wdl.diagram";
    this.tabName = "WDL diagram"
    this.tabDiv = createViewPartContainer(this.tabId);
    this.toReload = false;
    var _this = this;
    
    this.controllerDiv = $('<div class="overview_controller"/>');
    this.tabDiv.append(this.controllerDiv);
    
    /*
   * Indicates if view part is visible
   */
    this.isVisible = function(documentObject, callback)
    {
        if(documentObject == null || (!(documentObject instanceof ScriptDocument) && !(documentObject instanceof AnalysisDocument))) {
          callback(false);
          return;
        }
        var completePath = this.getWDLPath(documentObject);
        if(completePath == null)
            return;
        
        var path = getElementPath(completePath);
        var name = getElementName(completePath);
        if(!path || !name) return -1;
        var dc = getDataCollection(path);
        var type = dc.getChildClass(name);
        if (instanceOf(type,'biouml.plugins.wdl.WDLScript'))
        {
            callback(true);
        }
        else
        {
            callback(false);
        }
    };
    
    /*
   * Open document event handler
   */
    this.explore = function(documentObject)
    {
            this.openOverview(documentObject);
            if(documentObject instanceof ScriptDocument)
                documentObject.addContentListener(this);
    };

    this.contentChanged = function(documentObject) {
      this.openOverview(documentObject);
    };
    
    /*
   * Save function
   */
    this.save = function()
    {
        // nothing to do
    };
  
    /*
     * Show viewpart event handler
     */
    this.show = function(documentObject)
    {
        if (this.toReload) 
        {
            this.openOverview(documentObject);
        }    
    };
    
    this.openOverview = function(documentObject)
    {
       var path = this.getWDLPath(documentObject);
      _this.width = 600;
      _this.height = 400;
      _this.scale = 1;
      if(path != null)
          queryBioUML("web/wdl",
          {
              action: "get_diagram_view",
              de: path
          }, function(data)
          {
              var viewJSON = data.values;
    
              if (!_this.viewPaneDiv)
              {
                _this.viewPaneDiv = $('<div class="diagramOverview"></div>');
                _this.controllerDiv.prepend(_this.viewPaneDiv);
                _this.viewPane = new ViewPane(_this.viewPaneDiv, {
                        dragAxis: 'none',
                        fullWidth: false,
                        fullHeight: false,
                        tile: 20
                });
              }
    
              var diagramView = CompositeView.createView(viewJSON, _this.viewPane.getContext());
              _this.viewPane.setView(diagramView, true);
              _this.viewPane.scale(_this.scale, _this.scale, false);
              _this.viewPane.invalidateTiles();
              _this.viewPane.repaint();
      });
    };
    
    this.getWDLPath = function (documentObject)
    {
        var completePath = documentObject.completeName;
        if(documentObject instanceof AnalysisDocument)
        {
            completePath = documentObject.getParameter("wdlScriptPath");
        }
        return completePath;
    };
    
}
