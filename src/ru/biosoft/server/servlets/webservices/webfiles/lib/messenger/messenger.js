// Global chat preferences
var serverName = 'ip-10-227-177-28';
var chatPreferences = {
	httpBase: appInfo.serverPath+"jabber/",
	serverName: serverName
};
if(appInfo.jabberServer)
{
	chatPreferences.httpBase = appInfo.jabberServer;
}

var CONFERENCE_SERVER_PREFIX = 'conference.';

// MessageConnector object
var messageConnector;
//
var messageTreeIconsProcessor;

/*
 * Convert string to correct JID  
 */
function toJIDString(str)
{
	var result = str.toLowerCase();
	var toEscape = "\\ \"&'/:<>@";
	for (var i=0; i<toEscape.length; i++)
	{
		var re = new RegExp("\\"+toEscape.charAt(i), "g");
		result = result.replace(re, "\\"+toEscape.charCodeAt(i).toString(16));
	}
	return result;
}

function getUserByJID(jid)
{
	var node = jid.split("@")[0];
	var re = /^\\(\d{2})$/;
	for (var i=0; i<node.length-2; i++)
	{
		var match = re.exec(node.substring(i,i+3));
		if(match)
		{
			node=node.substring(0,i)+String.fromCharCode(parseInt(match[1], 16))+node.substring(i+3);
		}
	}
	return node;
}

/*
 * Start chat session if it is possible to current user
 */
function startChatSession()
{
	if(chatPreferences.username && (chatPreferences.username !=""))
	{
		var jid = toJIDString(chatPreferences.username);
		messageConnector = new MessageConnector(chatPreferences.httpBase, chatPreferences.serverName);
		messageConnector.login(jid, chatPreferences.password);
		
		messageTreeIconsProcessor = new TreeIconsProcessor();
		messageConnector.commonListener = messageTreeIconsProcessor;
	}
}

/*
 * Close chat connection
 */
function closeChatSession()
{
	if(messageConnector)
	{
		messageConnector.quit();
	}
}

/*
 * Open new chat dialog with user or room
 * Called by files/actions/tree/openChat.json
 *       and files/actions/tree/openGroupChat.json
 */
function openChatDialog(target, domain)
{
	if(messageConnector)
	{
		var jid = toJIDString(target)+'@'+domain;
		if($('#message_pane_'+jid).length > 0)
		{
			//message dialog already opened
			//TODO: focus on opened dialog
		}
		else
		{
			if(domain)
			{
				if(domain.indexOf(CONFERENCE_SERVER_PREFIX) == 0)
				{
					var groupDialog = new MessageGroupDialog(messageConnector, jid);
					groupDialog.open();
				}
				else
				{
					var chatDialog = new MessageDialog(messageConnector, jid);
					chatDialog.open();
				}
			}
			
			//hide notifications
			$('#notification').fadeOut('slow');
			if(messageTreeIconsProcessor)
				messageTreeIconsProcessor.resetIcon(jid);
		}
	}
	else
	{
		logger.message("You are not able to use messenger");
	}
}

/*
 * Jabber connector
 */
