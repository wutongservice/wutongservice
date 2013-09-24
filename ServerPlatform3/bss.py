#!/usr/bin/python

import sys
import os

def print_usage():
    print 'Usage:'
    print '  bss package|clean|repack|dist|redist|doc [args]'

def make_doc():
    if sys.platform == 'cygwin':
        cmd = 'java -Dfile.encoding=UTF-8 -cp "`cygpath -w -a $JAVA_HOME`/lib/tools.jar;`cygpath -w -a .`/dist/target/dist-r2-distribution/lib/dist-r2-jar-with-dependencies.jar"'
    else:
        cmd = 'java -Dfile.encoding=UTF-8 -cp "$JAVA_HOME/lib/tools.jar:dist/target/dist-r2-distribution/lib/dist-r2-jar-with-dependencies.jar"'
        
    cmd += '  com.borqs.server.platform.web.doc.HttpApiDoclet '
    
    if sys.platform == 'cygwin':
        cmd += '"`cygpath -w -a .`/pubapi/src/main/java"'
    else:
        cmd += '"`pwd`/pubapi/src/main/java"'
        
    cmd += ' -ccom.borqs.server.platform.web.doc.html.HtmlOutput'
    
    if sys.platform == 'cygwin':
        cmd += ' -o"`cygpath -w -a .`/dist/target/dist-r2-distribution/doc"'
    else:
        cmd += ' -o"`pwd`/dist/target/dist-r2-distribution/doc"'
    print '============= make http api document ============='
    print cmd
    os.system(cmd)
    
def pack():
    os.system('mvn -Dfile.encoding=UTF-8 package -DskipTests=true')
    make_doc()

def clean():
    os.system('mvn -Dfile.encoding=UTF-8 clean')

def dist(argv):
    os.system('python ./dist/target/dist-r2-distribution/distscripts/dist.py ' + ' '.join(argv[2:]))

def main(argv):
    if len(argv) < 2:
        print_usage()
        return

    action = argv[1]
    if action == 'package':
        pack()
    elif action == 'clean':
        clean()
    elif action == 'repack':
        clean()
        pack()
    elif action == 'dist':
        dist(argv)
    elif action == 'redist':
        clean()
        pack()
        dist(argv)
    elif action == 'doc':
        make_doc()
    else:
        print_usage()

if __name__ == '__main__':
    main(sys.argv)
