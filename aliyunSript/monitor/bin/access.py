#!/usr/bin/python

import os
import httplib, urllib
import StringIO, gzip, json
import hashlib,base64
import string
import time
import logging
import logger

import sys
reload(sys)
sys.setdefaultencoding("utf8")

class Access:
    def httpCall(self, data, apiurl):       
        compresseddata = ""
        try:
            f = urllib.urlopen(
                url = "http://api.borqs.com" + apiurl,
                data = urllib.urlencode(data)   
            )
            
            compresseddata = f.read()
            compressedstream = StringIO.StringIO(compresseddata)        
            gzipper = gzip.GzipFile(fileobj=compressedstream)       
            data = gzipper.read()             
            if data.find("10001") == -1:
                fp = '../logs/' + time.strftime('%Y%m%d%H%M%S',time.localtime(time.time()))
                os.system("echo ---------------- gcutil -----------------  > " + fp)
                os.system("jstat -gcutil `cat ~/.bpid/main.pid` >> " + fp)
                os.system("echo ---------------- gccapacity ------------- >> " + fp)
                os.system("jstat -gccapacity `cat ~/.bpid/main.pid` >> " + fp)
                os.system("echo ---------------- compilation ------------ >> " + fp)
                os.system("jstat -printcompilation `cat ~/.bpid/main.pid` 1000 10 >> " + fp)
                os.system("echo ---------------- thread stack ----------- >> " + fp)
                os.system("jstack  -l `cat ~/.bpid/main.pid` >> " + fp)
                os.system("echo ---------------- user occupy cpu ----------- >> " + fp)
                os.system('top -b -n 1 |grep Cpu | cut -d "," -f 1 | cut -d ":" -f 2 >> ' + fp)
                os.system("echo ---------------- system occupy cpu ----------- >> " + fp)
                os.system('top -b -n 1 |grep Cpu | cut -d "," -f 2 >> ' + fp)
                os.system("echo ---------------- memory use ----------- >> " + fp)
                os.system('top -b -n 1 |grep Mem | cut -d "," -f 1 | cut -d ":" -f 2 >> ' + fp)
                os.chdir("/home/wutong/workWT/")
                os.system("./kill_server.py")
                os.system("./restart_server.py")
        except:                                   
            content = "Except occured, restart account server now!\n"; 
            logger.init_config(path='../logs/', filename='access.log')
            logging.info(content)                       
            # file_object = open('../logs/access.log', 'a')
            # file_object.writelines(content)
            # file_object.close()
            fp = '../logs/' + time.strftime('%Y%m%d%H%M%S',time.localtime(time.time()))
            os.system("echo ---------------- gcutil -----------------  > " + fp)
            os.system("jstat -gcutil `cat ~/.bpid/main.pid` >> " + fp)
            os.system("echo ---------------- gccapacity ------------- >> " + fp)
            os.system("jstat -gccapacity `cat ~/.bpid/main.pid` >> " + fp)
            os.system("echo ---------------- compilation ------------ >> " + fp)
            os.system("jstat -printcompilation `cat ~/.bpid/main.pid` 1000 10 >> " + fp)
            os.system("echo ---------------- thread stack ----------- >> " + fp)
            os.system("jstack  -l `cat ~/.bpid/main.pid` >> " + fp)
            os.system("echo ---------------- user occupy cpu ----------- >> " + fp)
            os.system('top -b -n 1 |grep Cpu | cut -d "," -f 1 | cut -d ":" -f 2 >> ' + fp)
            os.system("echo ---------------- system occupy cpu ----------- >> " + fp)
            os.system('top -b -n 1 |grep Cpu | cut -d "," -f 2 >> ' + fp)
            os.system("echo ---------------- memory use ----------- >> " + fp)
            os.system('top -b -n 1 |grep Mem | cut -d "," -f 1 | cut -d ":" -f 2 >> ' + fp)
            os.chdir("/home/wutong/workWT/")
            os.system("./kill_server.py")
            os.system("./restart_server.py")   
            
        return data

 
if __name__ == "__main__":
    apiurl = "/account/who"
    data = {"login": "jcsheng86@sina.com"}
    test = Access()
    while True:         
        test.httpCall(data, apiurl)
        time.sleep(60)
