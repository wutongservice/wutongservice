<#if active == true>
    <#assign spanClass="text-success display-version"/>
    <#assign tip="Active">
<#else>
    <#assign spanClass="text-error display-version"/>
    <#assign tip="Unpublished">
</#if>
<a <#if id!=''>id="${id}"</#if> href="/publish/products/${product}/${_version}">
  <span class="${spanClass}" style="text-align: center; overflow: hidden;"><strong>[${_version}]&nbsp;${versionName}<#if beta!=0>&nbsp;<span class="badge badge-warning version-beta">BETA</span></#if></strong></span>
</a>