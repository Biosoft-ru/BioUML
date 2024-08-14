var errorMessage = "Element cannot be saved in current location.";
var errorMessageFull = errorMessage + " Server reported the following message:<br>{message}<br>";
var errorMessageFolder = "Folder cannot be copied to the selected location.";
var errorMessageFullFolder = errorMessage + " Server reported the following message:<br>{message}<br>";

function copyElement(completePath)
{
    var path = completePath + " copy";

    var tpath = completePath;
    var pli = tpath.lastIndexOf( "." );
    if( pli > 0 )
    { 
        var befp = tpath.substring( 0, pli );
        var ext = tpath.substring( pli + 1 );
        path = befp + " copy." +  ext;
    }

    createSaveElementDialog("Copy element", 
           getElementClass(completePath),
           path,
           function(newPath)
           {
               showWaitDialog("Copying in progress...");
               queryBioUML("web/doc/save", { de: completePath, newPath: newPath }, function(data)
               {
                   removeWaitDialog();
                   refreshTreeBranch(getElementPath(newPath));
                   logger.message("Element saved.");
               }, function(data)
               {
                   removeWaitDialog();
                   logger.message(data.message?errorMessageFull.replace("{message}", data.message):errorMessage);
               });
           });
}

function copyFolder(completePath)
{
    var path = completePath + " copy";
    createSaveElementDialog( "Copy folder",
    		getElementClass(completePath),
    		path,
    		function(newPath)
    		{
    			var jobID = rnd();
    			queryBioUML("web/folder/copy", { path : completePath, newPath : newPath, jobID : jobID }, function(data) {
    				showProgressDialog(jobID, "Folder copying", function() {
    					refreshTreeBranch(getElementPath(newPath));
       					logger.message("Folder saved.");
       					queryBioUML("web/folder/clearTask", {jobID : jobID});
    				});
				}, function(data) {
					logger.message(data.message ? errorMessageFullFolder.replace(
							"{message}", data.message) : errorMessageFolder);
				});
			});
}
