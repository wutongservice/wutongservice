function wutongaddr() {
    var tempStr = "http://localhost:6789/";
    //var tempStr = "http://api.borqs.com:6789/";
    return tempStr;
}

function invokeApi(cmd, jdata, call)
{	
	$.getJSON(wutongaddr()+cmd, jdata, function(ret){
		call(ret);
	});
}

function invokeApiTemp(cmd, jdata, call)
{	
	$.getJSON(wutongaddr()+cmd+"?callback=?", jdata, function(ret){
		call(ret);
	});
}

function invokeApiLogin(cmd, jdata, call)
{	
	$.getJSON("http://api.borqs.com/"+cmd+"?callback=?", jdata, function(ret){
		call(ret);
	});
}

function date2str(value) {
	if(value == null || isNaN(value)) {
		return null;
	}
	Date.prototype.format = function(fmt) {
		var o = {   
		"M+" : this.getMonth()+1,                 //月份   
		"d+" : this.getDate(),                    //日   
		"h+" : this.getHours(),                   //小时   
		"m+" : this.getMinutes(),                 //分   
		"s+" : this.getSeconds(),                 //秒   
		"q+" : Math.floor((this.getMonth()+3)/3), //季度   
		"S"  : this.getMilliseconds()             //毫秒   
		};
		if(/(y+)/.test(fmt)) {
			fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
		}
		for(var k in o) {
		    if(new RegExp("("+ k +")").test(fmt)) {
		    	fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));   
		    }
		}
		return fmt;   
	}
	var d = new Date();
	d.setTime(value);
	return d.format("yyyy-MM-dd hh:mm");
}


		

function setCookie(cookie_name, cookie_val, ex) {

    if (!navigator.cookieEnabled) {
    } else {

        cookie_val = escape(cookie_val);
        var expires = ex != null ? ex : 30;
        //var path = path != null ? path : "/";
        //var domain = dm != null ? dm : "localhost";
        //var satey = sa != null ? sa : ":secure";


        var date = new Date();
        date.setTime(date.getTime() + 60000 * expires);


        document.cookie = cookie_name + '=' + cookie_val + ';expires=' + date.toGMTString();


    }
}

function getCookie(cookie_name) {
    var allcookies = document.cookie;
    var cookie_pos = allcookies.indexOf(cookie_name);

    if (cookie_pos != -1) {
        cookie_pos += cookie_name.length + 1;
        var cookie_end = allcookies.indexOf(";", cookie_pos);
        if (cookie_end == -1) {
            cookie_end = allcookies.length;
        }
        var value = unescape(allcookies.substring(cookie_pos, cookie_end));
    }
    return value;
}


function myreplace(instr) {
    var tempStr = instr;
    while (tempStr.indexOf("\"") > -1) {
        tempStr = tempStr.replace("\"", "");
    }
    while (tempStr.indexOf(" ") > -1) {
        tempStr = tempStr.replace(" ", "");
    }
    while (tempStr.indexOf("'") > -1) {
        tempStr = tempStr.replace("'", "");
    }
    while (tempStr.indexOf("%") > -1) {
        tempStr = tempStr.replace("%", "");
    }
    while (tempStr.indexOf("^") > -1) {
        tempStr = tempStr.replace("^", "");
    }
    if (tempStr.length != instr.length) {
        return false;
    }
    else {
        return true;
    }
}

function newReplace(mystr,instr,tostr)
{
   var re=new RegExp(instr,"g");
   var newstart="";
   if (mystr !=""){
   	newstart = mystr.replace(re,tostr);
   }
   return newstart;
} 

function myreplaceAll(mystr,instr,tostr) {
    var tempStr = mystr;
    while (tempStr.indexOf(instr) > -1) {
        tempStr = tempStr.replace(instr, tostr);
    }
    return tempStr;
}

function mystrreplace(instr) {
    var tempStr = instr;
    tempStr = tempStr.replace("\"", "”");
    tempStr = tempStr.replace("'", "’");
    tempStr = tempStr.replace("%", "");
    tempStr = tempStr.replace("^", "");
    return tempStr;

}


