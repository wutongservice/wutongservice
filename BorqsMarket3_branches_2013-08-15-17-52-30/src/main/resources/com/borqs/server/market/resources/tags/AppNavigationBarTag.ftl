<ul class="nav nav-list">
  <li class="nav-header">${spring.message('AppNavigationBarTag.label.apps')}</li>
<#list items as item>
  <li <#if item.id==currentAppId!''>class="active"</#if>>
    <a href="${item.link}" data-toggle="tooltip" title="${item.id}">
      <i class=" icon-chevron-right"></i>&nbsp;${item.name}
    </a>
  </li>
</#list>
</ul>