function MessageConnector(httpBase, serverName)
{
    this.httpBase = httpBase;
    this.serverName = serverName;
    
    /*
     * Map of available user listeners (key - username to chat with, value - user listener)
     * listener methods:
     * 	- messageReceived(messageItem) - new message was received from user
     *  - statusChanged(status, userID) - user status was changed
     *  - connected(isConnected) - indicates when connection is alive
     */
    this.userMap = [];
    
    /*
     * Listener for common chat events
     * listener methods:
     * 	- newChatStarted(user) - new message received from user without registered listener
     */
    this.commonListener;
    
    this.statusBuffer = [];
    this.messageBuffer = [];
    
    var _this = this;
    
    this.addListener = function(userID, listener)
    {	
    	_this.userMap[userID] = listener;
    	
    	//process status from buffer
    	var status = _this.statusBuffer[userID];
    	if(status)
    	{
    		if(listener.statusChanged)
    			listener.statusChanged(status, null);
    	}
    	
    	//process offline messages
    	var messageList = _this.messageBuffer[userID];
    	if(messageList)
    	{
    		for(i in messageList)
    		{
    			if(listener.messageReceived)
    				listener.messageReceived(messageList[i]);
    		}
    		delete _this.messageBuffer[userID];
    	}
    };
    
    this.removeListener = function(userID)
    {
    	delete _this.userMap[userID];
    };
    
    this.login = function(username, password) 
    {
    	this.username = username;
  		try 
  		{
    		// setup args for contructor
    		var oArgs = new Object();
    		oArgs.httpbase = this.httpBase;
    		oArgs.timerval = 2000;
    		
      		this.con = new JSJaCHttpBindingConnection(oArgs);    
    		this.setupCon(this.con);

    		// setup args for connect method
    		oArgs = new Object();
    		oArgs.domain = this.serverName;
    		oArgs.username = username;
    		oArgs.resource = 'bioumlweb(jsjac)-'+rnd();
    		oArgs.pass = password;
    		oArgs.register = false;
    		oArgs.authtype = 'nonsasl';
    		this.con.connect(oArgs);
  		} 
  		catch (e) 
  		{
  			logger.message(e.toString());
  		} 
  		finally 
  		{
    		return false;
  		}
	};
	
	//send message to selected user
	this.sendMsg = function(targetID, msg, type) 
	{
  		try 
  		{
    		var aMsg = new JSJaCMessage();
    		aMsg.setTo(new JSJaCJID(targetID));
    		if(type)
    		{
    			aMsg.setType(type);
    		}
    		aMsg.setBody(msg);
    		_this.con.send(aMsg);
    	} 
  		catch (e) 
  		{
  			logger.error(e.message);
  		}
	};
	
	//disconnect from the server
    this.quit = function() 
    {
    	var p = new JSJaCPresence();
  		p.setType("unavailable");
  		this.con.send(p);
  		this.con.disconnect();
  		for(listener in _this.userMap)
  		{
  			if(_this.userMap[listener].connected)
  				_this.userMap[listener].connected(false);
  		}
  	};
  	
  	//chat room functions
	this.createRoom = function(roomName)
	{
		try 
  		{
			var packet = new JSJaCPresence();
			packet.setTo(roomName+'@'+CONFERENCE_SERVER_PREFIX+_this.serverName+'/'+_this.username);
			var xNode = packet.appendNode('x', {xmlns: NS_MUC});
			xNode.appendChild(packet.buildNode('password', {xmlns: ''},chatPreferences.password));
    		_this.con.send(packet);
    		
    		//send default room settings submit query
    		var iq = new JSJaCIQ();
            iq.setType('set');
            iq.setTo(roomName+'@'+CONFERENCE_SERVER_PREFIX+_this.serverName);
            var query = iq.setQuery(NS_MUC_OWNER);
            query.appendChild(iq.buildNode('x', {'xmlns': NS_XDATA, 'type': 'submit'}));
            _this.con.send(iq);
    	} 
  		catch (e) 
  		{
  			logger.error(e.message);
  		}
	};
	this.inviteUser = function(roomName, targetUser)
	{
		try 
  		{
    		var aMsg = new JSJaCMessage();
    		aMsg.setTo(roomName+'@'+CONFERENCE_SERVER_PREFIX+_this.serverName);
    		var xNode = aMsg.appendNode('x', {xmlns: NS_MUC_USER});
    		xNode.appendChild(aMsg.buildNode('invite', {xmlns: '', to: targetUser+'@'+_this.serverName},''));
    		_this.con.send(aMsg);
    	} 
  		catch (e) 
  		{
  			logger.error(e.message);
  		}
	};
	
	//init connection handlers
	this.setupCon = function(con) 
	{
    	con.registerHandler('message',this.handleMessage);
    	con.registerHandler('presence',this.handlePresence);
    	con.registerHandler('iq',this.handleIQ);
    	con.registerHandler('onconnect',this.handleConnected);
    	con.registerHandler('onerror',this.handleError);
    	con.registerHandler('status_changed',this.handleStatusChanged);
    	con.registerHandler('ondisconnect',this.handleDisconnected);

    	con.registerIQGet('query', NS_VERSION, this.handleIqVersion);
    	con.registerIQGet('query', NS_TIME, this.handleIqTime);
    };
    
	//
	// handlers
	//
	
	this.handleIQ = function(aIQ) 
	{
		_this.con.send(aIQ.errorReply(ERR_FEATURE_NOT_IMPLEMENTED));
	};

	this.handleMessage = function(aJSJaCPacket) 
	{
		var jid = aJSJaCPacket.getFromJID();
		var userID = jid.getNode()+'@'+jid.getDomain();
		var userName = getUserByJID(userID);
		var xRoom = aJSJaCPacket.getChild('x', NS_MUC_USER);
		if(xRoom)
		{
			var xChild = xRoom.firstChild;
			if(xChild.nodeName == 'invite')
			{
				//accept invite
				var packet = new JSJaCPresence();
				packet.setTo(userID+'/'+_this.username);
				var xNode = packet.appendNode('x', {xmlns: NS_MUC});
				_this.con.send(packet);
			}
		}
		var msg = aJSJaCPacket.getBody().htmlEnc();
		if(jQuery.trim(msg).length > 0)
		{
			var messageItem = new MessageItem(msg, new Date(), userID, jid.getResource());
	    	var listener = _this.userMap[userID];
	    	if(listener)
	    	{	
	    		if(listener.messageReceived)
	    			listener.messageReceived(messageItem);
	    	}
	    	else
	    	{
	    		var messageList = _this.messageBuffer[userID];
	    		if(!messageList)
	    		{
	    			messageList = [];
	    			_this.messageBuffer[userID] = messageList;
	    		}
	    		messageList.push(messageItem);
	    		if(_this.commonListener)
	    		{
	    			_this.commonListener.newChatStarted(userID);
	    		}
	    	
	    		$('#notification').html('<span><img src="icons/new_msg.gif"/>&nbsp;New message from <b>'+userName+'</b></span>');
				$('#notification').fadeIn('slow');
				$("#notification").unbind("click");
				$('#notification').click(function()
				{
					openChatDialog(userName, jid.getDomain());
				});
	    	}
		}
	};

	this.handlePresence = function(aJSJaCPacket) 
	{
		var jid = aJSJaCPacket.getFromJID();
		var userID = jid.getNode()+'@'+jid.getDomain();
	    var statusMessage;
	    if (!aJSJaCPacket.getType() && !aJSJaCPacket.getShow()) 
		{
			statusMessage = 'online';
		}
		else if(aJSJaCPacket.getType() && aJSJaCPacket.getType()=='unavailable')
		{
			statusMessage = 'offline';
		}
  		else
  		{
  			statusMessage = '';
  			if (aJSJaCPacket.getType())
      			statusMessage += aJSJaCPacket.getType() + '.</b>';
    		else
      			statusMessage += aJSJaCPacket.getShow() + '.</b>';
    		if (aJSJaCPacket.getStatus())
      			statusMessage += ' ('+aJSJaCPacket.getStatus().htmlEnc()+')';
      	}
	    
	    var forUser = null;
	    var xRoom = aJSJaCPacket.getChild('x', NS_MUC_USER);
		if(xRoom)
		{
			for(var i=0;i<xRoom.childNodes.length;i++)
			{
				var xChild = xRoom.childNodes.item(i);
				if(xChild.nodeName == 'item')
				{
					var forUserObj = new JSJaCJID(xChild.getAttribute('jid'));
					forUser = forUserObj.getNode()+'@'+forUserObj.getDomain();
				}
			}
		}
  		
  		_this.statusBuffer[userID] = statusMessage;
  		var listener = _this.userMap[userID];
		if(listener)
		{
			if(listener.statusChanged)
				listener.statusChanged(statusMessage, forUser);
		}
  	};

	this.handleError = function(e) 
	{
		var code = e.getAttribute('code');
		if(code == 503)//do not display errors when jabber server is not available
			return;
		
		var errorMessage;
		if(code == 500)
		{
			errorMessage = resources.jabberErrorConnection;
		} else if(code == 401)
		{
			errorMessage = resources.jabberErrorAuthentication;
		} else
		{
			errorMessage = resources.jabberErrorCustom.replace("{message}", code+": "+e.firstChild.nodeName);
		}
    	logger.error(errorMessage);
    	
  		if (_this.con.connected())
    		_this.con.disconnect();
	};

	this.handleStatusChanged = function(status) 
	{
 	 	//nothing to do
	};

	this.handleConnected = function() 
	{
  		for(listener in _this.userMap)
  		{
  			if(_this.userMap[listener])
  				_this.userMap[listener].connected(true);
  		}
  		_this.con.send(new JSJaCPresence());
	};

	this.handleDisconnected = function() 
	{
  		for(listener in _this.userMap)
  		{
  			if(_this.userMap[listener])
  				_this.userMap[listener].connected(false);
  		}
	};

	this.handleIqVersion = function(iq) 
	{
  		_this.con.send(iq.reply([
        	iq.buildNode('name', 'bioumlweb(jsjac)'),
            iq.buildNode('version', JSJaC.Version),
            iq.buildNode('os', navigator.userAgent)
       	]));
  		return true;
	};

	this.handleIqTime = function(iq) 
	{
  		var now = new Date();
  		_this.con.send(iq.reply([
  			iq.buildNode('display', now.toLocaleString()), 
  			iq.buildNode('utc',now.jabberDate()),
  			iq.buildNode('tz',now.toLocaleString().substring(now.toLocaleString().lastIndexOf(' ')+1))
  		]));
  		return true;
	};
}

