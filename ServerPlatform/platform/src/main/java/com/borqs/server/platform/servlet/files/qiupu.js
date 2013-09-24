var qiupuip="http://api.borqs.com/",
    notifiapi="http://api.borqs.com/bmb/service/informations/";
//discuz switch
var ForDiscuz = false;

function prepareData(jdata)
{
	//sort the keys
	var keys = [];
	for (var key in jdata)
		keys.push(key); //.toLowerCase() removed by liuchengtao
	keys.sort(function(a, b){
		return a > b ? 1 : a < b ? -1 : 0;
	});

	var src = "";
	for (var i=0; i < keys.length; i++) 
		src += keys[i];
	jdata['ticket'] = $.cookie('ticket');
	jdata['appid'] = "1";
	jdata['sign'] = getSignData(src);

	return jdata;
}

function invokeApi1(cmd, jdata, call)
{
	$.getJSON("http://apitest.borqs.com/"+cmd+"?callback=?", jdata, call);
}
function invokeApi(cmd, jdata, call)
{	
	$.getJSON(qiupuip+cmd+"?callback=?", jdata, function(ret){
		/*if (ret["error_code"] == 106) {
			// server will send 106 error if it think the ticket invalid.
			// so must return to login page when ever got such error.
			$.cookie('user_id', '');
			$.cookie('ticket', '');
			$.cookie('display_name', '');
			$.cookie('login_name', '');
			window.location = "login.html";
		}
		else */call(ret);
	});
}
function invokeBBSApi(cmd, jdata, call)
{
	$.getJSON("http://bbs.borqs.com/"+cmd+"?callback=?", jdata, call);
}
function invokeNotifiApi(cmd, jdata, call)
{	
	var cmdstring = "ticket=" + $.cookie('ticket');	                
	for (var key in jdata){
	    cmdstring = cmdstring+"&"+key+"="+jdata[key];	
	}		
	$.getJSON(notifiapi+cmd+"?"+cmdstring+"&callback=?",call);	          	          
}
function getPadding(src) {
	return "appSecret1" + src + "appSecret1";
}

function getSignData(src) {	
	return b64_md5(getPadding(src));
}

function getRequestParam(name){
      var params=location.search.substring(1).toLowerCase();
      var paramList=[];
      var param=null;
      var parami;
      if(params.length>0) {
             if(params.indexOf("&") >=0) {  // >=2 parameters
                paramList=params.split( "&" );
             }else {                        // 1 parameter
                paramList[0] = params;
             }
             for(var i=0,listLength = paramList.length;i<listLength;i++) {
                 parami = paramList[i].indexOf(name+"=" );
                 if(parami>=0) {
                     param =paramList[i].substr(parami+(name+"=").length); //get value
                     break;
                 }
             }
       }
       return param;
}

//图片按比例缩放,可输入参数设定初始大小
function resizeimg(ImgD, iwidth, iheight) {
	var image = new Image();
	image.src = ImgD.src;
	if(image.width > 0 && image.height > 0) {
		if(image.width / image.height >= iwidth / iheight) {
			if(image.width > iwidth) {
				ImgD.width = iwidth;
				ImgD.height = (image.height * iwidth) / image.width;
			} else {
				ImgD.width = iwidth;
				ImgD.height = iheight;
				//ImgD.width=image.width;
				// ImgD.height=image.height;
			}
			ImgD.alt = image.width + "×" + image.height;
		} else {
			if(image.height > iheight) {
				ImgD.height = iheight;
				ImgD.width = (image.width * iheight) / image.height;
			} else {
				ImgD.width = iwidth;
				ImgD.height = iheight;
				//   ImgD.width=image.width;
				//   ImgD.height=image.height;
			}
			ImgD.alt = image.width + "×" + image.height;
		}
		//　　　　ImgD.style.cursor= "pointer"; //改变鼠标指针
		//　　　　　ImgD.onclick = function() { window.open(this.src);} //点击打开大图片
		/*　　　　if (navigator.userAgent.toLowerCase().indexOf("ie") > -1) { //判断浏览器，如果是IE
		 　　　　　　ImgD.title = "请使用鼠标滚轮缩放图片，点击图片可在新窗口打开";
		 　　　　　　ImgD.onmousewheel = function img_zoom() //滚轮缩放
		 　　　　　 {
		 　　　　　　　　　　var zoom = parseInt(this.style.zoom, 10) || 100;
		 　　　　　　　　　　zoom += event.wheelDelta / 12;
		 　　　　　　　　　　if (zoom> 0)　this.style.zoom = zoom + "%";
		 　　　　　　　　　　return false;
		 　　　　　 }
		 　　　  } else { //如果不是IE
		 　　　　　　　     ImgD.title = "点击图片可在新窗口打开";
		 　　　　　　   }
		 }
		 */
	}
}


