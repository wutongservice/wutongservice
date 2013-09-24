<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
    <title>播思专区应用管理</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <META content="MSHTML 5.50.4613.1700" name=GENERATOR>
</head>

<body>
<table border=0>
    <form name="XForm" method="post" action="borqs" onsubmit="return false">
        <tr>
            <td>应用包名: </td>
            <td><input type="text" name="package" /></td>
        </tr>
        <tr>
            <td><input type="radio" id="add" name="opt" value="1" />加入播思专区</td>
            <td><input type="radio" id="remove" name="opt" value="0" />从播思专区中去除</td>
        </tr>
        <tr>
            <td><input type="button" value="确定" ID="WRITE" onclick="document.XForm.submit();" /></td>
            <td><input type="reset" value="重置" /></td>
        </tr>
    </form>
</table>
</body>
</html>