function MessageItem(msg, date, from, resource)
{
	this.msg = msg;//text message
	this.date = date;//Date object
	this.from = from;//from userID
	this.resource = resource;//necessary to identify group chat user
}

/*
 *	Chat dialog
 *	Uses MessageConnector to send messages, 
 *  implements MessageConnector listener to receive messages(see MessageConnector.userMap for details)
 */
function MessageDialog(messageConnector, target)
{
	this.messageConnector = messageConnector;
	this.target = target;
	
	var _this = this;
	
	this.open = function()
	{
		var dialogDiv = $('<div id="message_pane_'+this.target+'" title="'+getUserByJID(this.target)+'"></div>');
		_this.jabbedPane = $('<div id="jabber_pane">User status: <span id="jabber_status">offline</span><div id="jabber_messages"></div><form name="sendForm" action="#"><div><textarea id="jabber_input" rows="3" cols="80" tabindex="2"></textarea></div></form></div>');
    	dialogDiv.append(_this.jabbedPane);
    	dialogDiv.dialog(
    	{
    		autoOpen: false,
        	width: 500,
        	modal: false,
        	buttons: 
        	{
        		"Close": function()
        	    {
        	    	$(this).dialog("close");
       	        	$(this).remove();
            	},
            	"Send": function()
        	    {
        	        var msg = $(this).find('#jabber_input').val();
       				if(msg == '')
       					return;
       				_this.sendMessage(msg);
            	}
        	},
        	close: _this.quit
    	});
    	dialogDiv.dialog("open");
    	_this.jabbedPane.find('#jabber_input').keyup(function(event)
    	{
    	    if (event.keyCode == 13 && !event.ctrlKey) 
        	{
            	var msg = _this.jabbedPane.find('#jabber_input').val();
       			if(msg == '')
       				return;
       			msg = msg.substring(0, msg.length - 1);
       			_this.sendMessage(msg);
				return true;
        	}
    	});
    	
    	//add dialog as jabber connector listener
    	_this.messageConnector.addListener(_this.target, _this);
	};
	
	this.quit = function()
	{
		//remove dialog from jabber connector listener
		_this.messageConnector.removeListener(_this.target);
    };
	
	this.sendMessage = function(msg)
	{
		_this.messageConnector.sendMsg(_this.target, msg);
		_this.addMessage(new MessageItem(msg, new Date(), _this.messageConnector.username+'@'+_this.messageConnector.serverName), false);
		_this.jabbedPane.find('#jabber_input').val('');
	}
	
	this.addMessage = function(messageItem, isIncoming)
	{
	    var color;
	    if(isIncoming)
	    {
	    	color = 'red';
	    }
	    else
	    {
	    	color = 'blue'
	    }
	    var timeStr = messageItem.date.toTimeString();
	    timeStr = timeStr.substring(0, timeStr.indexOf(' '));
		var html = $('<div><b><font color="'+color+'">'+getUserByJID(messageItem.from)+'('+timeStr+'):</font></b><br/>'+messageItem.msg+'</div>');
		
		var magDiv = _this.jabbedPane.find('#jabber_messages');
		magDiv.append(html);
		magDiv.scrollTop(magDiv[0].scrollHeight);
	};
	
	//
	// MessageConnector listener implementation
	//
	
	this.messageReceived = function(messageItem)
	{
		_this.addMessage(messageItem, true);
	};
	
    this.statusChanged = function(status, userID)
    {
    	_this.jabbedPane.find('#jabber_status').html(status);
  	};
    
    this.connected = function(isConnected)
    {
    	if(isConnected)
		{
			_this.jabbedPane.find('input').attr('disabled', 'disabled'); 
			_this.jabbedPane.find('textarea').attr('disabled', 'disabled'); 
		}
		else
		{
			_this.jabbedPane.find('input').attr('disabled', ''); 
			_this.jabbedPane.find('textarea').attr('disabled', ''); 
		}
    };
}

