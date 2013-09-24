#!/usr/bin/py

from fabric.context_managers import cd
from fabric.operations import run, put

def deploy(tomcat_dir):
    with cd(tomcat_dir):
        run('bin/shutdown.sh', pty=False)
        put('./target/BorqsMarket.war', 'webapps/BorqsMarket.war')
        run('bin/startup.sh', pty=False)
