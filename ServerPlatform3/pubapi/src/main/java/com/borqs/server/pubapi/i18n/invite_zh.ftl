<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>激活您的播思账号</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<META content="MSHTML 5.50.4613.1700" name=GENERATOR>
<META name="viewport" content="width=device-width, minimum-scale=1.0, maximum-scale=2.0">

<link rel="stylesheet"  href="http://${host}/invite_files/jquerymobile/jquery.mobile-1.0rc2.min.css" />
<script src="http://${host}/invite_files/jquerymobile/jquery-1.6.4.min.js"></script>
<script src="http://${host}/invite_files/jquerymobile/jquery.mobile-1.0rc2.min.js"></script>

<script type=text/javascript>
var keyStr = "ABCDEFGHIJKLMNOP" +
        "QRSTUVWXYZabcdef" +
        "ghijklmnopqrstuv" +
        "wxyz0123456789+/" +
        "=";

function encode64(input)
{
    input = escape(input);
    var output = "";
    var chr1, chr2, chr3 = "";
    var enc1, enc2, enc3, enc4 = "";
    var i = 0;

    do
    {
        chr1 = input.charCodeAt(i++);
        chr2 = input.charCodeAt(i++);
        chr3 = input.charCodeAt(i++);

        enc1 = chr1 >> 2;
        enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
        enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
        enc4 = chr3 & 63;

        if (isNaN(chr2))
        {
            enc3 = enc4 = 64;
        }
        else if (isNaN(chr3))
        {
            enc4 = 64;
        }

        output = output +
                keyStr.charAt(enc1) +
                keyStr.charAt(enc2) +
                keyStr.charAt(enc3) +
                keyStr.charAt(enc4);
        chr1 = chr2 = chr3 = "";
        enc1 = enc2 = enc3 = enc4 = "";
    } while (i < input.length);

    return output;
}

function MD5(sMessage) {
    function RotateLeft(lValue, iShiftBits) { return (lValue<<iShiftBits) | (lValue>>>(32-iShiftBits)); }
    function AddUnsigned(lX,lY) {
        var lX4,lY4,lX8,lY8,lResult;
        lX8 = (lX & 0x80000000);
        lY8 = (lY & 0x80000000);
        lX4 = (lX & 0x40000000);
        lY4 = (lY & 0x40000000);
        lResult = (lX & 0x3FFFFFFF)+(lY & 0x3FFFFFFF);
        if (lX4 & lY4) return (lResult ^ 0x80000000 ^ lX8 ^ lY8);
        if (lX4 | lY4) {
            if (lResult & 0x40000000) return (lResult ^ 0xC0000000 ^ lX8 ^ lY8);
            else return (lResult ^ 0x40000000 ^ lX8 ^ lY8);
        } else return (lResult ^ lX8 ^ lY8);
    }
    function F(x,y,z) { return (x & y) | ((~x) & z); }
    function G(x,y,z) { return (x & z) | (y & (~z)); }
    function H(x,y,z) { return (x ^ y ^ z); }
    function I(x,y,z) { return (y ^ (x | (~z))); }
    function FF(a,b,c,d,x,s,ac) {
        a = AddUnsigned(a, AddUnsigned(AddUnsigned(F(b, c, d), x), ac));
        return AddUnsigned(RotateLeft(a, s), b);
    }
    function GG(a,b,c,d,x,s,ac) {
        a = AddUnsigned(a, AddUnsigned(AddUnsigned(G(b, c, d), x), ac));
        return AddUnsigned(RotateLeft(a, s), b);
    }
    function HH(a,b,c,d,x,s,ac) {
        a = AddUnsigned(a, AddUnsigned(AddUnsigned(H(b, c, d), x), ac));
        return AddUnsigned(RotateLeft(a, s), b);
    }
    function II(a,b,c,d,x,s,ac) {
        a = AddUnsigned(a, AddUnsigned(AddUnsigned(I(b, c, d), x), ac));
        return AddUnsigned(RotateLeft(a, s), b);
    }
    function ConvertToWordArray(sMessage) {
        var lWordCount;
        var lMessageLength = sMessage.length;
        var lNumberOfWords_temp1=lMessageLength + 8;
        var lNumberOfWords_temp2=(lNumberOfWords_temp1-(lNumberOfWords_temp1 % 64))/64;
        var lNumberOfWords = (lNumberOfWords_temp2+1)*16;
        var lWordArray=Array(lNumberOfWords-1);
        var lBytePosition = 0;
        var lByteCount = 0;
        while ( lByteCount < lMessageLength ) {
            lWordCount = (lByteCount-(lByteCount % 4))/4;
            lBytePosition = (lByteCount % 4)*8;
            lWordArray[lWordCount] = (lWordArray[lWordCount] | (sMessage.charCodeAt(lByteCount)<<lBytePosition));
            lByteCount++;
        }
        lWordCount = (lByteCount-(lByteCount % 4))/4;
        lBytePosition = (lByteCount % 4)*8;
        lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80<<lBytePosition);
        lWordArray[lNumberOfWords-2] = lMessageLength<<3;
        lWordArray[lNumberOfWords-1] = lMessageLength>>>29;
        return lWordArray;
    }
    function WordToHex(lValue) {
        var WordToHexValue="",WordToHexValue_temp="",lByte,lCount;
        for (lCount = 0;lCount<=3;lCount++) {
            lByte = (lValue>>>(lCount*8)) & 255;
            WordToHexValue_temp = "0" + lByte.toString(16);
            WordToHexValue = WordToHexValue + WordToHexValue_temp.substr(WordToHexValue_temp.length-2,2);
        }
        return WordToHexValue;
    }
    var x=Array();
    var k,AA,BB,CC,DD,a,b,c,d
    var S11=7, S12=12, S13=17, S14=22;
    var S21=5, S22=9 , S23=14, S24=20;
    var S31=4, S32=11, S33=16, S34=23;
    var S41=6, S42=10, S43=15, S44=21;