//some common resources. please put only common resources here.
var resMainPage = new Array(
		'Main Page',
		'主页',
		'página principal',
		'メインページ');

var resLastPage = new Array(
		'You got last Page',
		'最后一页了',
		'você tem a última página',
		'あなたは最後のページを持って');

var resDownload = new Array(
		'Download',
		'下载',
		'baixar',
		'ダウンロード');

var resAddComments = new Array(
		'Add Comments',
		'评论',
		'Adicionar comentários',
		'コメントを追加');
		
var resNavlnkhome  = new Array('Home',
                               '主页',
                               'casa',
                               'ホーム');
                                				
var resNavlnknotice  = new Array('Notice',
                                 '消息',
                                 'notar',
                                 '気付く');                                				
                                				                                				                                   
var resNavlnkforum = new Array('Forum',
                                '论坛',
                                'fórum',
                                'フォーラム');
                               					                                					 
var resNavlnkapps = new Array('Application Pack',
                               '应用宝盒',
                               'aplicação Pack',
                               'アプリケーション');                               					 
                               					 
var resNavlnkemail = new Array('Contact Us',
                               '联系我们',
                               'Fale Conosco',
                               'お問い合わせ');                               					 
                               					 
var resNavlnklogout = new Array('Logout',
                                '退出',
                                'sair',
                                'ログアウト');                               					 
                               					                                                                           
var resNavlnksearch = new Array(
		                     'Search Username',
		                     '查找用户名',
		                     'Pesquisar usuário',
		                     'ユーザーの検索');    
		              
var resPhoneNavTitle = new Array(
		                       'English',
		                       '中文',
		                       'português',
		                       '日本语'); 
		                       
var resDetailTitle = new Array(
		                       'More',
		                       '更多',
		                       'mais',
		                       'より多くの'); 		                       
	                                                       
var rescircleNames = new Array( ['Default','Family','Closed Friends','Acquaintance'],
                                ['default','family','closed friends','acquaintance'],
                                ['关注','家人','挚友','熟人'],
                                ['atenção','família','amigo','conhecimento'],
                                ['注意','ファミリー','親友','知人']);
                                             
var resFriendtitle = new Array(
                                 'friend',
                                 '好友', 		                          
  		                          'amigo',
  		                          '友達'
  		                         );

var resFollowertitle = new Array(
                                  'fans',
                                  '粉丝',	                          
  		                          'seguidores',
  		                          'フォロワー'
  		                         );
  		                                             
var resCircletitle = new Array(
                                  'My Circles',
                                  '我的圈子',	                          
  		                          'Meu círculo de',
  		                          '私のサークルの'
  		                         ); 
  		                         
var resCircleCreate = new Array(
                                  'new circle',
                                  '新建圈子',	                          
  		                          'novo círculo',
  		                          '新しいサークル'
  		                         );   		                         

  		                         
var resCheckAllorNot = new Array(
                                  'Select All/Clear All',
                                  '全选/反选',	                          
  		                          'Selecionar Todos/Limpar tudoo',
  		                          'すべてを選択/すべてをクリアします'
  		                         );  		                         
  		                            
  		                         
var resChooseFriend = new Array(
                                  'Select friends',
                                  '选择好友',	                          
  		                          'Selecione um amigo',
  		                          '友人を選択します'
  		                         );  
  		                         
var resAddCircleBtn = new Array(
                                  'Add',
                                  '增加',	                          
  		                          'adicionar',
  		                          '加える'
  		                         );  	
  		                         	                         
var resfilterFriend = new Array(
                                  'Filter...',
                                  '查找...',	                          
  		                          'Filtro...',
  		                          'フィルタ...'
  		                         ); 
  		                                                                                     
var resTimeLineTabs = new Array(['Friends\' Post','My Post','Others\' Post','Hot Post'],
                                ['好友动态','我的动态','其他人','热门关注'],
                                ['amigos liberados','meu post','Outros liberar','liberação Popular'],
                                ['リリースフレンズ','リリース','公開される他の','人気のあるリリース']);
var resPhoneTimeLineTabs = new Array(['Friends\' Post','Others\' Post','Notice'],
                                ['好友动态','其他人','消息'],
                                ['amigos liberados','Outros liberar','notar'],
                                ['リリースフレンズ','公開される他の','気付く']);                                
 
