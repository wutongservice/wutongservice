from __future__ import unicode_literals

__author__ = 'rongxin.gao@borqs.com'

import random
import time
import datetime
import sys
import MySQLdb

# Table: orders
# Columns:
# id	varchar(128) PK
# created_at	bigint(20)
# purchaser_id	varchar(128)
# status	tinyint(4)
# product_id	varchar(128)
# product_version	int(11)
# product_category_id	varchar(128)
# product_app_id	varchar(128)
# purchaser_device_id	varchar(128)
# purchaser_locale	varchar(64)
# purchaser_ip	varchar(64)
# purchaser_ua	varchar(512)
# google_iab_order	varchar(128)
# cmcc_mm_order	varchar(128)
# cmcc_mm_trade	varchar(128)
# pay_cs	varchar(16)
# pay_amount	double

# Table: downloads
# Columns:
# id	varchar(128) PK
# created_at	bigint(20)
# purchaser_id	varchar(128)
# order_id	varchar(128)
# product_id	varchar(128)
# product_version	int(11)
# product_category_id	varchar(128)
# product_app_id	varchar(128)
# download_device_id	varchar(128)
# download_locale	varchar(64)
# download_ip	varchar(64)
# download_ua	varchar(512)



def current_millis():
    return int(round(time.time() * 1000))


random.seed(current_millis())


def random_long():
    return long(current_millis() << 21) | random.randint(0, 2097152)


def ts_to_date(ts):
    return datetime.datetime.fromtimestamp(int(ts / 1000.0)).strftime('%Y-%m-%d')


def country_from_locale(l):
    try:
        return l.split('_')[1]
    except:
        return ''

class ProgressPrinter:
    def __init__(self, name, print_interval = 1000):
        self.name, self.print_interval = name, print_interval
        self.counter = 0

    def next(self):
        if self.counter % self.print_interval == 0:
            print "%s => %s" % (self.name, self.counter)
        self.counter += 1

    def reset(self):
        self.counter = 0


def migrate_downloads(cursor):
    print "..... Downloads => Orders"
    pp1 = ProgressPrinter("orders => downloads")

    # orders => downloads
    cursor.execute("SELECT * FROM orders")
    for row in cursor.fetchall():
        pp1.next()

        product_id = row['product_id']
        download_id = '%s.%s' % (product_id, random_long())
        order_id, purchaser_id = row['id'], row['purchaser_id']
        cursor.execute("""
            INSERT INTO downloads SET
                id = %s,
                created_at = %s,
                purchaser_id = %s,
                order_id = %s,
                product_id = %s,
                product_version = %s,
                product_category_id = %s,
                product_app_id = %s,
                download_device_id = %s,
                download_locale = %s,
                download_ip = %s,
                download_ua = %s
            ;
        """, (
            download_id, # id
            row['created_at'], # created_at
            purchaser_id, # purchaser_id
            order_id if purchaser_id else '', # order_id
            product_id, # product_id
            row['product_version'], # product_version
            row['product_category_id'], # product_category_id,
            row['product_app_id'], # product_app_id,
            row['purchaser_device_id'], # download_device_id,
            row['purchaser_locale'], # download_locale
            row['purchaser_ip'], # download_ip,
            row['purchaser_ua'],                # download_ua
        ))

    # clean orders
    cursor.execute("DELETE FROM orders WHERE purchaser_id=''")


def migrate_counts(cursor):
    print "..... Recounts"
    pp1 = ProgressPrinter("Recount purchase_count")
    pp2 = ProgressPrinter("Recount download_count")

    # purchase count
    cursor.execute("UPDATE products SET purchase_count=0")
    cursor.execute("UPDATE product_versions SET purchase_count=0")
    cursor.execute("SELECT * FROM orders")
    for row in cursor.fetchall():
        pp1.next()
        product_id, version = row['product_id'], row['product_version']
        cursor.execute("UPDATE products SET purchase_count=purchase_count+1 WHERE id=%s", product_id)
        cursor.execute("UPDATE product_versions SET purchase_count=purchase_count+1 WHERE product_id=%s AND version=%s",
                       (product_id, version))


    # download count
    cursor.execute("UPDATE products SET download_count=0")
    cursor.execute("UPDATE product_versions SET download_count=0")
    cursor.execute("SELECT * FROM downloads")
    for row in cursor.fetchall():
        pp2.next()
        product_id, version = row['product_id'], row['product_version']
        cursor.execute("UPDATE products SET download_count=download_count+1 WHERE id=%s", product_id)
        cursor.execute("UPDATE product_versions SET download_count=download_count+1 WHERE product_id=%s AND version=%s",
                       (product_id, version))


def migrate_stat(cursor):
    print "..... Restat"
    pp1 = ProgressPrinter("Restat purchase_count")
    pp2 = ProgressPrinter("Restat download_count")

    # clear stat
    cursor.execute("DELETE FROM statistics")

    # recount purchase count
    cursor.execute("SELECT * FROM orders")
    for row in cursor.fetchall():
        pp1.next()
        product_id, version, date = row['product_id'], row['product_version'], ts_to_date(row['created_at'])
        cursor.execute("""
            INSERT INTO statistics
              SET
                app_id=%s,
                category_id=%s,
                product_id=%s,
                version=%s,
                country=%s,
                dates=%s,
                `count`= 1
              ON DUPLICATE KEY UPDATE
                `count` = `count` + 1
        """, (
            row['product_app_id'], # app_id
            row['product_category_id'], # category_id
            product_id, # product_id
            version, # version
            country_from_locale(row['purchaser_locale']), # country
            date                            # dates
        ))

    # recount download_count
    cursor.execute("SELECT * FROM downloads")
    for row in cursor.fetchall():
        pp2.next()
        product_id, version, date = row['product_id'], row['product_version'], ts_to_date(row['created_at'])
        cursor.execute("""
            INSERT INTO statistics
              SET
                app_id=%s,
                category_id=%s,
                product_id=%s,
                version=%s,
                country=%s,
                dates=%s,
                `download_count`= 1
              ON DUPLICATE KEY UPDATE
                `download_count` = `download_count` + 1
        """, (
            row['product_app_id'], # app_id
            row['product_category_id'], # category_id
            product_id, # product_id
            version, # version
            country_from_locale(row['download_locale']), # country
            date                            # dates
        ))


def migrate(cursor):
    migrate_downloads(cursor)
    migrate_counts(cursor)
    migrate_stat(cursor)


def main():
    host, db, user, pwd = sys.argv[1:]
    conn = MySQLdb.connect(host=host, db=db, user=user, passwd=pwd)
    conn.autocommit(False)
    cursor = None
    try:
        cursor = conn.cursor(cursorclass=MySQLdb.cursors.DictCursor)
        migrate(cursor)
        conn.commit()
    except Exception, e:
        conn.rollback()
        raise e
    finally:
        if cursor:
            cursor.close()
        conn.close()
    print "OK done"


if __name__ == '__main__':
    main()
