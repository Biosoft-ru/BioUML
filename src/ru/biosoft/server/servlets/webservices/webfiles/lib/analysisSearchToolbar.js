
    function initAnalysisSearchToolbar()
    {
    	$('#analysis_search_button').bind('click', function(){
    		var searchString = $('#analysis_search_string').val();
        	runSearch(searchString);
    	});
    	$('#analysis_search_string').bind('keyup', function(event)
	    {
	        if (event.keyCode == 13) 
	        {
	        	var searchString = $('#analysis_search_string').val();
	        	runSearch(searchString);
	        }
	    });
    	if(!activeDC || !(activeDC.getName() == "analyses"))
			$('#analysisSearchToolbar').hide();
    }
    
    window.initAnalysisSearchToolbar = initAnalysisSearchToolbar;
    window.runAnalysisSearch = runSearch;
    addTabChangeListener(new searchChangeListener());
    
    function searchChangeListener()
    {
    	this.tabChanged = function()
    	{
    		//TODO: optimize not to resize while switching between two non-analysis tabs
			if(activeDC && activeDC.getName() == "analyses")
				$('#analysisSearchToolbar').show();
			else
				$('#analysisSearchToolbar').hide();
			el.leftTopPane.trigger('resize');
    	};
    }
    
    function runSearch(searchString, callback)
    {
        $(".btn-search").addClass("ui-state-disabled");
    	queryBioUML("web/analysis/search", {str: searchString}, function(data)
		{
    		toggleUI(true);
    		var resultViewPart = lookForViewPart('search.results');
    		var msg;
    		var toUpdate = false;
			if(data.values == "ok")
			{
				msg = '<p>'+resources.vpSearchSearching+'</p>';
                toUpdate = true;
			}
			else
				msg = '<p>'+resources.vpSearchNotFound+'</p>'
			resultViewPart.setMessage(msg);
			$(".btn-search").removeClass("ui-state-disabled");
            updateViewParts();
            selectViewPart('search.results');
            if(toUpdate)
            	resultViewPart.updateData();
			if(callback)
            	callback();
		});
    }
