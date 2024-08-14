var Lucene = function(id)
{
    id = typeof id !== 'undefined' ? id : 'lucene_search';
    this.id = id;
    var _this = this;
    this.searchField = $('#' + id + ' #search_string');
    
    this.completeName = "";
    
    _this.searchField.autocomplete({source: function(req, resp)
        {
            if(!_this.completeName)
            {
                resp([]);
                return;
            }
            queryBioUML("web/lucene/suggest", 
            {
                de: _this.completeName,
                query: req.term
            }, function(data)
            {
                resp(data.values);
            }, function()
            {
                resp([]);
            });
        }});
    
    this.setCollection = function(dc)
    {
        this.searchField.autocomplete("close");
        var dbName = dc.getDatabaseName();
        if(dbName == "" && dc.getName() !== 'analyses')
        {
            $("#"+_this.id+" #search_target").html(resources.commonSearchNoCollectionSelected);
            this.completeName = "";
            _this.searchField.attr('disabled','disabled');
        }
        else
        {
            _this.searchField.removeAttr('disabled');
            var completeName = dc.getName();
            if(!instanceOf(dc.getClass(), "ru.biosoft.access.core.DataCollection"))
                completeName = getElementPath(completeName);
            this.completeName = completeName;
            $("#"+_this.id+" #search_target").html(resources.commonSearchCollectionSelected.replace("{collection}", completeName.escapeHTML()));
        }
    };
    
    $('#'+id+' #search_string').attr('disabled','disabled').keyup(function(event)
    {
        if (event.keyCode == 13) 
        {
            startLuceneSearch();
        }
    });
    $('#'+id+' #search_button').bind("click", function()
    {
        startLuceneSearch();
        return false;
    });
    
    $('#'+id+' #filter').hide();
    $('#'+id+' #filter #collections').change(function()
    {
        filterChanged();
    });
    $('#'+id+' #filter #fields').change(function()
    {
        filterChanged();
    });

    function filterChanged()
    {
        var resultViewPart = lookForViewPart('search.results');
        var coll = $('#'+id+' #filter #collections').val();
        var field = $('#'+id+' #filter #fields').val();
        if(coll == '-All-' && field == '-All-')
        {
            resultViewPart.filter = null;
        }
        else
        {
            var f = "";
            var l = "";
            if(coll != '-All-')
            {
                f+="(Path.toString().substr(0, "+coll.length+") === \""+coll+"\")";
                l = " && ";
            }
            if(field != '-All-')
            {
                f+=l+"(Field == \""+field+"\")";
            }
            resultViewPart.filter = f;
        }
        resultViewPart.updateData();
    }

    /*
     * Start lucene search
     */
    function startLuceneSearch()
    {
        _this.searchField.autocomplete("close");
        var searchString = _this.searchField.val();
        if($("#"+_this.id+" #search_add_asterisk").attr("checked"))
            searchString = searchString.replace(/\**$/, "*");
            var path = _this.completeName;
            if (path) 
            {
                var components = getPathComponents(path);
                var dc = _this.completeName;
                var resultViewPart = lookForViewPart('search.results');
                resultViewPart.setMessage('<p>'+resources.vpSearchSearching+'</p>');
                updateViewParts();
                selectViewPart('search.results');
                $('#'+_this.id+' #filter').hide();
                queryBioUML("web/lucene/search",  
                {
                    de: dc,
                    query: searchString,
                    prefix: "<b>",
                    postfix: "</b>"
                }, function(data)
                {
                    if (data.values.results == 0) 
                    {
                        resultViewPart.setMessage("<p>"+resources.vpSearchNotFound+"</p>");
                    }
                    else 
                    {
                        resultViewPart.filter = null;
                        resultViewPart.updateData();
                        $('#'+_this.id+' #filter #collections').children('option').remove();
                        $('#'+_this.id+' #filter #collections').append('<option value="-All-">-All-</option>');
                        $('#'+_this.id+' #filter #fields').children('option').remove();
                        $('#'+_this.id+' #filter #fields').append('<option value="-All-">-All-</option>');
                        if(data.values.collections && data.values.collections.length>1)
                        {
                            for(var i in data.values.collections)
                            {
                                $('#'+_this.id+' #filter #collections').append('<option value="'+data.values.collections[i]+'">'+data.values.collections[i]+'</option>');
                            }
                            $('#'+_this.id+' #filter').show();
                        }
                        if(data.values.fields && data.values.fields.length>1)
                        {
                            for(var i in data.values.fields)
                            {
                                $('#'+_this.id+' #filter #fields').append('<option value="'+data.values.fields[i]+'">'+data.values.fields[i]+'</option>');
                            }
                            $('#'+_this.id+' #filter').show();
                        }
                    }
                }, function(data)
                {
                    resultViewPart.setMessage($('<p/>').html(data.message.replace("\n", "<br>")));
                    logger.message(data.message);
                });
            }
    }
};
$(function() {
    // init lucene search pane
    var luceneSearch = new Lucene();
    BioUML.selection.addListener(function(elementName)
    {
        var dc = getDataCollection(elementName);
        luceneSearch.setCollection( dc );
    });
});