// Steps 1 and 2. Append padding bits and length and convert to words
    x = ConvertToWordArray(sMessage);
// Step 3. Initialise
    a = 0x67452301; b = 0xEFCDAB89; c = 0x98BADCFE; d = 0x10325476;
// Step 4. Process the message in 16-word blocks
    for (k=0;k<x.length;k+=16) {
        AA=a; BB=b; CC=c; DD=d;
        a=FF(a,b,c,d,x[k+0], S11,0xD76AA478);
        d=FF(d,a,b,c,x[k+1], S12,0xE8C7B756);
        c=FF(c,d,a,b,x[k+2], S13,0x242070DB);
        b=FF(b,c,d,a,x[k+3], S14,0xC1BDCEEE);
        a=FF(a,b,c,d,x[k+4], S11,0xF57C0FAF);
        d=FF(d,a,b,c,x[k+5], S12,0x4787C62A);
        c=FF(c,d,a,b,x[k+6], S13,0xA8304613);
        b=FF(b,c,d,a,x[k+7], S14,0xFD469501);
        a=FF(a,b,c,d,x[k+8], S11,0x698098D8);
        d=FF(d,a,b,c,x[k+9], S12,0x8B44F7AF);
        c=FF(c,d,a,b,x[k+10],S13,0xFFFF5BB1);
        b=FF(b,c,d,a,x[k+11],S14,0x895CD7BE);
        a=FF(a,b,c,d,x[k+12],S11,0x6B901122);
        d=FF(d,a,b,c,x[k+13],S12,0xFD987193);
        c=FF(c,d,a,b,x[k+14],S13,0xA679438E);
        b=FF(b,c,d,a,x[k+15],S14,0x49B40821);
        a=GG(a,b,c,d,x[k+1], S21,0xF61E2562);
        d=GG(d,a,b,c,x[k+6], S22,0xC040B340);
        c=GG(c,d,a,b,x[k+11],S23,0x265E5A51);
        b=GG(b,c,d,a,x[k+0], S24,0xE9B6C7AA);
        a=GG(a,b,c,d,x[k+5], S21,0xD62F105D);
        d=GG(d,a,b,c,x[k+10],S22,0x2441453);
        c=GG(c,d,a,b,x[k+15],S23,0xD8A1E681);
        b=GG(b,c,d,a,x[k+4], S24,0xE7D3FBC8);
        a=GG(a,b,c,d,x[k+9], S21,0x21E1CDE6);
        d=GG(d,a,b,c,x[k+14],S22,0xC33707D6);
        c=GG(c,d,a,b,x[k+3], S23,0xF4D50D87);
        b=GG(b,c,d,a,x[k+8], S24,0x455A14ED);
        a=GG(a,b,c,d,x[k+13],S21,0xA9E3E905);
        d=GG(d,a,b,c,x[k+2], S22,0xFCEFA3F8);
        c=GG(c,d,a,b,x[k+7], S23,0x676F02D9);
        b=GG(b,c,d,a,x[k+12],S24,0x8D2A4C8A);
        a=HH(a,b,c,d,x[k+5], S31,0xFFFA3942);
        d=HH(d,a,b,c,x[k+8], S32,0x8771F681);
        c=HH(c,d,a,b,x[k+11],S33,0x6D9D6122);
        b=HH(b,c,d,a,x[k+14],S34,0xFDE5380C);
        a=HH(a,b,c,d,x[k+1], S31,0xA4BEEA44);
        d=HH(d,a,b,c,x[k+4], S32,0x4BDECFA9);
        c=HH(c,d,a,b,x[k+7], S33,0xF6BB4B60);
        b=HH(b,c,d,a,x[k+10],S34,0xBEBFBC70);
        a=HH(a,b,c,d,x[k+13],S31,0x289B7EC6);
        d=HH(d,a,b,c,x[k+0], S32,0xEAA127FA);
        c=HH(c,d,a,b,x[k+3], S33,0xD4EF3085);
        b=HH(b,c,d,a,x[k+6], S34,0x4881D05);
        a=HH(a,b,c,d,x[k+9], S31,0xD9D4D039);
        d=HH(d,a,b,c,x[k+12],S32,0xE6DB99E5);
        c=HH(c,d,a,b,x[k+15],S33,0x1FA27CF8);
        b=HH(b,c,d,a,x[k+2], S34,0xC4AC5665);
        a=II(a,b,c,d,x[k+0], S41,0xF4292244);
        d=II(d,a,b,c,x[k+7], S42,0x432AFF97);
        c=II(c,d,a,b,x[k+14],S43,0xAB9423A7);
        b=II(b,c,d,a,x[k+5], S44,0xFC93A039);
        a=II(a,b,c,d,x[k+12],S41,0x655B59C3);
        d=II(d,a,b,c,x[k+3], S42,0x8F0CCC92);
        c=II(c,d,a,b,x[k+10],S43,0xFFEFF47D);
        b=II(b,c,d,a,x[k+1], S44,0x85845DD1);
        a=II(a,b,c,d,x[k+8], S41,0x6FA87E4F);
        d=II(d,a,b,c,x[k+15],S42,0xFE2CE6E0);
        c=II(c,d,a,b,x[k+6], S43,0xA3014314);
        b=II(b,c,d,a,x[k+13],S44,0x4E0811A1);
        a=II(a,b,c,d,x[k+4], S41,0xF7537E82);
        d=II(d,a,b,c,x[k+11],S42,0xBD3AF235);
        c=II(c,d,a,b,x[k+2], S43,0x2AD7D2BB);
        b=II(b,c,d,a,x[k+9], S44,0xEB86D391);
        a=AddUnsigned(a,AA); b=AddUnsigned(b,BB); c=AddUnsigned(c,CC); d=AddUnsigned(d,DD);
    }
