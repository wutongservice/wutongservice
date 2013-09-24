@echo off

set code=%1
set service=NotificationAVROService
set curpath=%~dp0
set NotifiHome=.
set MainClass=com.borqs.information.rpc.service.AvroServiceLauncher
if %code%==start (
  if exist %service%.pid goto exit1
    prunsrv //ES//%service%    
) else if %code%==startc (
    prunsrv //TS//%service%
) else if %code%==stop (
    prunsrv //SS//%service%
) else if %code%==install (
    prunsrv //IS//%service% --DisplayName="%service%" --Startup=auto --Classpath=%NotifiHome%/lib/notifications-server-1.0.0.jar;%NotifiHome%/lib/mongo-java-driver-2.6.3.jar;%NotifiHome%/lib/commons-daemon-1.0.8.jar;%NotifiHome%/lib/activation-1.1.jar;%NotifiHome%/lib/activemq-all-5.5.0.jar;%NotifiHome%/lib/aopalliance-1.0.jar;%NotifiHome%/lib/asm-3.3.1.jar;%NotifiHome%/lib/asm-commons-3.3.1.jar;%NotifiHome%/lib/asm-tree-3.3.1.jar;%NotifiHome%/lib/aspectjrt-1.6.11.jar;%NotifiHome%/lib/aspectjweaver-1.6.11.jar;%NotifiHome%/lib/avalon-framework-4.1.3.jar;%NotifiHome%/lib/libthrift-0.7.0.jar;%NotifiHome%/lib/cglib-nodep-2.2.jar;%NotifiHome%/lib/commons-collections-3.1.jar;%NotifiHome%/lib/commons-dbcp-1.2.1.jar;%NotifiHome%/lib/commons-lang-2.1.jar;%NotifiHome%/lib/commons-logging-1.1.jar;%NotifiHome%/lib/commons-pool-1.2.jar;%NotifiHome%/lib/dom4j-1.6.1.jar;%NotifiHome%/lib/hamcrest-core-1.1.jar;%NotifiHome%/lib/icu4j-2.6.1.jar;%NotifiHome%/lib/jackson-core-asl-1.7.3.jar;%NotifiHome%/lib/jackson-core-lgpl-1.8.3.jar;%NotifiHome%/lib/jackson-mapper-asl-1.7.3.jar;%NotifiHome%/lib/jackson-mapper-lgpl-1.8.3.jar;%NotifiHome%/lib/jars.txt;%NotifiHome%/lib/jaxb-api-2.2.4.jar;%NotifiHome%/lib/jaxb-impl-2.2.4.jar;%NotifiHome%/lib/jdom-1.1.jar;%NotifiHome%/lib/jetty-6.1.26.jar;%NotifiHome%/lib/jetty-util-6.1.26.jar;%NotifiHome%/lib/joda-time-1.6.2.jar;%NotifiHome%/lib/json-20080701.jar;%NotifiHome%/lib/junit-4.10.jar;%NotifiHome%/lib/log4j-1.2.15.jar;%NotifiHome%/lib/logkit-1.0.1.jar;%NotifiHome%/lib/mysql-connector-java-5.1.17.jar;%NotifiHome%/lib/netty-3.2.4.Final.jar;%NotifiHome%/lib/notifications-thrift-api-1.2.0.jar;%NotifiHome%/lib/paranamer-2.3.jar;%NotifiHome%/lib/servlet-api-2.3.jar;%NotifiHome%/lib/servlet-api-2.5-20081211.jar;%NotifiHome%/lib/slf4j-api-1.5.8.jar;%NotifiHome%/lib/slf4j-log4j12-1.5.8.jar;%NotifiHome%/lib/snappy-java-1.0.3.2.jar;%NotifiHome%/lib/spring-aop-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-asm-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-aspects-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-beans-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-context-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-context-support-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-core-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-expression-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-jdbc-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-jms-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-oxm-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-test-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-tx-3.0.6.RELEASE.jar;%NotifiHome%/lib/spring-web-3.0.6.RELEASE.jar;%NotifiHome%/lib/stax-api-1.0-2.jar;%NotifiHome%/lib/stax-api-1.0.1.jar;%NotifiHome%/lib/velocity-1.7.jar;%NotifiHome%/lib/wstx-asl-3.2.7.jar;%NotifiHome%/lib/xalan-2.6.0.jar;%NotifiHome%/lib/xercesImpl-2.0.2.jar;%NotifiHome%/lib/xml-apis-1.0.b2.jar;%NotifiHome%/lib/xmlParserAPIs-2.6.2.jar;%NotifiHome%/lib/xom-1.0.jar;%NotifiHome%/lib/xpp3-1.1.4c.jar --Install=%curpath%prunsrv.exe --Jvm=auto --StartMode=jvm --StopMode=jvm --StartClass=%MainClass% --StartMethod=winstart --StartParams= --StopClass=%MainClass% --StopMethod=winstop --StopParams= --LogPath=. --PidFile=%service%.pid --LogPrefix=%service% 
) else if %code%==uninstall (
    prunsrv //DS//%service%
) else if %code%==mng (
    start /B Prunmgr //ES//%service%
) else (
    echo usage: %0 [install][uninstall][start][stop][mng]   
)
goto exit

:exit1
echo Service %service% is already running
goto exit

:exit
echo bye!