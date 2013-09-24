<#assign content = map.mapCombine>
<div align="left">
    <div style="width: 160px; height: 80px;float:left; margin:10px;font-size:12px;"><a href="${content.url}"
                                                                                       style="text-decoration:none;">
        <img style="max-width:80px; max-height:80px; float: left; border: 0" src="${content.iconUrl}"/>
    </a>
        <li style="list-style-type:none; font-weight:bold">${content.follower_name}</li>
    <#if values.isFriend?exists && values.isFriend=='false'>
        <#if content.addFriendUrl?exists>
            <li style="list-style-type:none;font-size:11px;"><a
                    style="color:#15A0E6; text-decoration:none;font-weight:bold" href="${content.addFriendUrl}">addFriend</a>
            </li>
        </#if>
    </#if>
    </div>
</div>


