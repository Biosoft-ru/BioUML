function gitClone(path, callback)
{
    var dialogDiv = $('<div title="Clone Git repository"></div>');

    var form = $(
    '<table> \
     <tr><td>Repository:</td><td><input size="65" id="git_repository"/></td></tr> \
     <tr><td>Username:</td><td><input size="25" id="git_username"/></td></tr> \
     <tr><td>Password:</td><td><input type="password" size="25" id="git_password"/></td></tr> \
     </table>');

    dialogDiv.append( form );

    var progress = $('<textarea style="width:100%; background-color:#F8F8F8" rows="10"></textarea>').attr("readonly", "readonly");

    dialogDiv.append('Server output:<br />');
    dialogDiv.append(progress);

    var projectName = null;
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 500,
        modal: true,
        buttons: 
        {
            "Ok": function()
            {
                var urlInput = $('#git_repository');

                if( !urlInput.val() )
                { 
                    logger.error( "Please fill 'Repository' field!" );
                    return;
                }

                var userInput = $('#git_username');
                var passInput = $('#git_password');

                var waitButton = git_private_hackButtons(); 

                queryBioUML("web/git/clone",
                {
                    de: path,
                    repository: urlInput.val(),
                    username: userInput.val(),
                    password: passInput.val()
                },
                function(data)
                {
                    console.log( data );
                    if( waitButton )
                    {
                       waitButton.hide();
                    }
                    progress.val( data.values[ 1 ] );
                    projectName = data.values[ 0 ];
                });
            },
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
                if( projectName )
                {
                    if(callback)
                    {
                        callback(projectName);
                    }
                    else
                    {
                        refreshTreeBranch(appInfo.userProjectsPath);
                        initProjectsTable();
                        initJournals( function() {
                            setCurrentProject(projectName);
                        });
                    }
                }
            }
        }
    });

    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    dialogDiv.dialog("open");
}

function gitPull(path, callback)
{
    var dialogDiv = $('<div title="Pull from Git repository"></div>');

    var progress = $('<textarea style="width:98%; background-color:#F8F8F8" rows="10"></textarea>').attr("readonly", "readonly");

    dialogDiv.append('Server output:<br />');
    dialogDiv.append(progress);

    var projectName = null;
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 500,
        modal: true,
        buttons: 
        {
            "Ok": function()
            {
                var waitButton = git_private_hackButtons(); 

                queryBioUML("web/git/pull",
                {
                    de: path
                },
                function(data)
                {
                    console.log( data );
                    if( waitButton )
                    {
                       waitButton.hide();
                    }
                    progress.val( data.values[ 1 ] );
                    projectName = data.values[ 0 ];
                });
            },
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();

                if( projectName )
                {
                    if(callback)
                    {
                        callback(projectName);
                    }
                    else
                    {
                        refreshTreeBranch( path + "/Data" );
                        openBranch( path + "/Data" );
                    }
                } 
            }
        }
    });

    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    dialogDiv.dialog("open");
}

function gitCommit(path, callback)
{
    var dialogDiv = $('<div title="Commit to Git repository"></div>');

    var msgInput = $('<input></input>').attr("size", 67);
    var msgControl = createSinglePropertyControl( "Message", msgInput );

    var progress = $('<textarea style="width:100%; background-color:#F8F8F8" rows="10"></textarea>').attr("readonly", "readonly");

    dialogDiv.append(msgControl);
    dialogDiv.append('Server output:<br />');
    dialogDiv.append(progress);

    var projectName = null;
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 500,
        modal: true,
        buttons: 
        {
            "Ok": function()
            {
                if( !msgInput.val() )
                { 
                    logger.error( "Please fill 'Message' field!" );
                    return;
                }

                var waitButton = git_private_hackButtons(); 

                queryBioUML("web/git/commit",
                {
                    de: path,
                    message: msgInput.val()
                },
                function(data)
                {
                    console.log( data );
                    if( waitButton )
                    {
                       waitButton.hide();
                    }
                    progress.val( data.values[ 1 ] );
                    projectName = data.values[ 0 ];
                });
            },
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();

                if( projectName )
                {
                    if(callback)
                    {
                        callback(projectName);
                    }
                } 
            }
        }
    });

    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    dialogDiv.dialog("open");
}

function gitPush(path, callback)
{
    var dialogDiv = $('<div title="Push to Git repository"></div>');

    var progress = $('<textarea style="width:98%; background-color:#F8F8F8" rows="10"></textarea>').attr("readonly", "readonly");

    dialogDiv.append('Server output:<br />');
    dialogDiv.append(progress);

    var projectName = null;
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 500,
        modal: true,
        buttons: 
        {
            "Ok": function()
            {
                var waitButton = git_private_hackButtons(); 

                queryBioUML("web/git/push",
                {
                    de: path
                },
                function(data)
                {
                    console.log( data );
                    if( waitButton )
                    {
                       waitButton.hide();
                    }
                    progress.val( data.values[ 1 ] );
                    projectName = data.values[ 0 ];
                });
            },
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();

                if( projectName )
                {
                    if(callback)
                    {
                        callback(projectName);
                    }
                } 
            }
        }
    });

    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    dialogDiv.dialog("open");
}


function git_private_hackButtons()
{
    var buttons = $("button");
    var waitButton = null; 
    for( var i = 0; i < buttons.length; i++ )
    {
        var b = $(buttons[ i ]);
        if( b.text() == "Ok" )
        {
            waitButton = b; 
            b.button( "disable" ).text( "Wait..." );    
        }
        else
        {
            b.text( "Close" );
        }
    }
    return waitButton;
}