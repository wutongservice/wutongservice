<#list map.mapCombine?keys as key>
    <#assign values = map.mapCombine[key]>
<div align="left">
    <div style=" width: 160px; height: 80px;float:left; margin:10px;font-size:12px;"><a href="${values.url}"
                                                                                        style="text-decoration:none;"><img
            style="max-width:80px; max-height:80px; float: left; border: 0" src="${values.iconUrl}"/></a>
        <li style="list-style-type:none; font-weight:bold">${values.follower_name}</li>
        <#if values.isFriend?exists && values.isFriend=='false'>
            <#if values.addFriendUrl?exists>
                <li style="list-style-type:none;font-size:11px; font-weight:bold"><a
                        style="color:#15A0E6; text-decoration:none;" href="${values.addFriendUrl}">addFriend</a></li>
            </#if>
        </#if>
    </div>
</div>
</#list>
<#if map.more?exists && map.more=='true'>
<div align="left">
    <div style=" width: 60px; height: 80px;float:left; margin:10px;font-size:12px; line-height:150px">
        <a style="color:#15A0E6; text-decoration:none;" href="${map.moreinfo}" style="text-decoration:none;
              bottom:0px;
              padding:0px;
              margin:0px;
               font-weight:bold">
            more...
        </a>
    </div>
</div>

</#if>
