/* $Id: messageBundle.js,v 1.100 2013/08/23 04:08:42 lan Exp $ */
var resourceVars = {
    adminMail: "<a href='mailto:"+appInfo.adminMail+"?subject=Problem with "+appInfo.name+" "+appInfo.version+" ("+appInfo.serverAddress+")'>"+appInfo.adminMail+"</a>", 
    end: ""
};

function getCustomizedMessage( msgKey )
{
    var perspectiveMessage = perspective.messageBundle ? perspective.messageBundle[ msgKey ] : null;
    return perspectiveMessage || resources[ msgKey ];
}