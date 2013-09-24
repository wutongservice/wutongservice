<html>
<head>
    <meta http-equiv=Content-Type content=text/html; charset=utf-8>
    <title>send mail</title>
</head>
<body style:background-image:url(http://www.borqs.com/img/logo.jpg)>
<form onsubmit="return false">
    <input type="hidden" name="emailType" value="${emailType}" />
<table width=600 align=left cellpadding=3 cellspacing=3 style='border-top:#cfd8c5 1px solid;border-left:#cfd8c5 1px solid;border-right:#cfd8c5 1px solid;border-bottom:#cfd8c5 1px solid;' border=0>
    <tr valign=middle>
        <td valign=middle colspan=2 height=22 style='border-top:#c5f6c4 1px solid;border-left:#c5f6c4 1px solid;border-right:#c5f6c4 1px solid;border-bottom:#c5f6c4 1px solid;background-color:#b1cf92;;text-align:left;color:#024079;padding:2px 2px 0px 2px'><img width=40 border=0 src=http://${serverHost}/sys/icon/bpc.png><b>&nbsp;&nbsp;<font size:3>播思通行证</font></b></td>
    </tr>
    <tr>
        <td colspan=2 height=10></td>
    </tr>
    <tr>
        <td style='border-bottom:#999999 0px solid;color:#333333;padding:2px 2px 0px 2px;word-wrap:break-word; word-break:break-all;'>
        ${content}
            <br><br><br>
        </td></tr>
    <tr>
        <td colspan=2 style='border-bottom:#999999 0px solid;color:#333333;padding:2px 2px 0px 2px;word-wrap:break-word; word-break:break-all;'>
            <table width=100% align=center cellpadding=2 cellspacing=2 style='border-top:cef9d1 1px solid;border-left:cef9d1 1px solid;border-right:cef9d1 1px solid;border-bottom:cef9d1 1px solid;background-color:#edf9e0' border=0>
                <tr>
                    <td>
                    <#assign emailType = emailType?string>
                    <#if emailType == "email.share_to">
                        您收到此邮件是因为您的好友指定<a href=mailto:${to}>${to}</a>作为接受者。<br>     如果您不想再接收到此种类型的邮件，请点击<a href=http://${serverHost}/preferences/subscribe?user=${to}&type=${type}&value=1 target=_blank>退订</a>。<br>
                    <#elseif emailType != "email.essential">
                        这封邮件是发送给<a href=mailto:${to}>${to}</a>的，<br>     如果您不想再接收到此种类型的邮件，请点击<a href=http://${serverHost}/preferences/subscribe?user=${to}&type=${type}&value=1 target=_blank>退订</a>。<br>
                    </#if>
                        北京播思软件技术有限公司<br>		<a href=http://${serverHost}/search?q=com.borqs.qiupu target=_blank>http://${serverHost}/search?q=com.borqs.qiupu</a>
                    </td>
                </tr>
            </table>
    </tr>
</table>
</form>
</body>
</html>