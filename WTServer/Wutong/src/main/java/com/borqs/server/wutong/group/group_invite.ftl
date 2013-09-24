<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
    <title>邀请</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <META content="MSHTML 5.50.4613.1700" name=GENERATOR>
    <META name="viewport" content="width=device-width, minimum-scale=1.0, maximum-scale=2.0">

    <link rel="stylesheet"  href="http://${host}/invite_files/jquerymobile/jquery.mobile-1.0rc2.min.css" />
    <script src="http://${host}/invite_files/jquerymobile/jquery-1.6.4.min.js"></script>
    <script src="http://${host}/invite_files/jquerymobile/jquery.mobile-1.0rc2.min.js"></script>

    <script type=text/javascript>
        function download()
        {
            document.location = "http://${host}/search?q=com.borqs.qiupu";
        }

        function onAccept()
        {
            document.location = "${acceptUrl}";
        }

        function onReject()
        {
            document.location = "${rejectUrl}";
        }
    </script>
</head>
<body style="overflow-x:hidden; background:#fff; color:#000; font-size:18px;  width:320px; margin:auto;">
<form name="form1" action="" method="post" onsubmit="return false">
    <table cellpadding=1 cellspacing=1 border=0 align=left>
        <tr>
            <td>
                <table cellpadding=1 cellspacing=1 border=0 align=left name="t1">
                    <tr>
                        <td align=center valign="bottom">
                            <img border=0 src="http://${host}/invite_files/images/logo.jpg"></td>
                        </td>
                    </tr>
                    <tr>
                        <td align=left valign=top>
                            尊敬的${displayName}, ${fromName}邀请您${register}加入${groupType}【${groupName}】，并希望成为您的好友，以下是他的附言：<br>
                        ${message}
                        </td>
                    </tr>
                    <tr>
                        <td align=left valign=top colspan=2>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td>
                <table cellpadding=1 cellspacing=1 border=0 align=left name="t2">
                    <tr>
                        <td align=center valign=top>
                            <input value="${register}加入${groupType}" ID="accept"  type="button" name="accept" onclick="onAccept()">
                        </td>
                        <td align=center valign=top>
                            <input value="不，谢谢" ID="reject"  type="button" name="reject" onclick="onReject()">
                        </td>
                    </tr>
                    <tr>
                        <td align=left valign=top colspan=2>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td>
                <table cellpadding=1 cellspacing=1 border=0 align=left>
                    <tr>
                        <td align=left valign=top colspan=2>加入${groupType}后，您可以更方便地和其中的人进行交流、沟通和分享。现在就赶快<span style="cursor:hand" onclick="download()"><U><font color=#0000ff>下载</font></U></span>体验吧！
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td align=left valign=top colspan=2>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
        </tr>
        <tr>
            <td align=center valign=top colspan=2>© 2007-2012 Borqs 版权所有</td>
        </tr>
    </table>
</form>
</body>
</html>