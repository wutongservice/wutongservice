<#if errorMessage!=''>
    <#assign controlGroupClass=controlGroupSpan + ' error '>
<#else>
    <#assign controlGroupClass=controlGroupSpan>
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
  <label <#if cid!=''>for="${cid}"</#if> class="control-label ${labelSpan}"><strong>${label?html}&nbsp;<#if required>
    <span class="text-info">*</span></#if></strong></label>
</#if>
  <div class="controls ${controlsSpan}">
    <div class="${inputSpan}" style="display: table-row;height:40px;">
      <div style="display: table-cell;width:30%;">
        <input type="text" autocomplete="off" <#if cid!=''>id="${cid}"</#if> <#if cname!=''>name="${cname}"</#if>
               placeholder="${spring.message('TagsInputTag.text.placeholder')?html}" <#if readonly==true>readonly="readonly"</#if> >
      </div>
      <div style="display: table-cell;width: 5%"></div>
      <div id="${cid}-tags-container" style="display: table-cell;width:65%;"></div>
    </div>
    <span class="help-block" id="${cid}-tip"></span>
    <span class="help-block">${errorMessage?html}</span>
    <script type="text/javascript">
      BorqsMarket.setupTagsManager('${cid}', {
        tip: '${availableTagsTip?html}',
        prefilled: ${value},
        readonly: ${readonly?string('true', 'false')},
        availableTags: ${typeAhead},
        allowFreeTags: ${allowFreeTags?string('true', 'false')}
        });
    </script>
  </div>
</div>