// Step 5. Output the 128 bit digest
    var temp= WordToHex(a)+WordToHex(b)+WordToHex(c)+WordToHex(d);
    return temp.toLowerCase();
}

function isEmail(s){
    var patrn = /^(.+)@(.+)$/;
    if(!patrn.exec(s)) {
        return false;
    }
    return true;
}

function isPhone(s){
    var patrn = /^[0-9]{11}$/;
    if(!patrn.exec(s)) {
        return false;
    }
    return true;
}

function checkPassword(s){
    var patrn = /^\w{4,15}$/;
    if(!patrn.exec(s)) {
        return false;
    }
    return true;
}

function expandCell(id)
{
    var item = document.getElementById(id);
    var isFold = (item.style.display=="none");
    item.style.display = isFold ? "" : "none";
}


function activeUser()
{

    var password = document.form1.password.value;

    if(password.length == 0)
    {
        alert("请您输入密码。");
        return;
    }
    else
    {
        var confirm = document.form1.confirm.value;
        if(confirm.length == 0)
        {
            alert("请输入确认密码。");
            return;
        }
        else
        {
            if(password != confirm)
            {
                alert("确认密码与密码不一致。");
                return;
            }

            if(checkPassword(password) == false)
            {
                alert("密码只能由数字，字母和下划线组成，长度在6～15之间。");
                return;
            }
        }
    }

    var display_name = document.form1.display_name.value;
    if(display_name.length == 0)
    {
        alert("请输入姓名。");
        return;
    }

    document.form1.password.value = MD5(password);
    document.form1.submit();

    var trActiveBtn = document.getElementById("trActiveBtn");
    var trActiveDown = document.getElementById("trActiveDown");
    trActiveBtn.style.display = "none";
    trActiveDown.style.display = "";
}