/*
 *	Group chat dialog (chat room)
 *	Uses MessageConnector to send messages, 
 *  implements MessageConnector listener to receive messages(see MessageConnector.userMap for details)
 */
function MessageGroupDialog(messageConnector, target)
{
	this.messageConnector = messageConnector;
	this.target = target;
	this.colorMap = [];//map of colors for users
	
	var _this = this;
	
	this.open = function()
	{
		var groupName = _this.target.substring(0, _this.target.indexOf('@'));
		//TODO: if group exists just join to group
		_this.messageConnector.createRoom(groupName);
		
		var dialogDiv = $('<div id="message_pane_'+this.target+'" title="'+getUserByJID(this.target)+'"></div>');
		_this.jabbedPane = $('<div id="jabber_pane"><div id="jabber_messages_gr"></div><form name="sendForm" action="#"><div><textarea id="jabber_input_gr" rows="3" cols="80" tabindex="2"></textarea></div></form><div id="jabber_groupusers"></div></div>');
    	dialogDiv.append(_this.jabbedPane);
    	dialogDiv.dialog(
    	{
    		autoOpen: false,
        	width: 600,
        	modal: false,
        	buttons: 
        	{
        		"Close": function()
        	    {
        	    	$(this).dialog("close");
       	        	$(this).remove();
            	},
            	"Send": function()
        	    {
        	        var msg = $(this).find('#jabber_input_gr').val();
       				if(msg == '')
       					return;
       				_this.sendMessage(msg);
            	}
        	},
        	close: _this.quit
    	});
    	dialogDiv.dialog("open");
    	_this.jabbedPane.find('#jabber_input_gr').keyup(function(event)
    	{
    	    if (event.keyCode == 13 && !event.ctrlKey) 
        	{
            	var msg = _this.jabbedPane.find('#jabber_input_gr').val();
       			if(msg == '')
       				return;
       			msg = msg.substring(0, msg.length - 1);
       			_this.sendMessage(msg);
				return true;
        	}
    	});
    	
    	//add dialog as jabber connector listener
    	_this.messageConnector.addListener(_this.target, _this);
		
    	//invite all users in group
    	inviteUsersToJabberGroup(_this.messageConnector, groupName);
    };
	
	this.quit = function()
	{
		//remove dialog from jabber connector listener
		_this.messageConnector.removeListener(_this.target);
    };
    
    this.sendMessage = function(msg)
	{
		_this.messageConnector.sendMsg(_this.target, msg, 'groupchat');
		_this.jabbedPane.find('#jabber_input_gr').val('');
	}
	
	//
	// MessageConnector listener implementation
	//
	
	this.messageReceived = function(messageItem)
	{
		var timeStr = messageItem.date.toTimeString();
	    timeStr = timeStr.substring(0, timeStr.indexOf(' '));
	    if(messageItem.resource)
	    {
	    	var from = messageItem.resource+'@'+_this.messageConnector.serverName;
	    	var fromSpan = $('<span>'+getUserByJID(from)+'('+timeStr+'):</span>');
	    	fromSpan.css('color', _this.getUserColor(from)).css('font-weight', 'bold');
	    	var html = $('<div></div>');
	    	html.append(fromSpan).append('<br/>').append('<span>'+messageItem.msg+'</span>');
	    	var magDiv = _this.jabbedPane.find('#jabber_messages_gr');
			magDiv.append(html);
			magDiv.scrollTop(magDiv[0].scrollHeight);
	    }
	    else
	    {
	    	//TODO: this is service message from server
	    }
	};
	
    this.statusChanged = function(status, userID)
    {
    	if(userID)
    	{
    		var userName = getUserByJID(userID);
    		var iserDivId = userName.replace(/\W/g,"_");
    		var userDiv = _this.jabbedPane.find('#jabber_groupusers').find('#'+iserDivId);
    		if((status == 'online') && (userDiv.size() == 0))
    		{
    			var userRecordDiv = $('<div id="'+iserDivId+'">'+userName+'</div>');
    			userRecordDiv.css('color', _this.getUserColor(userID)).css('font-weight', 'bold').css('padding','5px');
    			_this.jabbedPane.find('#jabber_groupusers').append(userRecordDiv);
    		}
    		else if((status == 'offline') && (userDiv.size() > 0 ))
    		{
    			userDiv.remove();
    		}
    	}
  	};
    
    this.connected = function(isConnected)
    {
    	if(isConnected)
		{
			_this.jabbedPane.find('input').attr('disabled', 'disabled'); 
			_this.jabbedPane.find('textarea').attr('disabled', 'disabled'); 
		}
		else
		{
			_this.jabbedPane.find('input').attr('disabled', ''); 
			_this.jabbedPane.find('textarea').attr('disabled', ''); 
		}
    };
    
    //
	// Utility functions
	//
    
    this.getUserColor = function(userID)
    {
    	var color = _this.colorMap[userID];
    	if(!color)
    	{
    		var r = Math.ceil( Math.random() * 255 );
    		var g = Math.ceil( Math.random() * 255 );
    		var b = Math.ceil( Math.random() * 255 );
    		var delta = Math.ceil(((r+g+b)-255)/3);
    		if(r > delta) r = r - delta; else r = 0; 
    		if(g > delta) g = g - delta; else g = 0; 
    		if(b > delta) b = b - delta; else b = 0; 
    		color = 'rgb('+r+','+g+','+b+')';
    		_this.colorMap[userID] = color;
    	}
    	return color;
    };
}

