#!/usr/bin/py

import time
from fabric.context_managers import cd
from fabric.operations import run, put

def deploy(tomcat_dir):
    with cd(tomcat_dir):
        run('bin/shutdown.sh', pty=False)
        for i in range(5):
            time.sleep(1)
            print "Wait %s ..." % (i + 1)
        run('rm -rf webapps/BorqsMarket')
        run('rm -rf work/Catalina')
        put('./target/BorqsMarket.war', 'webapps/BorqsMarket.war')
        run('bin/startup.sh', pty=False)