function bindUser()
{
    var password = document.form2.borqs_pwd.value;
    document.form2.borqs_pwd.value = MD5(password);
    document.form2.submit();
}

function download()
{
    document.location = "http://api.borqs.com/search?q=com.borqs.qiupu";
}

function mutualFriend()
{
    document.form3.submit();
}

function activeDownload()
{
    var bind = document.form1.bind.value;
    var pwd = document.form1.password.value;
    document.location = "http://api.borqs.com/qiupu/active_down?bind=" + bind + "&password=" + pwd;
}
</script>
</head>
<body style="overflow-x:hidden; background:#fff; color:#000; font-size:18px;  width:320px; margin:auto;">
    <table cellpadding=1 cellspacing=1 border=0 align=left>
        <tr>
            <td>
                <table cellpadding=1 cellspacing=1 border=0 align=left name="t1">
                    <tr>
                        <td align=center valign="bottom">
                            <img border=0 src="http://${host}/invite_files/images/logo.jpg"></td>
                        </td>
                    </tr>
    <#assign uid = uid?number>
    <#if uid == 0>
                    <tr>
                        <td align=left valign=top>尊敬的${name0}：<br>
                            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${fromName}邀请您激活播思账号，并希望成为您的好友。</td>
                    </tr>
                    <tr>
                        <td align=left valign=top>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr style="cursor:hand" onclick="expandCell('trActive')">
            <td>
                <img src="http://${host}/invite_files/images/open_column.gif" width="23" height="20">&nbsp;<font size="4px"><strong>激活您的播思账号</strong></font>
            </td>
        </tr>
        <tr id="trActive">
            <td>
                <form name="form1" action="#" method="post" onsubmit="return false">
                    <input type="hidden" name="fromid" value="${fromId}" />
                    <input type="hidden" name="uid" value="${uid}" />
                    <input type="hidden" name="action" value="1"/>
                <table cellpadding=1 cellspacing=1 border=0 align=left name="t2">
                    <tr>
                        <td align=right valign=middle name="td1">姓名：</td>
                        <td align=left valign=top><input type="text" name="display_name" value="${name0}"></td>
                    </tr>
                    <tr>
                        <td align=right valign=middle name="td2">性别：</td>
                        <td align=left valign=top><select name="gender"><option value="m">男</option><option value="f">女</option></select></td>
                    </tr>
                    <tr>
                        <td align=right valign=middle name="td2">登录名：</td>
                        <td align=left valign=top><input type="text" name="bind" value="${login_name}" readonly></td>
                    </tr>
                    <tr>
                        <td align=right valign=middle name="td4">密码：</td>
                        <td align=left valign=top><input type="password" name="password"></td>
                    </tr>
                    <tr>
                        <td align=right valign=middle name="td5">确认密码：</td>
                        <td align=left valign=top><input type="password" name="confirm"></td>
                    </tr>
                    <tr>
                        <td align=left valign=top colspan=2>    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input name="mutual" type="checkbox" id="mutual" class="custom" value="true" checked="checked"/><label for="mutual">加邀请者为好友</label></td>
                    </tr>
                    <tr>
                        <td align=left valign=top colspan=2>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    </tr>
                    <tr id="trActiveBtn">
                        <td align=center valign=top colspan=2>

                            <input value="激    活" ID="activeBtn"  type="button" name="activeBtn" onclick="activeUser()">

                        </td>
                    </tr>
                    <tr style="display:none" id="trActiveDown">
                        <td align=center valign=top colspan=2>

                            <input value="激    活" id="activeDown"  type="button" name="activeDown" onclick="activeDownload()">

                        </td>
                    </tr>
                    <tr>
                        <td align=left valign=top colspan=2>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    </tr>
                </table>
                </form>
            </td>
        </tr>

        <tr>
            <td>
                <table cellpadding=1 cellspacing=1 border=0 align=left>
                    <tr style="cursor:hand" onclick="expandCell('trBind')">
                        <td>
                            <img src="http://${host}/invite_files/images/open_column.gif" width="23" height="20">&nbsp;<font size="4px"><strong>已经注册过播思账号？推荐您将上面的登录名绑定到您的已有账号上</strong></font>
                        </td>
                    </tr>
                    <tr style="display:none" id="trBind">
                        <td>
                            <form name="form2" action="#" method="post" onsubmit="return false">
                                <input type="hidden" name="fromid" value="${fromId}" />
                                <input type="hidden" name="uid" value="${uid}" />
                                <input type="hidden" name="bind" value="${login_name}" />
                                <input type="hidden" name="action" value="2"/>
                            <table cellpadding=1 cellspacing=1 border=0 align=left name="t2">
                                <tr>
                                    <td align=right valign=middle name="td2">已有播思账号：</td>
                                    <td align=left valign=top><input type="text" name="borqs_account"></td>
                                </tr>
                                <tr>
                                    <td align=right valign=middle name="td4">密码：</td>
                                    <td align=left valign=top><input type="password" name="borqs_pwd"></td>
                                </tr>
                                <tr>
                                    <td align=center valign=top colspan=2>
                                        <input value="绑    定" ID="button"  type="button" name="activeBtn" onclick="bindUser()">
                                    </td>
                                </tr>
                            </table>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td align=left valign=top>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    </tr>
                </table>
            </td>
        </tr>
    <#else>
        <tr>
            <td align=left valign=top>尊敬的${name0}：<br>
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${fromName}
                <#assign isFriend = isFriend?number>
                <#if isFriend == 0>
                    希望成为您的好友。
                <#else>
                    邀请您使用播思服务(梧桐)。
                </#if>
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
                <form name="form3" action="#" method="post" onsubmit="return false">
                    <input type="hidden" name="fromid" value="${fromId}" />
                    <input type="hidden" name="uid" value="${uid}" />
                    <input type="hidden" name="action" value="3"/>
                <table cellpadding=1 cellspacing=1 border=0 align=left name="t2">
                    <tr>
                        <#if isFriend == 0>
                        <td align=center valign=top>
                            <input value="加为好友" ID="button"  type="button" name="addBtn" onclick="mutualFriend()">
                        </td>
                        </#if>
                        <td align=center valign=top>
                            <input value="下    载" ID="button"  type="button" name="downBtn" onclick="download()">
                        </td>
                    </tr>
                    <tr>
                        <td align=left valign=top colspan=2>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    </tr>
                </table>
                </form>
            </td>
        </tr>
    </#if>
        <tr>
            <td>
                <table cellpadding=1 cellspacing=1 border=0 align=left>
                    <tr>
                        <td align=left valign=top colspan=2>激活播思账号后，您可以免费使用播思账号为您提供的应用和便捷的服务。其中包括应用宝盒，播思读书，OpenFace等。应用宝盒是由北京播思软件技术有限公司2011年重磅推出的一款集应用管理、应用商店、社交网络、数据备份等多功能于一体的Android客户端软件。播思读书为您提供分享和阅读图书的丰富体验。OpenFace让您和您的好友面对面无距离沟通。现在就赶快<span style="cursor:hand" onclick="download()"><U><font color=#0000ff>下载</font></U></span>体验吧！
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
</body>
</html>