function inviteUsersToJabberGroup(messageConnector, groupName)
{
	getDataCollection('users').getNameList(function(nameList)
	{
		if (nameList != null) 
	    {
			for (nli = 0; nli < nameList.length; nli++) 
	        {
				if(toJIDString(nameList[nli].name) == groupName)
				{
					var groupDC = getDataCollection('users/'+nameList[nli].name);
					groupDC.getNameList(function(nameList)
					{
						if (nameList != null) 
					    {
							for (nli = 0; nli < nameList.length; nli++) 
					        {
								var userJID = toJIDString(nameList[nli].name);
								if(userJID != messageConnector.username)
								{
									messageConnector.inviteUser(groupName, userJID);
								}
					        }
					    }
					});
				}
			}
		}
	});
}

function TreeIconsProcessor()
{
	var _this = this;
	
	//special status icons for users
	this.userIcons = [];
	
	/*
	 *	Listener for message connector
	 */
	this.newChatStarted = function(userID)
	{
		_this.userIcons[userID] = 'icons/new_msg.gif';
		_this.forAllUsers(userID, function(path, link)
		{
			var user = new DataCollection(path);
			link.style.backgroundImage = "url('"+_this.userIcons[userID]+"')";
		});
	};
	
	this.resetIcon = function(userID)
	{
		delete _this.userIcons[userID];
		_this.forAllUsers(userID, function(path, link)
		{
			link.style.backgroundImage = getNodeIcon(getDataCollection(getElementPath(path)), getElementName(path));
		});
	}
	
	this.forAllUsers = function(userID, callback)
	{
		var username = userID.substring(0, userID.indexOf('@'));
		var parent = document.getElementById("#rt_users");
		$(parent).children('ul').children('li').each(function() 
		{
			$(this).children('ul').children('li').each(function()
			{
				if(toJIDString($(this).children('a').html())==username)
				{
					callback(getTreeNodePath($(this).get(0)), $(this).children('a').get(0));
				}
			});
		});
	}
}