function encode64(input) {
	if (input !=null && input !=undefined){
		    var keyStr = "ABCDEFGHIJKLMNOP" +
        "QRSTUVWXYZabcdef" +
        "ghijklmnopqrstuv" +
        "wxyz0123456789+/" +
        "=";

    input = escape(input);
    var output = "";
    var chr1, chr2, chr3 = "";
    var enc1, enc2, enc3, enc4 = "";
    var i = 0;

    do {
        chr1 = input.charCodeAt(i++);
        chr2 = input.charCodeAt(i++);
        chr3 = input.charCodeAt(i++);

        enc1 = chr1 >> 2;
        enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
        enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
        enc4 = chr3 & 63;

        if (isNaN(chr2)) {
            enc3 = enc4 = 64;
        } else if (isNaN(chr3)) {
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
else{
		return "";
	}
}

function decode64(input) {
	if (input !=null && input !=undefined){
		    var keyStr = "ABCDEFGHIJKLMNOP" +
        "QRSTUVWXYZabcdef" +
        "ghijklmnopqrstuv" +
        "wxyz0123456789+/" +
        "=";
    var output = "";
    var chr1, chr2, chr3 = "";
    var enc1, enc2, enc3, enc4 = "";
    var i = 0;

    // remove all characters that are not A-Z, a-z, 0-9, +, /, or =
    var base64test = /[^A-Za-z0-9\+\/\=]/g;
    if (base64test.exec(input)) {
    }
    input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

    do {
        enc1 = keyStr.indexOf(input.charAt(i++));
        enc2 = keyStr.indexOf(input.charAt(i++));
        enc3 = keyStr.indexOf(input.charAt(i++));
        enc4 = keyStr.indexOf(input.charAt(i++));

        chr1 = (enc1 << 2) | (enc2 >> 4);
        chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
        chr3 = ((enc3 & 3) << 6) | enc4;

        output = output + String.fromCharCode(chr1);

        if (enc3 != 64) {
            output = output + String.fromCharCode(chr2);
        }
        if (enc4 != 64) {
            output = output + String.fromCharCode(chr3);
        }

        chr1 = chr2 = chr3 = "";
        enc1 = enc2 = enc3 = enc4 = "";

    } while (i < input.length);
    return unescape(output);
	}else{
		return "";
	}
}


function GetUrlParms() {
    var args = new Object();
    var query = location.search.substring(1);//获取查询串
    var pairs = query.split("&");//在逗号处断开
    for (var i = 0; i < pairs.length; i++) {
        var pos = pairs[i].indexOf('=');//查找name=value
        if (pos == -1)   continue;//如果没有找到就跳过
        var argname = pairs[i].substring(0, pos);//提取name
        var value = pairs[i].substring(pos + 1);//提取value
        args[argname] = unescape(value);//存为属性
    }
    return args;
}




//中文utf-8编码
function EncodeUtf8(s1) {
    var s = escape(s1);
    var sa = s.split("%");
    var retV = "";
    if (sa[0] != "") {
        retV = sa[0];
    }
    for (var i = 1; i < sa.length; i ++) {
        if (sa[i].substring(0, 1) == "u") {
            retV += Hex2Utf8(Str2Hex(sa[i].substring(1, 5)));

        }
        else retV += "%" + sa[i];
    }

    return retV;
}
function Str2Hex(s) {
    var c = "";
    var n;
    var ss = "0123456789ABCDEF";
    var digS = "";
    for (var i = 0; i < s.length; i ++) {
        c = s.charAt(i);
        n = ss.indexOf(c);
        digS += Dec2Dig(eval(n));

    }
    //return value;
    return digS;
}
function Dec2Dig(n1) {
    var s = "";
    var n2 = 0;
    for (var i = 0; i < 4; i++) {
        n2 = Math.pow(2, 3 - i);
        if (n1 >= n2) {
            s += '1';
            n1 = n1 - n2;
        }
        else
            s += '0';

    }
    return s;

}
function Dig2Dec(s) {
    var retV = 0;
    if (s.length == 4) {
        for (var i = 0; i < 4; i ++) {
            retV += eval(s.charAt(i)) * Math.pow(2, 3 - i);
        }
        return retV;
    }
    return -1;
}
function Hex2Utf8(s) {
    var retS = "";
    var tempS = "";
    var ss = "";
    if (s.length == 16) {
        tempS = "1110" + s.substring(0, 4);
        tempS += "10" + s.substring(4, 10);
        tempS += "10" + s.substring(10, 16);
        var sss = "0123456789ABCDEF";
        for (var i = 0; i < 3; i ++) {
            retS += "%";
            ss = tempS.substring(i * 8, (eval(i) + 1) * 8);


            retS += sss.charAt(Dig2Dec(ss.substring(0, 4)));
            retS += sss.charAt(Dig2Dec(ss.substring(4, 8)));
        }
        return retS;
    }
    return "";
}

var hexcase=0;
function hex_md5(a)
{ 
	if(a=="") return a; 
	return rstr2hex(rstr_md5(str2rstr_utf8(a)))}function hex_hmac_md5(a,b){return rstr2hex(rstr_hmac_md5(str2rstr_utf8(a),str2rstr_utf8(b)))}function md5_vm_test(){return hex_md5("abc").toLowerCase()=="900150983cd24fb0d6963f7d28e17f72"}function rstr_md5(a){return binl2rstr(binl_md5(rstr2binl(a),a.length*8))}function rstr_hmac_md5(c,f){var e=rstr2binl(c);if(e.length>16){e=binl_md5(e,c.length*8)}var a=Array(16),d=Array(16);for(var b=0;b<16;b++){a[b]=e[b]^909522486;d[b]=e[b]^1549556828}var g=binl_md5(a.concat(rstr2binl(f)),512+f.length*8);return binl2rstr(binl_md5(d.concat(g),512+128))}function rstr2hex(c){try{hexcase}catch(g){hexcase=0}var f=hexcase?"0123456789ABCDEF":"0123456789abcdef";var b="";var a;for(var d=0;d<c.length;d++){a=c.charCodeAt(d);b+=f.charAt((a>>>4)&15)+f.charAt(a&15)}return b}function str2rstr_utf8(c){var b="";var d=-1;var a,e;while(++d<c.length){a=c.charCodeAt(d);e=d+1<c.length?c.charCodeAt(d+1):0;if(55296<=a&&a<=56319&&56320<=e&&e<=57343){a=65536+((a&1023)<<10)+(e&1023);d++}if(a<=127){b+=String.fromCharCode(a)}else{if(a<=2047){b+=String.fromCharCode(192|((a>>>6)&31),128|(a&63))}else{if(a<=65535){b+=String.fromCharCode(224|((a>>>12)&15),128|((a>>>6)&63),128|(a&63))}else{if(a<=2097151){b+=String.fromCharCode(240|((a>>>18)&7),128|((a>>>12)&63),128|((a>>>6)&63),128|(a&63))}}}}}return b}function rstr2binl(b){var a=Array(b.length>>2);for(var c=0;c<a.length;c++){a[c]=0}for(var c=0;c<b.length*8;c+=8){a[c>>5]|=(b.charCodeAt(c/8)&255)<<(c%32)}return a}function binl2rstr(b){var a="";for(var c=0;c<b.length*32;c+=8){a+=String.fromCharCode((b[c>>5]>>>(c%32))&255)}return a}function binl_md5(p,k){p[k>>5]|=128<<((k)%32);p[(((k+64)>>>9)<<4)+14]=k;var o=1732584193;var n=-271733879;var m=-1732584194;var l=271733878;for(var g=0;g<p.length;g+=16){var j=o;var h=n;var f=m;var e=l;o=md5_ff(o,n,m,l,p[g+0],7,-680876936);l=md5_ff(l,o,n,m,p[g+1],12,-389564586);m=md5_ff(m,l,o,n,p[g+2],17,606105819);n=md5_ff(n,m,l,o,p[g+3],22,-1044525330);o=md5_ff(o,n,m,l,p[g+4],7,-176418897);l=md5_ff(l,o,n,m,p[g+5],12,1200080426);m=md5_ff(m,l,o,n,p[g+6],17,-1473231341);n=md5_ff(n,m,l,o,p[g+7],22,-45705983);o=md5_ff(o,n,m,l,p[g+8],7,1770035416);l=md5_ff(l,o,n,m,p[g+9],12,-1958414417);m=md5_ff(m,l,o,n,p[g+10],17,-42063);n=md5_ff(n,m,l,o,p[g+11],22,-1990404162);o=md5_ff(o,n,m,l,p[g+12],7,1804603682);l=md5_ff(l,o,n,m,p[g+13],12,-40341101);m=md5_ff(m,l,o,n,p[g+14],17,-1502002290);n=md5_ff(n,m,l,o,p[g+15],22,1236535329);o=md5_gg(o,n,m,l,p[g+1],5,-165796510);l=md5_gg(l,o,n,m,p[g+6],9,-1069501632);m=md5_gg(m,l,o,n,p[g+11],14,643717713);n=md5_gg(n,m,l,o,p[g+0],20,-373897302);o=md5_gg(o,n,m,l,p[g+5],5,-701558691);l=md5_gg(l,o,n,m,p[g+10],9,38016083);m=md5_gg(m,l,o,n,p[g+15],14,-660478335);n=md5_gg(n,m,l,o,p[g+4],20,-405537848);o=md5_gg(o,n,m,l,p[g+9],5,568446438);l=md5_gg(l,o,n,m,p[g+14],9,-1019803690);m=md5_gg(m,l,o,n,p[g+3],14,-187363961);n=md5_gg(n,m,l,o,p[g+8],20,1163531501);o=md5_gg(o,n,m,l,p[g+13],5,-1444681467);l=md5_gg(l,o,n,m,p[g+2],9,-51403784);m=md5_gg(m,l,o,n,p[g+7],14,1735328473);n=md5_gg(n,m,l,o,p[g+12],20,-1926607734);o=md5_hh(o,n,m,l,p[g+5],4,-378558);l=md5_hh(l,o,n,m,p[g+8],11,-2022574463);m=md5_hh(m,l,o,n,p[g+11],16,1839030562);n=md5_hh(n,m,l,o,p[g+14],23,-35309556);o=md5_hh(o,n,m,l,p[g+1],4,-1530992060);l=md5_hh(l,o,n,m,p[g+4],11,1272893353);m=md5_hh(m,l,o,n,p[g+7],16,-155497632);n=md5_hh(n,m,l,o,p[g+10],23,-1094730640);o=md5_hh(o,n,m,l,p[g+13],4,681279174);l=md5_hh(l,o,n,m,p[g+0],11,-358537222);m=md5_hh(m,l,o,n,p[g+3],16,-722521979);n=md5_hh(n,m,l,o,p[g+6],23,76029189);o=md5_hh(o,n,m,l,p[g+9],4,-640364487);l=md5_hh(l,o,n,m,p[g+12],11,-421815835);m=md5_hh(m,l,o,n,p[g+15],16,530742520);n=md5_hh(n,m,l,o,p[g+2],23,-995338651);o=md5_ii(o,n,m,l,p[g+0],6,-198630844);l=md5_ii(l,o,n,m,p[g+7],10,1126891415);m=md5_ii(m,l,o,n,p[g+14],15,-1416354905);n=md5_ii(n,m,l,o,p[g+5],21,-57434055);o=md5_ii(o,n,m,l,p[g+12],6,1700485571);l=md5_ii(l,o,n,m,p[g+3],10,-1894986606);m=md5_ii(m,l,o,n,p[g+10],15,-1051523);n=md5_ii(n,m,l,o,p[g+1],21,-2054922799);o=md5_ii(o,n,m,l,p[g+8],6,1873313359);l=md5_ii(l,o,n,m,p[g+15],10,-30611744);m=md5_ii(m,l,o,n,p[g+6],15,-1560198380);n=md5_ii(n,m,l,o,p[g+13],21,1309151649);o=md5_ii(o,n,m,l,p[g+4],6,-145523070);l=md5_ii(l,o,n,m,p[g+11],10,-1120210379);m=md5_ii(m,l,o,n,p[g+2],15,718787259);n=md5_ii(n,m,l,o,p[g+9],21,-343485551);o=safe_add(o,j);n=safe_add(n,h);m=safe_add(m,f);l=safe_add(l,e)}return Array(o,n,m,l)}function md5_cmn(h,e,d,c,g,f){return safe_add(bit_rol(safe_add(safe_add(e,h),safe_add(c,f)),g),d)}function md5_ff(g,f,k,j,e,i,h){return md5_cmn((f&k)|((~f)&j),g,f,e,i,h)}function md5_gg(g,f,k,j,e,i,h){return md5_cmn((f&j)|(k&(~j)),g,f,e,i,h)}function md5_hh(g,f,k,j,e,i,h){return md5_cmn(f^k^j,g,f,e,i,h)}function md5_ii(g,f,k,j,e,i,h){return md5_cmn(k^(f|(~j)),g,f,e,i,h)}function safe_add(a,d){var c=(a&65535)+(d&65535);var b=(a>>16)+(d>>16)+(c>>16);return(b<<16)|(c&65535)}function bit_rol(a,b){return(a<<b)|(a>>>(32-b))};

function returnParam(instr) {
    Request = {
        QueryString : function(item) {
            var svalue = location.search.match(new RegExp("[\?\&]" + item + "=([^\&]*)(\&?)", "i"));
            return svalue ? svalue[1] : svalue;
        }
    }
    return Request.QueryString(instr);
}
//转换为UNIX时间戳
function strtotimestamp(datestr) {
    var new_str = datestr.replace(/:/g, "-");
    new_str = new_str.replace(/ /g, "-");
    var arr = new_str.split("-");
    var datum = new Date(Date.UTC(arr[0], arr[1] - 1, arr[2], arr[3] - 8, arr[4], arr[5]));

    return (datum.getTime());
}
//UNIX时间戳转换为字符串
function timestamptostr(timestamp) {
    d = new Date(timestamp.getTime());
    var jstimestamp = (d.getFullYear()) + "-" + (d.getMonth() + 1) + "-" + (d.getDate()) + " " + (d.getHours()) + ":" + (d.getMinutes()) + ":" + (d.getSeconds());
    return jstimestamp;
}

function sub_html_str(str, num)
{
    var reg = new RegExp( '<[^>]+>' , 'g' );
    var rt, rts = [], indexs = [], tstr, endstr, rstr, endtag;
 
    //提取所有的html标签和标签在字符串中的位置 
    while ( ( rt = reg.exec(str) ) != null )
    {
        rts.push(rt[0]);
        indexs.push(rt['index']);
    }
 
    //删除字符串中所有的html标签
    str = str.replace(reg, '');
    //对剩余的纯字符串进行substr
    tstr = str.substr(0, num);
 
    //判断有没有把实体腰斩，如果有腰斩的就再接上 
    endstr = (/&[^&]*$/.exec(tstr) || '');
    if ( endstr !== '' ) endstr += '' + (/^[^;]*;/.exec(str.substr(num, str.length)) || '');
    if (/^(&\w{1,10};|&#\d+;)$/.test(endstr))
    {
        str = tstr.replace(/&[^&]*$/, endstr);
    }
    else
    {
        str = tstr;
    }
 
    //把html标签放回到截断完毕的字符串中，当然有的html标签这时候已经无家可归了
 
    var index = 0;
 
    for (var i = 0; i < rts.length; i ++)
    {
        index = indexs[i];
        if (str.length >= index)
        {
            str = str.substr(0, index) + rts[i] + str.substr(index, str.length);
        } 
        else
        {
            break;
        }
    }
    var lastindex = i ;
    //下面的代码用来闭合没有闭合的标签
    tstr = str;
    rstr = '';
 
    //把闭合的标签全部删除，tstr包含了没有闭合的标签
    while ( rstr != tstr )
    {
        rstr = tstr;
        tstr = tstr.replace(/<[^>]+>[^<]*<\/[^>]+>/g, '').replace(/<[^>]+ \/>/g, '');
    }
 
    reg.lastIndex = 0;
    //如果存在没有闭合的标签，从无家可归的标签里找下半身
    while ( reg.exec(rstr) != null ) 
    {
        while ( lastindex < rts.length )
        {
            endtag = rts[lastindex];
            //如果它是一个用来闭合的标签,就把它追加到字符串
            if ( /^<[ ]*\//.test(endtag) ) 
            {
                str = str + endtag;
                lastindex ++;
                break;
            }
            //如果它是自闭合的标签
            else if ( /<[^>]+ \/>/.test(endtag) )
            {
                lastindex ++;
            }
            //如果它是一个起始标签
            else
            {
                lastindex += 2;
            }
 
        }
    }
 
    return str;
}

//显示 提交数据中,请稍候...   
function showTip(){
    var w_w = $(window).width();
    var w_h = $(window).height(); 
    var d_w = $(document).width();
    var d_h = $(document).height();           
    $("#background").css("width",w_w);           
    if( w_h > d_h ){
        $("#background").css("height",w_h);                
    }
    else{
        $("#background").css("height",d_h);                
    } 
    $("#progressBar").text("提交数据中,请稍候...");            
    $("#progressBar").center();          
    $("#background").css("opacity","0.5");
    $("#background,#progressBar").fadeIn();
}
//隐藏显示
function hideTip(){        
    $("#background,#progressBar").fadeOut();            
}

function getWeek(indate){
	var b=indate.split('-');
	var T=new Date(b[0],b[1]-1,b[2]);
	var out=T.getDay();//2009-10-17日期转换为星期几out	
	if (out=="0")
		return "星期日";
	if (out=="1")
		return "星期一";
	if (out=="2")
		return "星期二";
	if (out=="3")
		return "星期三";
	if (out=="4")
		return "星期四";
	if (out=="5")
		return "星期五";
	if (out=="6")
		return "星期六";
	
}	


function jquerySetCookie(name, value, options){
				    if (typeof value != 'undefined') { 
	        options = options || {};
	        if (value === null) {
	            value = '';
	            options.expires = -1;
	        }
	        var expires = '';
	        if (options.expires && (typeof options.expires == 'number' || options.expires.toUTCString)) {
	            var date;
	            if (typeof options.expires == 'number') {
	                date = new Date();
	                date.setTime(date.getTime() + (options.expires * 24 * 60 * 60 * 1000));
	            } else {
	                date = options.expires;
	            }
	            expires = '; expires=' + date.toUTCString(); 
	        }
	        var path = options.path ? '; path=' + (options.path) : '';
	        var domain = options.domain ? '; domain=' + (options.domain) : '';
	        var secure = options.secure ? '; secure' : '';
	        document.cookie = [name, '=', encodeURIComponent(value), expires, path, domain, secure].join('');
	    } else { 
	        var cookieValue = null;
	        if (document.cookie && document.cookie != '') {
	            var cookies = document.cookie.split(';');
	            for (var i = 0; i < cookies.length; i++) {
	                var cookie = jQuery.trim(cookies[i]);
	                if (cookie.substring(0, name.length + 1) == (name + '=')) {
	                    cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
	                    break;
	                }
	            }
	        }
	        return cookieValue;
	    }
		}
		
		
		
		var market_user_id_verify = jquerySetCookie("market_user_id");	
		if (market_user_id_verify!=undefined && market_user_id_verify!=null){
			jquerySetCookie("market_user_id",jquerySetCookie("market_user_id"),{expires: 1});
			jquerySetCookie("market_user_ticket",jquerySetCookie("market_user_ticket"),{expires: 1});	
			jquerySetCookie("market_display_name",jquerySetCookie("market_display_name"),{expires: 1});	
			jquerySetCookie("market_login_name",jquerySetCookie("market_login_name"),{expires: 1});	
		}else{
			window.location.href = "error_404.html";	
		}