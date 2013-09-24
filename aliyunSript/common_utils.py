#!/usr/bin/python

import datetime
import time
import StringIO
import MySQLdb
import smtplib
import email.MIMEText
import email.Charset
import os
import subprocess

def yesterday():
    d = datetime.date.today() - datetime.timedelta(1)
    return (d.year, d.month, d.day)

def ts_to_str(ts):
    return datetime.datetime.fromtimestamp(ts / 1000.0).strftime('%Y-%m-%d %H:%M:%S')

def ymd_to_ts(ymd):
    return int(time.mktime(datetime.datetime(ymd[0], ymd[1], ymd[2]).timetuple())) * 1000

def ymd_to_str(ymd):
    return '%s-%s-%s' % ymd
    
def arch(a):
    return 'arm' if a == 1 else 'x86'

def archcode(s):
    return '1' if s == 'arm' else '0'

def save_text(f, text):
    try:
        f = open(f, 'w')
        f.write(text)
        return True
    except:
        return False
    finally:
        if f:
            f.close()   
        
        
def send_mail(subject, content, to_list, host, user, password, is_ssl=False):
    me = 'Borqs Server Reporter<%s>' % (user)
  
    email.Charset.add_charset('utf-8', email.Charset.SHORTEST, None, None)
    msg = email.MIMEText.MIMEText(content, 'html', _charset="UTF-8")
    msg['Subject'] = subject
    msg['From'] = me
    msg['To'] = ";".join(to_list)
    try:
        s = None
        if(is_ssl):
            s = smtplib.SMTP_SSL(host, 465)
        else:
            s.connect(host)
            s.ehlo()
            s.starttls()
            s.ehlo()
        s.login(user, password)
        s.sendmail(me, to_list, msg.as_string())
        return True
    except Exception, e:
        raise e
        #return False
    finally:
        if s:
            s.close()

def mysql_query(host, user, pwd, database, sql):
    db = None
    try:
        db = MySQLdb.connect(host=host, user=user, passwd=pwd, db=database, init_command='set names utf8', charset='utf8')
        cur = db.cursor(MySQLdb.cursors.DictCursor)
        cur.execute(sql)
        return cur.fetchall()
    finally:
        if db:
            db.close()

def mysql_exec(host, user, pwd, database, sql):
    db = None
    try:
        db = MySQLdb.connect(host=host, user=user, passwd=pwd, db=database, init_command='set names utf8', charset='utf8')
        cur = db.cursor(MySQLdb.cursors.DictCursor)
        cur.execute(sql)
        db.commit()
    except Exception, e:
        raise e 
    finally:
        if db:
            db.close()

def execCmd(cmd):
    handle = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    return handle.communicate()[0]
