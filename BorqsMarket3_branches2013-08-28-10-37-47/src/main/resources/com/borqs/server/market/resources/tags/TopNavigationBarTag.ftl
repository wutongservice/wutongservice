<div class="navbar navbar-fixed-top" style="height:50px;">
  <div class="navbar-inner">
    <div class="container">
      <div class="row-fluid">
        <img class="pull-right" alt="logo" src="/logo.png" style="height:30px;width:30px;margin:10px;"/>
        <span class="brand">Borqs Market</span>

        <#if module != 'signin' && module != 'error'>
          <ul class="nav">
            <li <#if module == 'publish'>class="active"</#if>>
              <a href="/publish">${spring.message('TopNavigationBarTag.label.publish')}</a>
            </li>
              <#if developEnabled?? && developEnabled == true>
                <li <#if module == 'develop'>class="active"</#if>>
                  <a href="/develop">${spring.message('TopNavigationBarTag.label.develop')}</a>
                </li>
              </#if>
              <#if operEnabled?? && operEnabled == true>
                <li <#if module == 'oper'>class="active"</#if>>
                  <a href="/oper">${spring.message('TopNavigationBarTag.label.oper')}</a>
                </li>
              </#if>
              <#if gstatEnabled?? && gstatEnabled == true>
                <li <#if module == 'gstat'>class="active"</#if>>
                  <a href="/gstat">${spring.message('TopNavigationBarTag.label.gstat')}</a>
                </li>
              </#if>
          </ul>
          <ul class="nav pull-right">
            <li>
              <a href="/signout">${spring.message('TopNavigationBarTag.label.signout')}</a>
            </li>
          </ul>
        </#if>
        </div>
      </div>
    </div>
  </div>
</div>
