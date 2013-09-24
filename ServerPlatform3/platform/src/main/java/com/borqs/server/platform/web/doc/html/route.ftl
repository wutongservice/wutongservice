<body>

<table width="100%">
    <tr>
        <td>
            <h2>
            <#list doc.displayTitles as title>
            ${title?html}<br/>
            </#list>
            </h2>
        </td>
        <td width="20%">
            <h3>Group: ${doc.displayGroupName}</h3>
        </td>
    </tr>
</table>
<hr/>

<p>${doc.displayDescription}</p>
<#if doc.displayRemark != "">
<h4>Remark</h4>
<p>${doc.displayRemark}</p>
</#if>
<p>Need Login: ${doc.displayNeedLogin}</p>
<hr/>

<#if doc.hasHttpParams()>
<h4>Parameters:</h4>
<table width="100%" border="1" cellpadding="3" cellspacing="0">
    <thead>
        <tr>
            <td width="20%">Name</td>
            <td>Description</td>
            <td width="5%">Must</td>
            <td width="20%">Default</td>
        </tr>
    </thead>
    <tbody>
    <#list doc.httpParams as httpParam>
        <tr>
            <td>${httpParam.displayNames}</td>
            <td>${httpParam.displayDescription}</td>
            <td>${httpParam.displayMust}</td>
            <td>${httpParam.displayDefault}</td>
        </tr>
    </#list>
    </tbody>
</table>
<hr/>
</#if>


<p>${doc.displayHttpReturn}</p>
<#list doc.displayHttpExamples as httpExample>
<pre>
${httpExample}
</pre>
</#list>
<#if doc.hasHttpReturnSections()>
<hr/>
</#if>

</body>