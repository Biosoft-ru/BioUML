/* $Id: serverquery.js,v 1.11 2013/08/21 07:22:35 lan Exp $ */
/*
 * Functions to talk with BioUML server
 * appInfo.serverPath should be defined and point to biouml server path (e.g.: "../biouml")
 */

/*
 * Types of result
 */
var QUERY_TYPE_SUCCESS = 0;
var QUERY_TYPE_REPORTED_ERROR = 1;
var QUERY_TYPE_ADDITIONAL = 2;
var QUERY_TYPE_UNREPORTED_ERROR = 3;
var QUERY_TYPE_REQUEST_FAILED = 4;

var activeQueries = {};

function ping()
{
    queryBioUML("web/ping", {},
    function(data) {
        var paths = data.values.refresh;
        var refreshPaths = {};
        for(var i = 0; i < paths.length; i++)
            refreshPaths[getElementPath(paths[i])] = 1;
        for(var path in refreshPaths)
            refreshTreeBranch(path);
        ping();
    }, function(){
        setTimeout(ping, 10000);
    });
}

/**
 * AJAX query to BioUML server
 *
 * @param queryID -
 *            string identificating query to watch for it (can be empty)
 * @param servletPath -
 *            path to servlet inside BioUML server (e.g. "web/bean")
 * @param parameters -
 *            parameters object (e.g. {action: "get", de: "databases/BioPath"})
 * @param success -
 *            function to be called upon successful query. Data-object will be
 *            passed. If success is not specified, call will be synchronized and
 *            result will be returned.
 * @param failure -
 *            function to be called upon failure (optional; if not specified,
 *            error box will be displayed). Data-object will be passed.
 */
function queryBioUMLWatched(queryID, servletPath, parameters, success, failure)
{
    if(failure == undefined)
    {
        failure = function(data)
        {
            logger.error(data.message);
        };
    }
    var result;
    
    var removeXHR = function(queryID, xhr)
    {
        var queries = activeQueries[queryID];
        if(queries == undefined) return;
        for(var i=0; i<queries.length; i++)
        {
            if(queries[i] == xhr)
            {
                queries.splice(i,1);
                break;
            }
        }
        if(queries.length == 0)
        {
            delete activeQueries[queryID];
        }
    };
    
    var xhr = $.ajax(
    {
        type: 'POST',
        url: appInfo.serverPath+servletPath,
        data: parameters,
        contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
        success: function(data, status, xhr)
        {
            removeXHR(queryID, xhr);
            if (xhr.status == 0)
            {
                if (xhr.aborted)
                    return null;
                else
                {
                    if (data == undefined) 
                        data = {
                            type: QUERY_TYPE_REQUEST_FAILED,
                            message: resources.commonErrorCannotConnect
                        };
                    failure(data);
                    result = data;
                }
            }
            else if (data && (data.type == 0 || data.type == 'ok' || data.type == 2)) 
            {
                if(success)
                {
                    success(data);
                } else
                {
                    result = data;
                }
            }
            else if(data && data.type == 3)
            {
                if(!invalidSession)
                {
                    invalidSession = true;
                    var invalidSessionHandler = function()
                    {
                        var user = new User();
                        user.reinit();
                    };
                    var unsaved = getNotSavedDocumentNames();
                    var dialogDiv = $('<div title="' + getCustomizedMessage( "commonSessionExpiredTitle" ) + '"></div>');
                    var message = unsaved.length == 0 ?
                              getCustomizedMessage( "commonErrorSessionExpiredNoUnsaved" ) :
                              getCustomizedMessage( "commonErrorSessionExpiredUnsaved" ).replace("{documents}", unsaved.join("<br>"));
                    dialogDiv.html("<p>" + message + "</p>");
                    dialogDiv.dialog(
                    {
                        autoOpen: false,
                        width: 500,
                        modal: true,
                        buttons: perspective.closeOnlyOnSessionExpire ? {} : { "Ok": invalidSessionHandler },
                        close: invalidSessionHandler
                    });
                    dialogDiv.dialog("open");
                    if( perspective.closeOnlyOnSessionExpire )
                    {
                        $( '.ui-dialog-titlebar-close' ).css( "display", "none" );
                    } 
                    else
                    {
                        dialogDiv.parent().find(":button:contains('Ok')").focus();
                    }
                }
            } 
            else
            {
                if(data == undefined) data = {type: QUERY_TYPE_UNREPORTED_ERROR, message: resources.commonErrorNoData};
                if(data.code && (data.code.match("^EX_INTERNAL_")))
                {
                    data.message = resources.commonErrorInternalError.replace("{message}", data.message);
                } else if(data.code && (data.code.match("^EX_QUERY_PARAM_")))
                {
                    data.message = resources.commonErrorInvalidQuery.replace("{message}", data.message);
                }
                failure(data);
                result = data;
            }
        },
        error: function(xhr, status, errorThrown)
        {
            if(status === "abort") return;
            var data = {};
            data.type = QUERY_TYPE_REQUEST_FAILED;
            if(errorThrown && errorThrown.result == 0x80004005)
                data.message = resources.commonErrorCannotConnect; 
            else
                data.message = resources.commonErrorQueryException.replace("{message}",
                        errorThrown == undefined?
                                status+(xhr.status>=500?" "+xhr.status+" "+xhr.statusText:""):
                                    (errorThrown.message == undefined?errorThrown.toString():errorThrown.message));
            failure(data);
            result = data;
        },
        dataType: 'json',
        async: !!success
    });
    if(queryID)
    {
        if(activeQueries[queryID])
        {
            activeQueries[queryID].push(xhr);
        } else
        {
            activeQueries[queryID] = [xhr];
        }
    }
    return result;
}

