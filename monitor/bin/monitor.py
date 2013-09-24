#!/usr/bin/python

import os
import time
import logging
import logger
from command import *

while True:
    content = "\nUser occupy cpu: "    
    content += execCmd('top -b -n 1 |grep Cpu | cut -d "," -f 1 | cut -d ":" -f 2')
    content += "\nSystem occupy cpu: "
    content += execCmd('top -b -n 1 |grep Cpu | cut -d "," -f 2')
    content += "\nMemory use: "   
    content += execCmd('top -b -n 1 |grep Mem | cut -d "," -f 1 | cut -d ":" -f 2')
    content += " "  
    content += execCmd('top -b -n 1 |grep Mem | cut -d "," -f 2')       
    pid = execCmd('netstat -anp | grep 8080 | awk \'{print $7}\'')
    index = pid.find('/')
    if index != -1:
        pid = pid[0:index]
        content += "Borqs web server cpu usage: " + execCmd('ps aux|grep ' + pid + '|grep -v "grep"|awk \'{print $3}\'')
        content += "Borqs web server mem usage: " + execCmd('ps aux|grep ' + pid + '|grep -v "grep"|awk \'{print $4}\'')
    logger.init_config(path='../logs/')
    logging.info(content)    
    # print content       
    time.sleep(10)

