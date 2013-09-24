centos-product-deploy.sh		--product environment deployment script
centos-test-deploy.sh		--test environment deployment script

dependencies:
(1)${NotificationServerHome}/libs/Zend.tar.gz
(2)${NotificationServerHome}/notifications-server-assembly/target/notifications-server-dist.tar.gz
(3)${NotificationServerHome}/notifications_php_web/target/notifications_php_web-dist.tar.gz	

pre-requirements:
(1)apache web server 2.2.x and more than edition has been deployed by YUM or APT-GET tool and configured proxy feature.
(2)installed Redis server 2.4.x and more than to the same host and configured Apache module(phpredis).
(3)installed MongoDB server 2.0.4 and more than to the same host.
(4)installed PHP 5.3.x and more than.

about the above installation, please refer to http://192.168.1.249/OMS_Sync/030_Deploy/Notification/Notification_Server.

step 1, upload deployment script and building target packages to /home/${user}/notifications direcotry.
step 2, execute the script to deploy.
