<div class="second-nav">
  <div class="container">
    <div class="row">
      <ul class="breadcrumb span8" style="background-color: inherit;">
      <#list levels as level>
          <#assign titleItem=level.titleItem>
          <#assign altItems=level.altItems>
        <li>
          <a href="${titleItem.link}" data-toggle="tooltip" title="${titleItem.id}">
          ${titleItem.name?html}
          </a>
            <#if altItems?has_content>
              <span class="dropdown">
            <a class="dropdown-toggle" data-toggle="dropdown"><b class="caret"></b></a>
            <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
              <#list altItems as altItem>
              <li><a href="${altItem.link}" data-toggle="tooltip" title="${altItem.id}">${altItem.name?html}</a></li>
              </#list>
            </ul>
            </span>
            </#if>
          <span class="divider">/</span>
        </li>
      </#list>
      </ul>
    </div>
  </div>
</div>