var resCircleTabs = new Array(['Circle dynamic','Circle members'],
                                ['圈内动态','圈内成员'],
                                ['círculo dinâmica','círculo dos membros'],
                                ['サークルダイナミック','のメンバーのサークル']); 
               
var resNoticeTitle = new Array(
		                      'list of messages',
		                      '消息列表',
		                      'lista de notificação',
		                      '通知リスト');                             
                                
var resNoticeTabs = new Array(['Unread notice','Read notice'],
                                ['未读消息','已读消息'],
                                ['Notificação não lida','Leia aviso'],
                                ['未読メッセージ','メッセージを読む']);   		                         
  		                         
		
var resSuggestionText = new Array(
		                         'People you may know',
		                         '可能感兴趣的',
		                         'pode estar interessado em',
		                         'に興味がある可能性があり');  	
	
var resChooseCircle = new Array(
                                'Select circle',
                                '选择圈子',	                          
  		                        'Selecione o círculo',
  		                        '選択する'
  		                       );   
  		                       
var resDeleteCircle = new Array(
                                'Sure you want to delete this circle (the inner members will be deleted)?',
                                '确定删除此圈子（圈子内成员也会被删除)？',	                          
  		                        'Claro que você quer apagar este círculo (o círculo interno de membros serão excluídos)?',
  		                        '確かに、このサークル（メンバーの側近グループが削除されます）を削除したいですか？'
  		                       );    		                       
  		                         	
var resDeleteFriend = new Array(
                                'Remove friends from the circle?',
                                '确定取消对好友的关注？',	                          
  		                        'Remover amigo do círculo?',
  		                        'サークルから友人を削除します？'
  		                       );   	
  		                       
var resLocation = new Array(
        'Location',
        '位置',
        'localização',
        '場所');  
var resPrivate = new Array(
        'Private',
        '私密',
        'privado',
        'プライベート');  
var resPost = new Array(
        'Post',
        '发布',
        'solte',
        'リリース');  

var resInputmax = new Array(
        'Has reached the maximum input words',
        '已达到最大输入字数',
        'Chegou às palavras de entrada máximas',
        '最大入力ワードに達しました');		

var resaddRec = new Array(
  		'Select friends or direct input',
  		'选择好友或直接输入',
  		'Seleccione amigos ou entrada direta',
  		'友人を選択するか、または直接入力'
  		);
  		
//多语言支持，本地语言显示
function setLocaleResource() {
	//Top navigation
    $('#navlnkhome').html(resNavlnkhome[locale]);
    $('#navlnknotice').html(resNavlnknotice[locale]);    
    $('#navlnkforum').html(resNavlnkforum[locale]);    
    $('#navlnkapps').html(resNavlnkapps[locale]);
    $('#navlnkemail').html(resNavlnkemail[locale]);    
    $('#navlnklogout').html(resNavlnklogout[locale]);           
    $('#usersearchtxt').attr('placeholder',resNavlnksearch[locale]);
          
    //Phone- Top navigation
    $('#phonenavmain').html(resPhoneNavTitle[locale]);    
        
    //Tabs for timeline    
    $('#tabs li a').each(function(i,o) {
   	    $(this).html(resTimeLineTabs[locale][i]);
    });     
    $('#menutabs li').each(function(i,o) {
   	    $(this).html(resPhoneTimeLineTabs[locale][i]);
    }); 
    //Tabs for circles
    $('#tabcircles li a').each(function(i,o) {
   	    $(this).html(resCircleTabs[locale][i]);
    });  
    
    //Tabs for notifications
    $('#titlentfxs').html(resNoticeTitle[locale]);    
    $('#tabntfxs li a').each(function(i,o) {
   	    $(this).html(resNoticeTabs[locale][i]);
    });          
      
    $('#newcirclelink').html(resCircleCreate[locale]);
    
    $('#newcirclebtn').val(resAddCircleBtn[locale]);
    
    $('#filterkey').attr('placeholder',resfilterFriend[locale]);
    
    $('#checkallbtn').html(resCheckAllorNot[locale]);
     
    $('#suggesttitle').html(resSuggestionText[locale]);
    
    $('#location').html(resLocation[locale]);       
    
    $('#checkprivate').html(resPrivate[locale]); 
     
    $('#upbutton').val(resPost[locale]);
    $('#recadd').html(resaddRec[locale]);
    
}
   
                                                                                     
                                
