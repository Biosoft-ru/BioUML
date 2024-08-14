(function() {
    var contextToolbarActions = [];

    /*
     * Init context toolbar
     */
    function initContextToolbar(actions)
    {
        for (i = 0; i < actions.length; i++) 
        {
            var actionProperties = new Action();
            actionProperties.parse(actions[i]);
            contextToolbarActions[i] = actionProperties;
        }
        updateContextToolbar(null, null, [null]);
    }
    
    /*
     * Update context toolbar actions
     */
    function updateContextToolbar(node, treeObj, paths)
    {
        var nodePaths;
        if (!node || node == null) 
        {
            nodePaths = paths != undefined ? paths : [null];
        }
        else 
        {
            nodePaths = [getTreeNodePath(node)];
        }
        
        var block = $('<div class="fg-buttonset ui-helper-clearfix"></div>');
        for (var i = 0; i < contextToolbarActions.length; i++) 
        {
            if (contextToolbarActions[i].id) 
            {
                if(!contextToolbarActions[i].multi && nodePaths.length > 1)
                    continue;
                var allVisible = nodePaths.every(function(path){
                    return contextToolbarActions[i].isVisible(contextToolbarActions[i].useOriginalPath?path:getTargetPath(path)) === true;
                });
                var visibleType = contextToolbarActions[i].isVisible();
                if (allVisible) 
                {
                    var action = $('<span class="fg-button ui-state-default fg-button-icon-solo  ui-corner-all" title="' + contextToolbarActions[i].label + '"><img class="fg-button-icon-span" src="' + contextToolbarActions[i].icon + '"></img></span>');
                    (function(actionHandler)
                    {
                        action.click(function(event)
                        {
                            _.each(nodePaths, actionHandler);
                        });
                    })(contextToolbarActions[i].doAction);
                    block.append(action);
                }
            }
        }
        var toolbar = $('#contextToolbar');
        toolbar.children('.fg-buttonset').remove();
        toolbar.append(block);
    }

    window.initContextToolbar = initContextToolbar;
    window.updateContextToolbar = updateContextToolbar;
    BioUML.selection.addListener(function(completeName) {
        updateContextToolbar(null, null, [completeName]);
    });
})();