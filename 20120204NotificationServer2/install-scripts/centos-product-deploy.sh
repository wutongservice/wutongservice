#!/bin/sh

MHOME=/root/notifications
SRVPATH=$MHOME/notifications-server
WEBPATH=$MHOME/notifications_php_web
ZENDPATH=$MHOME/Zend

SRVPACKFILE=notifications-server-dist.tar.gz
WEBPACKFILE=notifications_php_web-dist.tar.gz
ZENDPACKFILE=Zend.tar.gz

WEBCONFILE=bmb.conf

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
		
		echo "remove $SRVPATH directory"
		sudo rm -R $SRVPATH
	else
		echo "not found $SRVPATH."
	fi

	if [ -e $WEBPATH ]; then
		echo "remove $WEBPATH directory"
		sudo rm -R $WEBPATH
	fi
else
	echo "not found $MHOME!"
	exit 1
fi

# uncompress notifications application packages
cd $MHOME
if [ -e "$MHOME/$WEBPACKFILE" ]; then
	echo "starting to uncompress notifications php web package."
	sudo tar zxvf $WEBPACKFILE
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
	sudo tar zxvf $SRVPACKFILE
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
	sudo tar zxvf $ZENDPACKFILE
	if [ -e $ZENDPATH ]; then
		echo "succeed to uncompress Zend framework liberary package."
		sudo ln -s $ZENDPATH  $WEBPATH/library/Zend
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
cp notifications_php_web/bmb.conf notifications_php_web/bmb.conf.bak
#sed 's/development/production/g' notifications_php_web/bmb.conf.bak | sed "s/\/home\/liuchunrong\/workspace\/notifications/$CMHOME/g" | sed 's/8580/9580/g' > notifications_php_web/bmb.conf
sed 's/development/production/g' notifications_php_web/bmb.conf.bak | sed "s/\/home\/liuchunrong\/workspace\/notifications/$CMHOME/g" > notifications_php_web/bmb.conf
cat notifications_php_web/bmb.conf

#cp notifications-server/conf/product.properties notifications-server/conf/product.properties.bak
#sed 's/8083/9083/g' notifications-server/conf/product.properties.bak > notifications-server/conf/product.properties
cat notifications-server/conf/product.properties

cp notifications-server/conf/applicationContext.xml notifications-server/conf/applicationContext.xml.bak
sed 's/local.properties/product.properties/g' notifications-server/conf/applicationContext.xml.bak |sed "s/file:./file:$CMHOME\/notifications-server/g" > notifications-server/conf/applicationContext.xml
cat notifications-server/conf/applicationContext.xml

cp -f notifications-server/avro-service notifications-server/avro-service.bak
sed 's/JAVA_HOME_REAL=\/usr\/lib\/jvm\/java-6-openjdk/JAVA_HOME_REAL=\/usr\/local\/jdk1.6/g' notifications-server/avro-service.bak > notifications-server/avro-service

cp -f notifications-server/thrift-service notifications-server/thrift-service.bak
sed 's/JAVA_HOME_REAL=\/usr\/lib\/jvm\/java-6-openjdk/JAVA_HOME_REAL=\/usr\/local\/jdk1.6/g' notifications-server/thrift-service.bak > notifications-server/thrift-service

echo "mofidy privilege of application application......"
sudo chmod -R +x $SRVPATH
sudo chmod -R +x $WEBPATH

# startup avro and thrift services
cd $SRVPATH
echo "change to directory $SRVPATH"
pwd
./avro-service start
./thrift-service start

# configure and restart apache service
APACHECONFPATH=/etc/httpd/conf.d
TGTCONFILE=$APACHECONFPATH/$WEBCONFILE
if [ -e $TGTCONFILE ]; then
	echo "remove old $TGTCONFILE file."
	sudo rm $TGTCONFILE
fi

sudo cp $WEBPATH/bmb.conf $TGTCONFILE
if [ -e $TGTCONFILE ]; then
	echo "finished notification webapp configuration."
else
	echo "failed to configure notification webapp!"
	exit 1
fi
sudo service httpd restart
