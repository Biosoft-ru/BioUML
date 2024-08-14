/*
 * User class. Supports log-in.
 */

var isAnonymousSession = false;

function User()
{
    this.showLoginForm = function(message)
    {
        if(appInfo.externalLogin) {
            var params = {};
            for(var key in paramHash) {
                if(key !== 'user' && key !== 'pass') {
                    params[key] = paramHash[key];
                }
            }
            window.location = appInfo.serverPath+'map?'+toURI({login: 1, params: toURI(params)});
            return;
        } else if(appInfo.loginPage) {
            window.location.replace(appInfo.loginPage + window.location.hash);
            return;
        } 
        this.dialogDiv = $('<div title="'+resources.dlgLoginTitle+'"></div>');
        this.loginLabel = $('<p/>').text(resources.dlgLoginPrompt);
        if(!message) message = checkBrowserSupport();
        if(message)
            this.loginLabel.prepend($('<p><font color=red>' + message + '</font></p>'));
        var loginForm = $('<table>'
                +(appInfo.serverAddress?'<tr><td>'+resources.dlgLoginServer+'</td><td><b>'+appInfo.serverAddress+'</b></td>':'')
                +'<tr><td>'+resources.dlgLoginPlatform+'</td><td><b style=\"white-space: nowrap\">'+appInfo.name+' '+appInfo.edition+' '+appInfo.version+'</b>'
                +'<tr><td colspan="2" id="prompt" style="padding-top: 1em;"></td>'
                +'<tr><td>'+resources.dlgLoginUsername+' </td><td><input type="text" size="25" id="login_username"/></td></tr><tr><td>'+
                resources.dlgLoginPassword+' </td><td><input type="password" size="25" id="login_password"/></td></tr></table>');
        loginForm.find("#prompt").append(this.loginLabel);
        this.dialogDiv.append(loginForm);
        if( appInfo.biostoreForgotPasswordLink )
        {
        	var forgotPassword = $('<p><a target="_blank" href="' + appInfo.biostoreForgotPasswordLink + '">' + resources.dlgLoginForgotPassword + '</a></p>');
        	this.dialogDiv.append( forgotPassword );
        }
        if(appInfo.loginFormExtraMessage)
        {
            var extraMessage = $('<p></p>');
            extraMessage.html(appInfo.loginFormExtraMessage);
            this.dialogDiv.append( extraMessage );
        }
            
        var _this = this;
        var buttons = [];
        buttons.push({
            text : resources.dlgLoginButtonLogin,
            click   : function(){
                _this.submitLoginForm(true);
            }
        });
        if(!appInfo.disableAnonymous)
        {
            buttons.push({
                text : resources.dlgLoginButtonAnonymous,
                click   : function(){
                    _this.submitLoginForm(false);
                }
            });
        }
        if(appInfo.biostoreRegisterLink)
        {
            buttons.push({
                text : resources.dlgLoginButtonRegister,
                click   : function(){
                    window.open(appInfo.biostoreRegisterLink);
                }
            });
        }

        this.dialogDiv.dialog(
        {
            autoOpen: false,
            modal: true,
            resizable: false,
            width: 350,
            buttons: buttons, 
            close: function(ev, ui)
            {
                $(this).remove();
            }
        });
        this.dialogDiv.dialog("open").css('overflow', 'hidden');
        var userName = Cookies.getItem("last_user_name");
        if(userName)
        {
            $('#login_username').val(userName);
            $('#login_password').focus();
        } else
            $('#login_username').focus();
        this.dialogDiv.keyup(function(event)
        {
            if (event.keyCode == 13) 
            {
                _this.submitLoginForm(appInfo.disableAnonymous || $('#login_username').val()!="");
            }
        });
    };
    
    this.login = function(user, pass, nonAnonymous)
    {
        var _this = this;
        showWaitDialog(resources.commonAuth);
        if(nonAnonymous && user == "")
        { 
            user = " ";
        }
        if(!nonAnonymous)
        {
            user = "";
            pass = "";
        }
        var perspective = paramHash.perspective;
        if(!perspective)
        {  
           perspective = appInfo.perspective;
        } 

        var loginData = {
            username: user,
            password: pass,
            perspective: perspective
        };

        if( paramHash.sessionId )
        {
            loginData.sessionId = paramHash.sessionId;
        }

        queryBioUML("web/login", loginData, 
        function(data)
        {
            try
            {
                // login with 'sessionId' 
                //console.log( "login" );  
                //console.log( data );  
                if( user == " " && data.values.username )
                {
                    user = data.values.username;
                    delete data.values.username;
                }

                currentUser = user;
                if(_this.dialogDiv)
                {
                    _this.dialogDiv.dialog("close");
                    _this.dialogDiv.remove();
                }
                Cookies.setItem("last_user_name", currentUser);
                isAnonymousSession = !nonAnonymous;
                loadApplication(data.values);
            }
            finally
            {
                removeWaitDialog();
            }
        }, function(data)
        {
            removeWaitDialog();
            if(_this.loginLabel)
            {
            	if( data.message != '' && !data.message.endsWith('.') )
            		data.message = data.message + '.';
                _this.loginLabel.html('<p><font color=red>' + data.message + '</font><br> Please try to login again:</p>');
                $('#login_username').removeAttr("disabled");
                $('#login_password').removeAttr("disabled");
                $('#login_password').val("");
            } else
            {
                _this.showLoginForm(data.message);
            }
        });
        
        if(chatPreferences)
        {
            //save preferences for chat
            chatPreferences.username = user;
            chatPreferences.password = pass;
        }    
    };

    this.loginWithCookie = function(errorCallbackMsg)
    {
        var _this = this;
        showWaitDialog(resources.commonAuth);
        queryBioUML("web/login", 
        {
        }, function(data)
        {
            try
            {
                if(_this.dialogDiv)
                {
                    _this.dialogDiv.dialog("close");
                    _this.dialogDiv.remove();
                }
                var user = data.values.username;
                if(user)
                {
                    currentUser = user;
                    Cookies.setItem("last_user_name", user);
                    delete data.values.username;
                    isAnonymousSession = false;
                }
                else
                    isAnonymousSession = true;
                loadApplication(data.values);
            }
            finally
            {
                removeWaitDialog();
            }
        }, function(data)
        {
            removeWaitDialog();
            if(_this.loginLabel)
            {
            	if( data.message != '' && !data.message.endsWith('.') )
            		data.message = data.message + '.';
                _this.loginLabel.html('<p><font color=red>' + data.message + '</font><br> Please try to login again:</p>');
                $('#login_username').removeAttr("disabled");
                $('#login_password').removeAttr("disabled");
                $('#login_password').val("");
            } else
            {
                if( errorCallbackMsg )
                    _this.showLoginForm(errorCallbackMsg);
                else
                _this.showLoginForm("");
            }
        });
    };

    this.submitLoginForm = function(nonAnonymous)
    {
        var user = $('#login_username').val();
        var pass = $('#login_password').val();
        $('#login_username').attr("disabled", "disabled");
        $('#login_password').attr("disabled", "disabled");
        this.login(user, pass, nonAnonymous);
    };
    
    this.logout = function(force)
    {
        closeChatSession();//end chat connection
        var logoutFunc = function()
        {
            queryBioUML("web/logout", {}, function(data)
            {
                currentUser = null;
                if(appInfo.externalLogin)
                {
                    window.location.replace(appInfo.serverPath+'map?'+toURI({logout: 1}));
                } if(appInfo.loginPage) {
                    window.location.replace(appInfo.loginPage);
                } else{
                    window.location.reload();
                }
            }, function() {});
        };
        if(force) logoutFunc();
        else closeAllTabs(logoutFunc);
    };
    
    this.reinit = function()
    {
        if(appInfo.externalLogin)
        {
            closeChatSession();//end chat connection
            var params = {};
            var addParams = paramHash;
            if(currentHashStr)
            {
                addParams = parseURL(currentHashStr);
            }
            for(var key in addParams) {
                if(key !== 'user' && key !== 'pass') {
                    params[key] = addParams[key];
                }
            }
            window.location.replace(appInfo.serverPath+'map?'+toURI({reinit: 1, params: toURI(params)}));
        } else
        {
            this.logout(true);
        }
    };
}
