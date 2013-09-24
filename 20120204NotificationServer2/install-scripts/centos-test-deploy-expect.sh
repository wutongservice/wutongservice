#!/bin/sh

SUDOEXP=$1
PASS=$2
RLINK=$(readlink -f $0)
MHOME=$(dirname "$RLINK")
SRVPATH=$MHOME/notifications-server
WEBPATH=$MHOME/notifications_php_web
ZENDPATH=$MHOME/Zend

SRVPACKFILE=notifications-server-dist.tar.gz
WEBPACKFILE=notifications_php_web-dist.tar.gz
ZENDPACKFILE=Zend.tar.gz

WEBCONFILE=bmb_9.conf

PID_AVRO=$SRVPATH/notifications-avro-service.pid
PID_THRIFT=$SRVPATH/notifications-thrift-service.pid

# remove old directory and files
# stop avro and thrift service
if [ -e $MHOME ]; then
	echo "$MHOME directory has exists."
	if [ -e $SRVPATH ]; then
		echo "$SRVPATH directory has exists."
		cd $SRVPATH
		pwd
		
		if [ -e $SRVPATH/avro-service ]; then
			echo "$SRVPATH/avro-service file has exists. stop the avro service!"
			./avro-service stop
			
			if [ -e $PID_AVRO ]; then
				echo "stop avro-service failed! exit."
				exit 1
			fi
		fi		
		
		if [ -e $SRVPATH/thrift-service ]; then
			echo "$SRVPATH/thrift-service file has exists. stop the thrift service!"
			./thrift-service stop
			if [ -e $PID_THRIFT ]; then
				echo "stop thrift-service failed! exit."
				exit 1
			fi
		fi
		cd $MHOME
		echo "remove $SRVPATH directory"
		echo --- $SUDOEXP $PASS "rm -R -f $SRVPATH"
		$SUDOEXP $PASS "rm -R -f $SRVPATH"
	else
		echo "not found $SRVPATH."
	fi

	if [ -e $WEBPATH ]; then
		echo "remove $WEBPATH directory"
		echo --- $SUDOEXP $PASS "rm -R -f $WEBPATH"
		$SUDOEXP $PASS "rm -R -f $WEBPATH"
	fi
else
	echo "not found $MHOME!"
	exit 1
fi

# uncompress notifications application packages
cd $MHOME
if [ -e "$MHOME/$WEBPACKFILE" ]; then
	echo "starting to uncompress notifications php web package."
	tar zxf $WEBPACKFILE
	if [ -e $WEBPATH ]; then
		echo "succeed to uncompress notification web package."
	else
		echo "failed to uncompress notification web package!"
		exit 1
	fi
else
	echo "not found $WEBPACKFILE file!"
	exit 1
fi

if [ -e "$MHOME/$SRVPACKFILE" ]; then
	echo 'starting to uncompress notifications service package.'
	tar zxf $SRVPACKFILE
	if [ -e $SRVPATH ]; then
		echo "succeed to uncompress notification service package."
	else
		echo "failed to uncompress notification service package!"
		exit 1
	fi
else
	echo 'not found $SRVPACKFILE file!'
	exit 1
fi

# uncompress Zend framework liberary packages
if [ -e "$MHOME/$ZENDPACKFILE" ]; then
	echo 'starting to uncompress Zend framework liberary package.'
	tar zxf $ZENDPACKFILE
	if [ -e $ZENDPATH ]; then
		echo "succeed to uncompress Zend framework liberary package."
		$SUDOEXP $PASS "ln -s $ZENDPATH  $WEBPATH/library/Zend"
	else
		echo "failed to uncompress Zend framework liberary package!"
		exit 1
	fi
else
	# todo check /usr/share/php/Zend directory
	echo 'not found $ZENDPACKFILE file!'
	exit 1
fi

# configure notification applications
echo "configure notification application enviroment......"
CMHOME=${MHOME//\//\\/}
pwd
$SUDOEXP $PASS "cp notifications_php_web/bmb.conf notifications_php_web/bmb.conf.bak"
$SUDOEXP $PASS "sed 's/development/testing/g' notifications_php_web/bmb.conf.bak | sed 's/\/home\/liuchunrong\/workspace\/notifications/$CMHOME/g' | sed 's/8580/9580/g' > notifications_php_web/bmb.conf"
#sed 's/development/testing/g' notifications_php_web/bmb.conf.bak | sed "s/\/home\/liuchunrong\/workspace\/notifications/$CMHOME/g" > notifications_php_web/bmb.conf
cat notifications_php_web/bmb.conf

$SUDOEXP $PASS "cp notifications-server/conf/test.properties notifications-server/conf/test.properties.bak"
$SUDOEXP $PASS "sed 's/8083/9083/g' notifications-server/conf/test.properties.bak | sed 's/8084/9084/g' > notifications-server/conf/test.properties"
cat notifications-server/conf/test.properties

$SUDOEXP $PASS "cp notifications-server/conf/applicationContext.xml notifications-server/conf/applicationContext.xml.bak"
$SUDOEXP $PASS "sed 's/local.properties/test.properties/g' notifications-server/conf/applicationContext.xml.bak |sed 's/file:./file:$CMHOME\/notifications-server/g' > notifications-server/conf/applicationContext.xml"
cat notifications-server/conf/applicationContext.xml

$SUDOEXP $PASS "cp notifications_php_web/application/configs/application.ini notifications_php_web/application/configs/application.ini.bak"
$SUDOEXP $PASS "sed 's/8084/9084/g' notifications_php_web/application/configs/application.ini.bak > notifications_php_web/application/configs/application.ini"

$SUDOEXP $PASS "cp -f notifications-server/avro-service notifications-server/avro-service.bak"
$SUDOEXP $PASS "sed 's/JAVA_HOME_REAL=\/usr\/lib\/jvm\/java-6-openjdk/JAVA_HOME_REAL=\/usr\/local\/jdk1.6/g' notifications-server/avro-service.bak > notifications-server/avro-service"

$SUDOEXP $PASS "cp -f notifications-server/thrift-service notifications-server/thrift-service.bak"
$SUDOEXP $PASS "sed 's/JAVA_HOME_REAL=\/usr\/lib\/jvm\/java-6-openjdk/JAVA_HOME_REAL=\/usr\/local\/jdk1.6/g' notifications-server/thrift-service.bak > notifications-server/thrift-service"

echo "mofidy privilege of application application......"
$SUDOEXP $PASS "chmod -R +x $SRVPATH"
$SUDOEXP $PASS "chmod -R +x $WEBPATH"


# startup avro and thrift services
cd $SRVPATH
echo "change to directory $SRVPATH"
pwd
./avro-service start
./thrift-service start


cd $MHOME

# configure and restart apache service
APACHECONFPATH=/etc/httpd/conf.d
TGTCONFILE=$APACHECONFPATH/$WEBCONFILE
if [ -e $TGTCONFILE ]; then
	echo "remove old $TGTCONFILE file."
	$SUDOEXP $PASS "rm $TGTCONFILE"
fi


$SUDOEXP $PASS "cp $WEBPATH/bmb.conf $TGTCONFILE"
if [ -e $TGTCONFILE ]; then
	echo "finished notification webapp configuration."
else
	echo "failed to configure notification webapp!"
	exit 1
fi

$SUDOEXP $PASS "service httpd restart" "root" "-"
