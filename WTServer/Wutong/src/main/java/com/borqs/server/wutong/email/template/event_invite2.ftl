<body xmlns="http://www.w3.org/1999/html">
<meta http-equiv="Content-Type" content="text/html;charset=utf-8">

<style>
    body, td, p, div {
        font-size: 13px;
        FONT-FAMILY: "黑体";
        color: #646464;
    }

    body {
        text-align: left;
        margin: 0;
        overflow-x: auto;
        overflow-y: auto;
        line-height: 19px;
        background-color: #efefef;
    }
</style>

<div id="body" style="height:auto; min-height:500px; border:0px solid; margin-top:2px; margin-left:2px; width:831px; background-image:url(http://oss.aliyuncs.com/wutong-data/system/event-notification-bg-.jpg);">

    <table width="100%" cellSpacing="0" cellPadding="0" align=center height=350 border=0>
        <tr height=215>
        </tr>
        <tr>
            <td width=30%></td>
            <td width=40%>
                <span style="font-size:20px; color:#50b5e7; font-family:Verdana;">Hi</span><span style=" color:#50b5e7;font-size:20px;"> ${displayName} </span><br>
                <span style="color:#333333; font-size:14px;line-height: 2; ">
                        ${fromName} invite you ${register} join ${groupType} 【${groupName}】.
                        </br>
                        <table >
                            <tr></tr>
                            <tr><td align="left" width="25%" style="font-size:14px;">When</td><td align="left">${startTime} - ${endTime}</td></tr>
                            <tr></tr>
                            <tr><td align="left" width="25%" style="font-size:14px;">Where</td><td align="left"> ${address}</td></tr>
                            <tr></tr>
                            <tr><td align="left" width="25%" style="font-size:14px;">What</td><td align="left"> <pre>${description}</pre></td></tr>
                            <tr></tr>
                            <tr><td align="left"><pre style="font-size:14px;">${message}</pre></td></tr>
                        </table>

                </span>
            </td>
            <td width=30%></td>
        </tr>
    </table>
    <table width="100%" cellSpacing="0" cellPadding="0" align=center height=20 border=0>
        <tr>
            <td width=30%></td>
            <td width=20%><a href="${acceptUrl}"><input type=button value='Accept' style='background-image: url(http://oss.aliyuncs.com/wutong-data/system/accept_button_normal.png ); width:116px;height:39px;border: 0;background-repeat: no-repeat;'></a></td>
            <td width=20%><a href="${rejectUrl}"><input type=button value='No, thanks' style='background-image: url(http://oss.aliyuncs.com/wutong-data/system/thanks_button_normal.png ); width:116px;height:39px;border: 0;background-repeat: no-repeat;'></a></td>
            <td width=30% align="right"><img src="http://apps.borqs.com/images/logo.png" /></td>
        </tr>
    </table>
    <table width="98%" border="0" cellpadding="0" cellspacing="0" align=center>
        <tr height=4px>

        </tr>
        <tr>
            <td><div  style="border-top: 1px solid #d0d0d0;">


                <p align="center" style="font-size: 12px; line-height: 18px; color: #6a6a6a; margin: 0;"> This Email is auto sent by system, <font color='red'>don't reply directly</font>. Copyright 北京播思软件技术有限公司


                </p>

            </div></td>
        </tr>

    </table>
</div>
</body>
