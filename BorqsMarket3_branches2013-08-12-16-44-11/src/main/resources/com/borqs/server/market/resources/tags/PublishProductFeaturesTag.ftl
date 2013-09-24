<#if featureItems?has_content>
<div class="feature-nav">
  <ul class="nav nav-tabs">
      <#list featureItems as featureItem>
        <li <#if featureItem.id=currentFeature>class="active"</#if>>
          <a href="${featureItem.link}"><strong>${spring.message(featureItem.name)}</strong></a>
        </li>
      </#list>
  </ul>
</div>
</#if>