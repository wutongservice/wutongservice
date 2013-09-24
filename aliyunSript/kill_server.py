#!/usr/bin/python

from command import *
import os

res =  execCmd('ps -ef | grep "java -Xms128m -Xmx512m" | awk \'{print $2}\'')
arr = res.split('\n')
for pid in arr:
  execCmd('kill -9 ' + pid)