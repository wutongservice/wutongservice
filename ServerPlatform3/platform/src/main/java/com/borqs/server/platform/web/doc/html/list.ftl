<body>
<#list docs.groupNames as groupName>
<h3>${docs.getDisplayGroupName(groupName)}</h3>
    <#list docs.getGroupRoutes(groupName) as route>
    <a href="${docs.getDocByRoute(route).getRouteHtmlFile()}" target="routeFrame">${route}</a><br/>
    </#list>
</#list>
</body>