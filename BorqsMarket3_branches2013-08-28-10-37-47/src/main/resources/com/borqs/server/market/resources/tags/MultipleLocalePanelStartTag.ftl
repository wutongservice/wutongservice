<div id="${id}" class="tabbable">
  <ul class="nav nav-pills">
  <#list locales as locale>
    <li class="pull-right <#if locale_index==0>active</#if>">
      <a href="#${id}_${locale}" data-toggle="tab">${locale}</a>
    </li>
  </#list>
  </ul>
  <div class="tab-content">



