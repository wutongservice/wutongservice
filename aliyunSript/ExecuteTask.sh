#!/bin/bash
source /etc/profile
ps -ef | grep -E "(java|bsapp).*$1" | grep -v grep  | awk '{print $2}' | xargs kill -9
/home/wutong/workWT/dist-r3-distribution/bin/bsapp -Dlogback.configurationFile=/home/wutong/workWT/dist-r3-distribution/etc/production.logback.xml $1 /home/wutong/workWT/dist-r3-distribution/etc/production.config.properties