function abortQueries(queryID)
{
    var queries = activeQueries[queryID];
    if(queries == undefined) return;
    for (var i = 0; i < queries.length; i++) 
    {
        queries[i].aborted = true;
        queries[i].abort();
    }    
    delete activeQueries[queryID];
}

function queryBioUML(servletPath, parameters, success, failure)
{
    return queryBioUMLWatched("", servletPath, parameters, success, failure);
}

/**
 * AJAX query to BioUML server
 * 
 * @param servletPath -
 *            path to servlet inside BioUML server (e.g. "web/bean")
 * @param parameters -
 *            parameters object (e.g. {action: "get", de: "databases/BioPath"})
 * @param success -
 *            function to be called upon successful query. Data-object will be
 *            passed.
 * @param failure -
 *            function to be called upon failure. Data-object will be passed.
 * @param callback -
 *            function to be called after either success or failure. If not
 *            specified, call will be synchronous
 */
function queryBioUMLWithCallback(servletPath, parameters, success, failure, callback)
{
    if(callback != undefined)
        queryBioUML(servletPath, parameters, function() {callback(success());}, function() {callback(failure());});
    else
    {
        data = queryBioUML(servletPath, parameters, undefined, function(){});
        if(data.type == QUERY_TYPE_SUCCESS)
        {
            return success(data);
        } else
            return failure(data);
    }
}

/**
 * Query BioUML service
 * @param service service name (like "access.service")
 * @param command command number (like 29)
 * @param options object containing options which will be passed to the service
 * @param success function to be called upon successful query. May be omitted. Data-object will be passed.
 * @param failure function to be called upon failure. Data-object will be passed.
 * @returns
 */
function queryService(service, command, options, success, failure)
{
    var parameters = 
    {
        command: command,
        service: service
    };
    if(options)
    {
        for(var i in options)
        {
            parameters[i] = options[i];
        }
    }
    return queryBioUML("web/data", parameters, success, failure);
}

function queryBean(beanPath, options, success, failure)
{
    var parameters = 
    {
            showMode: SHOW_USUAL,
            useCache: "yes",
            ignoreErrors: false
    };
    for(optionName in options)
    {
        if(parameters[optionName] != undefined)
            parameters[optionName] = options[optionName];
    }
    if (failure == undefined) 
    {
        failure = parameters.ignoreErrors?function(){}:undefined;
    }
    return queryBioUML("web/bean/get",
    {
        de: beanPath,
        showMode: parameters.showMode,
        useCache: parameters.useCache
    }, success, failure);
}
