<#if errorMessage!=''>
    <#assign controlGroupClass=controlGroupSpan + ' error '>
<#else>
    <#assign controlGroupClass=controlGroupSpan>
</#if>
<#if readonly>
    <#assign inputClass=inputSpan + ' disabled '>
<#else>
    <#assign inputClass=inputSpan>
</#if>

<#if locale!=''>
    <#assign cid=id + '_' + locale>
    <#assign cname=name_ + '_' + locale>
<#else>
    <#assign cid=id>
    <#assign cname=name_>
</#if>

<div class="control-group ${controlGroupClass}">
<#if label!=''>
  <label <#if cid!=''>for="${cid}"</#if> class="control-label ${labelSpan}"><strong>${label?html}</strong></label>
</#if>
  <div class="controls ${controlsSpan}">
    <textarea <#if cid!=''>id="${cid}"</#if> <#if cname!=''>name="${cname}"</#if> class="${inputClass}"
              placeholder="${placeholder}" <#if readonly>readonly="readonly"</#if> rows="${rows!3}">${value!''?html}</textarea>
    <span class="help-block">${errorMessage?html}</span>
  </div>
</div>
