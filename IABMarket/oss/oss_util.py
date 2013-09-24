#!/usr/bin/env python
#coding=utf-8
import urllib
import base64
import hmac
import time
from hashlib import sha1 as sha
import os
import md5
import StringIO
from threading import Thread
import threading
import ConfigParser
from oss_xml_handler import *

#LOG_LEVEL can be one of DEBUG INFO ERROR CRITICAL WARNNING
LOG_LEVEL = "ERROR" 
PROVIDER = "OSS"
SELF_DEFINE_HEADER_PREFIX = "x-oss-"
if "AWS" == PROVIDER:
    SELF_DEFINE_HEADER_PREFIX = "x-amz-"

def initlog(log_level = LOG_LEVEL):
    import logging
    from logging.handlers import RotatingFileHandler
    LOGFILE = os.path.join(os.getcwd(), 'log.txt')
    MAXLOGSIZE = 100*1024*1024 #Bytes
    BACKUPCOUNT = 5
    FORMAT = \
    "%(asctime)s %(levelname)-8s[%(filename)s:%(lineno)d(%(funcName)s)] %(message)s"
    hdlr = RotatingFileHandler(LOGFILE,
                                  mode='a',
                                  maxBytes=MAXLOGSIZE,
                                  backupCount=BACKUPCOUNT)
    formatter = logging.Formatter(FORMAT)
    hdlr.setFormatter(formatter)
    logger = logging.getLogger("oss")
    logger.addHandler(hdlr)
    if "DEBUG" == log_level.upper():
        logger.setLevel(logging.DEBUG)
    elif "INFO" == log_level.upper():
        logger.setLevel(logging.INFO)
    elif "WARNING" == log_level.upper():
        logger.setLevel(logging.WARNING)
    elif "ERROR" == log_level.upper():
        logger.setLevel(logging.ERROR)
    elif "CRITICAL" == log_level.upper():
        logger.setLevel(logging.CRITICAL)
    else:
        logger.setLevel(logging.ERROR)
    return logger

log = initlog(LOG_LEVEL)

########## function for Authorization ##########
def _format_header(headers = None):
    '''
    format the headers that self define
    convert the self define headers to lower.
    '''
    if not headers:
        headers = {}
    tmp_headers = {}
    for k in headers.keys():
        if isinstance(headers[k], unicode):
            headers[k] = headers[k].encode('utf-8')

        if k.lower().startswith(SELF_DEFINE_HEADER_PREFIX):
            k_lower = k.lower()
            tmp_headers[k_lower] = headers[k]
        else:
            tmp_headers[k] = headers[k]
    return tmp_headers

def get_assign(secret_access_key, method, headers = None, resource="/", result = None):
    '''
    Create the authorization for OSS based on header input.
    You should put it into "Authorization" parameter of header.
    '''
    if not headers:
        headers = {}
    if not result:
        result = []
    content_md5 = ""
    content_type = ""
    date = ""
    canonicalized_oss_headers = ""
    log.debug("secret_access_key: %s" % secret_access_key)
    content_md5 = safe_get_element('Content-MD5', headers)
    content_type = safe_get_element('Content-Type', headers)
    date = safe_get_element('Date', headers)
    canonicalized_resource = resource
    tmp_headers = _format_header(headers)
    if len(tmp_headers) > 0:
        x_header_list = tmp_headers.keys()
        x_header_list.sort()
        for k in x_header_list:
            if k.startswith(SELF_DEFINE_HEADER_PREFIX):
                canonicalized_oss_headers += k + ":" + tmp_headers[k] + "\n"
    string_to_sign = method + "\n" + content_md5.strip() + "\n" + content_type + "\n" + date + "\n" + canonicalized_oss_headers + canonicalized_resource;
    result.append(string_to_sign)
    log.debug("\nmethod:%s\n content_md5:%s\n content_type:%s\n data:%s\n canonicalized_oss_headers:%s\n canonicalized_resource:%s\n" % (method, content_md5, content_type, date, canonicalized_oss_headers, canonicalized_resource))
    log.debug("\nstring_to_sign:%s\n \nstring_to_sign_size:%d\n" % (string_to_sign, len(string_to_sign)))
    h = hmac.new(secret_access_key, string_to_sign, sha)
    return base64.encodestring(h.digest()).strip()

def get_resource(params = None):
    if not params:
        params = {}
    tmp_headers = {}
    query_string = ""
    for k, v in params.items():
        tmp_k = k.lower().strip()
        tmp_headers[tmp_k] = v
    override_response_list = ['response-content-type', 'response-content-language', \
                              'response-cache-control', 'logging', 'response-content-encoding', \
                              'acl', 'uploadId', 'uploads', 'partNumber', 'group', \
                              'delete', 'website',\
                              'response-expires', 'response-content-disposition']
    override_response_list.sort()
    resource = ""
    uri = ""
    separator = "?"
    for i in override_response_list:
        if tmp_headers.has_key(i.lower()):
            resource += separator
            resource += i
            if len(tmp_headers[i.lower()]) != 0:
                resource += "="
                resource += tmp_headers[i.lower()]
            separator = '&'
    return resource

def append_param(url, params):
    '''
    convert the parameters to query string of URI.
    '''
    l = []
    for k,v in params.items():
        k = k.replace('_', '-')
        if  k == 'maxkeys':
            k = 'max-keys'
        if isinstance(v, unicode):
            v = v.encode('utf-8')
        if v is not None and v != '':
            l.append('%s=%s' % (urllib.quote(k), urllib.quote(str(v))))
        elif k == 'acl':
            l.append('%s' % (urllib.quote(k)))
        elif v is None or v == '':
            l.append('%s' % (urllib.quote(k)))
    if len(l):
        url = url + '?' + '&'.join(l)
    return url

############### Construct XML ###############
def create_object_group_msg_xml(part_msg_list = None):
    '''
    get information from part_msg_list and covert it to xml.
    part_msg_list has special format.
    '''
    if not part_msg_list:
        part_msg_list = []
    xml_string = r'<CreateFileGroup>'
    for part in part_msg_list:
        if len(part) >= 3:
            if isinstance(part[1], unicode):
                file_path = part[1].encode('utf-8')
            else:
                file_path = part[1]
            xml_string += r'<Part>'
            xml_string += r'<PartNumber>' + str(part[0]) + r'</PartNumber>'
            xml_string += r'<PartName>' + str(file_path) + r'</PartName>'
            xml_string += r'<ETag>"' + str(part[2]).upper() + r'"</ETag>'
            xml_string += r'</Part>'
        else:
            print "the ", part, " in part_msg_list is not as expected!"
            return ""
    xml_string += r'</CreateFileGroup>'

    return xml_string

def create_part_xml(part_msg_list = None):
    if not part_msg_list:
        part_msg_list = []
    '''
    get information from part_msg_list and covert it to xml.
    part_msg_list has special format.
    '''
    xml_string = r'<CompleteMultipartUpload>'
    for part in part_msg_list:
        if len(part) >= 3:
            if isinstance(part[1], unicode):
                file_path = part[1].encode('utf-8')
            else:
                file_path = part[1]
            xml_string += r'<Part>'
            xml_string += r'<PartNumber>' + str(part[0]) + r'</PartNumber>'
            xml_string += r'<ETag>"' + str(part[2]).upper() + r'"</ETag>'
            xml_string += r'</Part>'
        else:
            print "the ", part, " in part_msg_list is not as expected!"
            return ""
    xml_string += r'</CompleteMultipartUpload>'

    return xml_string

def create_delete_object_msg_xml(object_list = None, is_quiet = False, is_defult = False):
    '''
    covert object name list to xml.
    '''
    if not object_list:
        object_list = []
    xml_string = r'<Delete>'
    if not is_defult:
        if is_quiet:
            xml_string += r'<Quiet>true</Quiet>'
        else:
            xml_string += r'<Quiet>false</Quiet>'
    for object in object_list:
            key = object.strip()
            if isinstance(object, unicode):
                key = object.encode('utf-8')
            xml_string += r'<Object><Key>%s</Key></Object>' % key
    xml_string += r'</Delete>'
    return xml_string

############### operate OSS ###############
def clear_all_object_of_bucket(oss_instance, bucket):
    '''
    clean all objects in bucket, after that, it will delete this bucket.
    '''
    return clear_all_objects_in_bucket(oss_instance, bucket)

def clear_all_objects_in_bucket(oss_instance, bucket):
    '''
    it will clean all objects in bucket, after that, it will delete this bucket.

    example:
    from oss_api import *
    host = ""
    id = ""
    key = ""
    oss_instance = OssAPI(host, id, key)
    bucket = "leopublicreadprivatewrite"
    if clear_all_objects_in_bucket(oss_instance, bucket):
        pass
    else:
        print "clean Fail"
    '''
    b = GetAllObjects()
    b.get_all_object_in_bucket(oss_instance, bucket)
    for i in b.object_list:
        res = oss_instance.delete_object(bucket, i)
        if (res.status / 100 != 2):
            print "clear_all_objects_in_bucket: delete object fail, ret is:", res.status, "object is: ", i
            return False
        else:
            pass
    marker = ""
    id_marker = ""
    count = 0
    while True:
        res = oss_instance.get_all_multipart_uploads(bucket, key_marker = marker, upload_id_marker=id_marker)
        if res.status != 200:
            break

        body = res.read()
        hh = GetMultipartUploadsXml(body)
        (fl, pl) = hh.list()
        for i in fl:
            count += 1
            object = i[0]
            if isinstance(i[0], unicode):
                object = i[0].encode('utf-8')
            res = oss_instance.cancel_upload(bucket, object, i[1])
            if (res.status / 100 != 2 and res.status != 404):
                print "clear_all_objects_in_bucket: cancel upload fail, ret is:", res.status
            else:
                pass
        if hh.is_truncated:
            marker = hh.next_key_marker
            id_marker = hh.next_upload_id_marker
        else:
            break
        if len(marker) == 0:
            break

    res = oss_instance.delete_bucket(bucket)
    if (res.status / 100 != 2 and res.status != 404):
        print "clear_all_objects_in_bucket: delete bucket fail, ret is:", res.status
        return False
    return True

def clean_all_bucket(oss_instance):
    '''
    it will clean all bucket, including the all objects in bucket.
    '''
    res = oss_instance.get_service()
    if (res.status / 100) == 2:
        h = GetServiceXml(res.read())
        bucket_list = h.list()
        for b in h.bucket_list:
            if not clear_all_objects_in_bucket(oss_instance, b.name):
                print "clean bucket ", b.name, " failed! in clean_all_bucket"
                return False
        return True
    else:
        print "failed! get service in clean_all_bucket return ", res.status
        print res.read()
        print res.getheaders()
        return False

def delete_all_parts_of_object_group(oss, bucket, object_group_name):
    res = oss.get_object_group_index(bucket, object_group_name)
    if res.status == 200:
        body = res.read()
        h = GetObjectGroupIndexXml(body)
        object_group_index = h.list()
        for i in object_group_index:
            if len(i) == 4 and len(i[1]) > 0:
                part_name = i[1].strip()
                res = oss.delete_object(bucket, part_name)
                if res.status != 204:
                    print "delete part ", part_name, " in bucket:", bucket, " failed!"
                    return False
    else:
        return False

    return True;

class GetAllObjects:
    def __init__(self):
        self.object_list = []

    def get_object_in_bucket(self, oss, bucket="", marker="", prefix=""):
        object_list = []
        maxkeys = 1000
        try:
            res = oss.get_bucket(bucket, prefix, marker, maxkeys=maxkeys)
            body = res.read()
            hh = GetBucketXml(body)
            (fl, pl) = hh.list()
            if len(fl) != 0:
                for i in fl:
                    if isinstance(i[0], unicode):
                        object = i[0].encode('utf-8')
                        object_list.append(object)

            marker = hh.nextmarker
        except:
            pass
        return (object_list, marker)

    def get_all_object_in_bucket(self, oss, bucket="", marker="", prefix=""):
        marker2 = ""
        while True:
            (object_list, marker) = self.get_object_in_bucket(oss, bucket, marker2, prefix)
            marker2 = marker
            if len(object_list) != 0:
                self.object_list.extend(object_list)

            if len(marker) == 0:
                break

def get_upload_id(oss, bucket, object, headers = None):
    '''
    get the upload id of object.
    Returns:
            string
    '''
    if not headers:
        headers = {}
    upload_id = ""
    res = oss.init_multi_upload(bucket, object, headers)
    if res.status == 200:
        body = res.read()
        h = GetInitUploadIdXml(body)
        upload_id = h.upload_id
    else:
        print res.status
        print res.getheaders()
        print res.read()
    return upload_id

def get_all_upload_id_list(oss, bucket):
    '''
    get all upload id of bucket
    Returns:
            list
    '''
    all_upload_id_list = []
    marker = ""
    id_marker = ""
    while True:
        res = oss.get_all_multipart_uploads(bucket, key_marker = marker, upload_id_marker=id_marker)
        if res.status != 200:
            return all_upload_id_list

        body = res.read()
        hh = GetMultipartUploadsXml(body)
        (fl, pl) = hh.list()
        for i in fl:
                all_upload_id_list.append(i)
        if hh.is_truncated:
            marker = hh.next_key_marker
            id_marker = hh.next_upload_id_marker
        else:
            break
        if len(marker) == 0 and len(id_marker) == 0:
            break
    return all_upload_id_list

def get_upload_id_list(oss, bucket, object):
    '''
    get all upload id list of one object.
    Returns:
            list
    '''
    upload_id_list = []
    marker = ""
    id_marker = ""
    while True:
        res = oss.get_all_multipart_uploads(bucket, prefix=object, key_marker = marker, upload_id_marker=id_marker)
        if res.status != 200:
            break
        body = res.read()
        hh = GetMultipartUploadsXml(body)
        (fl, pl) = hh.list()
        for i in fl:
            upload_id_list.append(i[1])
        if hh.is_truncated:
            marker = hh.next_key_marker
            id_marker = hh.next_upload_id_marker
        else:
            break
        if len(marker) == 0:
            break

    return upload_id_list

def get_part_list(oss, bucket, object, upload_id, max_part=""):
    '''
    get uploaded part list of object.
    Returns:
            list
    '''
    part_list = []
    marker = ""
    while True:
        res = oss.get_all_parts(bucket, object, upload_id, part_number_marker = marker, max_parts=max_part)
        if res.status != 200:
            break
        body = res.read()
        h = GetPartsXml(body)
        part_list.extend(h.list())
        if h.is_truncated:
            marker = h.next_part_number_marker
        else:
            break
        if len(marker) == 0:
            break
    return part_list

def get_part_xml(oss, bucket, object, upload_id):
    '''
    get uploaded part list of object.
    Returns:
            string
    '''
    part_list = []
    part_list = get_part_list(oss, bucket, object, upload_id)
    xml_string = r'<CompleteMultipartUpload>'
    for part in part_list:
            xml_string += r'<Part>'
            xml_string += r'<PartNumber>' + str(part[0]) + r'</PartNumber>'
            xml_string += r'<ETag>' + part[1] + r'</ETag>'
            xml_string += r'</Part>'
    xml_string += r'</CompleteMultipartUpload>'
    return xml_string

def get_part_map(oss, bucket, object, upload_id):
    part_list = []
    part_list = get_part_list(oss, bucket, object, upload_id)
    part_map = {}
    for part in part_list:
        part_map[str(part[0])] = part[1]
    return part_map

########## multi-thread ##########
class DeleteObjectWorker(Thread):
    def __init__(self, oss, bucket, part_msg_list, retry_times=5):
        Thread.__init__(self)
        self.oss = oss
        self.bucket = bucket
        self.part_msg_list = part_msg_list
        self.retry_times = retry_times

    def run(self):
        bucket = self.bucket
        object_list = self.part_msg_list
        step = 1000
        begin = 0
        end = 0
        total_length = len(object_list)
        remain_length = total_length
        while True:
            if remain_length > step:
                end = begin + step
            elif remain_length > 0:
                end = begin + remain_length
            else:
                break
            is_fail = True
            retry_times = self.retry_times
            while True:
                try:
                    if retry_times <= 0:
                        break
                    res = self.oss.delete_objects(bucket, object_list[begin:end])
                    if res.status / 100 == 2:
                        is_fail = False
                        break
                except:
                    retry_times = retry_times - 1
                    time.sleep(1)
            if is_fail:
                print "delete object_list[%s:%s] failed!, first is %s" % (begin, end, object_list[begin])
            begin = end
            remain_length = remain_length - step

class PutObjectGroupWorker(Thread):
    def __init__(self, oss, bucket, file_path, part_msg_list, retry_times=5):
        Thread.__init__(self)
        self.oss = oss
        self.bucket = bucket
        self.part_msg_list = part_msg_list
        self.file_path = file_path
        self.retry_times = retry_times

    def run(self):
        for part in self.part_msg_list:
            if len(part) == 5:
                bucket = self.bucket
                file_name = part[1]
                if isinstance(file_name, unicode):
                    filename = file_name.encode('utf-8')
                object_name = file_name
                retry_times = self.retry_times
                is_skip = False
                while True:
                    try:
                        if retry_times <= 0:
                            break
                        res = self.oss.head_object(bucket, object_name)
                        if res.status == 200:
                            header_map = convert_header2map(res.getheaders())
                            etag = safe_get_element("etag", header_map)
                            md5 = part[2]
                            if etag.replace('"', "").upper() == md5.upper():
                                is_skip = True
                        break
                    except:
                        retry_times = retry_times - 1
                        time.sleep(1)

                if is_skip:
                    continue

                partsize = part[3]
                offset = part[4]
                retry_times = self.retry_times
                while True:
                    try:
                        if retry_times <= 0:
                            break
                        res = self.oss.put_object_from_file_given_pos(bucket, object_name, self.file_path, offset, partsize)
                        if res.status != 200:
                            print "upload ", file_name, "failed!"," ret is:", res.status
                            print "headers", res.getheaders()
                            retry_times = retry_times - 1
                            time.sleep(1)
                        else:
                            break
                    except:
                        retry_times = retry_times - 1
                        time.sleep(1)

            else:
                print "ERROR! part", part , " is not as expected!"

class UploadPartWorker(Thread):
    def __init__(self, oss, bucket, object, upoload_id, file_path, part_msg_list, uploaded_part_map, retry_times=5):
        Thread.__init__(self)
        self.oss = oss
        self.bucket = bucket
        self.object = object
        self.part_msg_list = part_msg_list
        self.file_path = file_path
        self.upload_id = upoload_id
        self.uploaded_part_map = uploaded_part_map
        self.retry_times = retry_times

    def run(self):
        for part in self.part_msg_list:
            part_number = str(part[0])
            if len(part) == 5:
                bucket = self.bucket
                object = self.object
                if self.uploaded_part_map.has_key(part_number):
                    md5 = part[2]
                    if self.uploaded_part_map[part_number].replace('"', "").upper() == md5.upper():
                        continue

                partsize = part[3]
                offset = part[4]
                retry_times = self.retry_times
                while True:
                    try:
                        if retry_times <= 0:
                            break
                        res = self.oss.upload_part_from_file_given_pos(bucket, object, self.file_path, offset, partsize, self.upload_id, part_number)
                        if res.status != 200:
                            log.warn("Upload %s/%s from %s, failed! ret is:%s." %(bucket, object, self.file_path, res.status))
                            log.warn("headers:%s" % res.getheaders())
                            retry_times = retry_times - 1
                            time.sleep(1)
                        else:
                            log.info("Upload %s/%s from %s, OK! ret is:%s." % (bucket, object, self.file_path, res.status))
                            break
                    except:
                        retry_times = retry_times - 1
                        time.sleep(1)
            else:
                log.error("ERROR! part %s is not as expected!" % part)

class MultiGetWorker(Thread):
    def __init__(self, oss, bucket, object, file, start, end, retry_times=5):
        Thread.__init__(self)
        self.oss = oss
        self.bucket = bucket
        self.object = object
        self.startpos = start
        self.endpos = end
        self.file = file
        self.length = self.endpos - self.startpos + 1
        self.need_read = 0
        self.get_buffer_size = 10*1024*1024
        self.retry_times = retry_times

    def run(self):
        if self.startpos >= self.endpos:
            return

        retry_times = 0
        while True:
            headers = {}
            self.file.seek(self.startpos)
            headers['Range'] = 'bytes=%d-%d' % (self.startpos, self.endpos)
            res = self.oss.object_operation("GET", self.bucket, self.object, headers)
            if res.status == 206:
                while self.need_read < self.length:
                    left_len = self.length - self.need_read
                    if left_len > self.get_buffer_size:
                        content = res.read(self.get_buffer_size)
                    else:
                        content = res.read(left_len)
                    if content:
                        self.need_read += len(content)
                        self.file.write(content)
                    else:
                        break
                break
            retry_times += 1
            if retry_times > self.retry_times:
                break

        self.file.flush()
        self.file.close()

############### misc ###############

def split_large_file(file_path, object_prefix = "", max_part_num = 1000, part_size = 10 * 1024 * 1024, buffer_size = 10 * 1024):
    parts_list = []

    if os.path.isfile(file_path):
        file_size = os.path.getsize(file_path)

        if file_size > part_size * max_part_num:
            part_size = (file_size + max_part_num - file_size % max_part_num) / max_part_num

        part_order = 1
        fp = open(file_path, 'rb')
        fp.seek(os.SEEK_SET)

        total_split_len = 0
        part_num = file_size / part_size
        if file_size % part_size != 0:
            part_num += 1

        for i in range(0, part_num):
            left_len = part_size
            real_part_size = 0
            m = md5.new()
            offset = part_size * i
            while True:
                read_size = 0
                if left_len <= 0:
                    break
                elif left_len < buffer_size:
                    read_size = left_len
                else:
                    read_size = buffer_size

                buffer_content = fp.read(read_size)
                m.update(buffer_content)
                real_part_size += len(buffer_content)

                left_len = left_len - read_size

            md5sum = m.hexdigest()

            temp_file_name = os.path.basename(file_path) + "_" + str(part_order)
            if isinstance(object_prefix, unicode):
                object_prefix = object_prefix.encode('utf-8')
            if len(object_prefix) == 0:
                file_name = sum_string(temp_file_name) + "_" + temp_file_name
            else:
                file_name = object_prefix + "/" + sum_string(temp_file_name) + "_" + temp_file_name
            part_msg = (part_order, file_name, md5sum, real_part_size, offset)
            total_split_len += real_part_size
            parts_list.append(part_msg)
            part_order += 1

        fp.close()
    else:
        print "ERROR! No file: ", file_path, ", please check."

    return parts_list

def sumfile(fobj):
    '''Returns an md5 hash for an object with read() method.'''
    m = md5.new()
    while True:
        d = fobj.read(8096)
        if not d:
            break
        m.update(d)
    return m.hexdigest()

def md5sum(fname):
    '''Returns an md5 hash for file fname, or stdin if fname is "-".'''
    if fname == '-':
        ret = sumfile(sys.stdin)
    else:
        try:
            f = file(fname, 'rb')
        except:
            return 'Failed to open file'
        ret = sumfile(f)
        f.close()
    return ret

def md5sum2(filename, offset = 0, partsize = 0):
    m = md5.new()
    fp = open(filename, 'rb')
    if offset > os.path.getsize(filename):
        fp.seek(os.SEEK_SET, os.SEEK_END)
    else:
        fp.seek(offset)

    left_len = partsize
    BufferSize = 8 * 1024
    while True:
        if left_len <= 0:
           break
        elif left_len < BufferSize:
           buffer_content = fp.read(left_len)
        else:
           buffer_content = fp.read(BufferSize)
        m.update(buffer_content)

        left_len = left_len - len(buffer_content)
    md5sum = m.hexdigest()
    return md5sum

def sum_string(content):
    f = StringIO.StringIO(content)
    md5sum = sumfile(f)
    f.close()
    return md5sum

def convert_header2map(header_list):
    header_map = {}
    for (a, b) in header_list:
        header_map[a] = b
    return header_map

def safe_get_element(name, container):
    for k, v in container.items():
        if k.strip().lower() == name.strip().lower():
            return v
    return ""

def get_content_type_by_filename(file_name):
    suffix = ""
    name = os.path.basename(file_name)
    suffix = name.split('.')[-1]

    #http://www.iangraham.org/books/html4ed/appb/mimetype.html
    map = {}
    map['html'] = 'text/html'
    map['htm'] = 'text/html'
    map['asc'] = 'text/plain'
    map['txt'] = 'text/plain'
    map['c'] = 'text/plain'
    map['c++'] = 'text/plain'
    map['cc'] = 'text/plain'
    map['cpp'] = 'text/plain'
    map['h'] = 'text/plain'
    map['rtx'] = 'text/richtext'
    map['rtf'] = 'text/rtf'
    map['sgml'] = 'text/sgml'
    map['sgm'] = 'text/sgml'
    map['tsv'] = 'text/tab-separated-values'
    map['wml'] = 'text/vnd.wap.wml'
    map['wmls'] = 'text/vnd.wap.wmlscript'
    map['etx'] = 'text/x-setext'
    map['xsl'] = 'text/xml'
    map['xml'] = 'text/xml'
    map['talk'] = 'text/x-speech'
    map['css'] = 'text/css'

    map['gif'] = 'image/gif'
    map['xbm'] = 'image/x-xbitmap'
    map['xpm'] = 'image/x-xpixmap'
    map['png'] = 'image/png'
    map['ief'] = 'image/ief'
    map['jpeg'] = 'image/jpeg'
    map['jpg'] = 'image/jpeg'
    map['jpe'] = 'image/jpeg'
    map['tiff'] = 'image/tiff'
    map['tif'] = 'image/tiff'
    map['rgb'] = 'image/x-rgb'
    map['g3f'] = 'image/g3fax'
    map['xwd'] = 'image/x-xwindowdump'
    map['pict'] = 'image/x-pict'
    map['ppm'] = 'image/x-portable-pixmap'
    map['pgm'] = 'image/x-portable-graymap'
    map['pbm'] = 'image/x-portable-bitmap'
    map['pnm'] = 'image/x-portable-anymap'
    map['bmp'] = 'image/bmp'
    map['ras'] = 'image/x-cmu-raster'
    map['pcd'] = 'image/x-photo-cd'
    map['wi'] = 'image/wavelet'
    map['dwg'] = 'image/vnd.dwg'
    map['dxf'] = 'image/vnd.dxf'
    map['svf'] = 'image/vnd.svf'
    map['cgm'] = 'image/cgm'
    map['djvu'] = 'image/vnd.djvu'
    map['djv'] = 'image/vnd.djvu'
    map['wbmp'] = 'image/vnd.wap.wbmp'

    map['ez'] = 'application/andrew-inset'
    map['cpt'] = 'application/mac-compactpro'
    map['doc'] = 'application/msword'
    map['msw'] = 'application/x-dox_ms_word'
    map['oda'] = 'application/oda'
    map['dms'] = 'application/octet-stream'
    map['lha'] = 'application/octet-stream'
    map['lzh'] = 'application/octet-stream'
    map['class'] = 'application/octet-stream'
    map['so'] = 'application/octet-stream'
    map['dll'] = 'application/octet-stream'
    map['pdf'] = 'application/pdf'
    map['ai'] = 'application/postscript'
    map['eps'] = 'application/postscript'
    map['ps'] = 'application/postscript'
    map['smi'] = 'application/smil'
    map['smil'] = 'application/smil'
    map['mif'] = 'application/vnd.mif'
    map['xls'] = 'application/vnd.ms-excel'
    map['xlc'] = 'application/vnd.ms-excel'
    map['xll'] = 'application/vnd.ms-excel'
    map['xlm'] = 'application/vnd.ms-excel'
    map['xlw'] = 'application/vnd.ms-excel'
    map['ppt'] = 'application/vnd.ms-powerpoint'
    map['ppz'] = 'application/vnd.ms-powerpoint'
    map['pps'] = 'application/vnd.ms-powerpoint'
    map['pot'] = 'application/vnd.ms-powerpoint'

    map['wbxml'] = 'application/vnd.wap.wbxml'
    map['wmlc'] = 'application/vnd.wap.wmlc'
    map['wmlsc'] = 'application/vnd.wap.wmlscriptc'
    map['vcd'] = 'application/x-cdlink'
    map['pgn'] = 'application/x-chess-pgn'
    map['dcr'] = 'application/x-director'
    map['dir'] = 'application/x-director'
    map['dxr'] = 'application/x-director'
    map['spl'] = 'application/x-futuresplash'

    map['gtar'] = 'application/x-gtar'
    map['tar'] = 'application/x-tar'
    map['ustar'] = 'application/x-ustar'
    map['bcpio'] = 'application/x-bcpio'
    map['cpio'] = 'application/x-cpio'
    map['shar'] = 'application/x-shar'
    map['zip'] = 'application/zip'
    map['hqx'] = 'application/mac-binhex40'
    map['sit'] = 'application/x-stuffit'
    map['sea'] = 'application/x-stuffit'
    map['bin'] = 'application/octet-stream'
    map['exe'] = 'application/octet-stream'
    map['src'] = 'application/x-wais-source'
    map['wsrc'] = 'application/x-wais-source'
    map['hdf'] = 'application/x-hdf'

    map['js'] = 'application/x-javascript'
    map['sh'] = 'application/x-sh'
    map['csh'] = 'application/x-csh'
    map['pl'] = 'application/x-perl'
    map['tcl'] = 'application/x-tcl'

    map['skp'] = 'application/x-koan'
    map['skd'] = 'application/x-koan'
    map['skt'] = 'application/x-koan'
    map['skm'] = 'application/x-koan'
    map['nc'] = 'application/x-netcdf'
    map['cdf'] = 'application/x-netcdf'
    map['swf'] = 'application/x-shockwave-flash'
    map['sv4cpio'] = 'application/x-sv4cpio'
    map['sv4crc']  = 'application/x-sv4crc'
    map['t'] = 'application/x-troff'
    map['tr'] = 'application/x-troff'
    map['roff'] = 'application/x-troff'
    map['man'] = 'application/x-troff-man'
    map['me'] = 'application/x-troff-me'
    map['ms'] = 'application/x-troff-ms'
    map['latex'] = 'application/x-latex'
    map['tex'] = 'application/x-tex'
    map['texinfo'] = 'application/x-texinfo'
    map['texi'] = 'application/x-texinfo'
    map['dvi'] = 'application/x-dvi'
    map['xhtml'] = 'application/xhtml+xml'
    map['xht'] = 'application/xhtml+xml'

    map['au'] = 'audio/basic'
    map['snd'] = 'audio/basic'
    map['aif'] = 'audio/x-aiff'
    map['aiff'] = 'audio/x-aiff'
    map['aifc'] = 'audio/x-aiff'
    map['wav'] = 'audio/x-wav'
    map['mpa'] = 'audio/x-mpeg'
    map['abs'] = 'audio/x-mpeg'
    map['mpega'] = 'audio/x-mpeg'
    map['mp2a'] = 'audio/x-mpeg2'
    map['mpa2'] = 'audio/x-mpeg2'
    map['mid'] = 'audio/midi'
    map['midi'] = 'audio/midi'
    map['kar'] = 'audio/midi'
    map['mp2'] = 'audio/mpeg'
    map['mp3'] = 'audio/mpeg'
    map['m3u'] = 'audio/x-mpegurl'
    map['ram'] = 'audio/x-pn-realaudio'
    map['rm'] = 'audio/x-pn-realaudio'
    map['rpm'] = 'audio/x-pn-realaudio-plugin'
    map['ra'] = 'audio/x-realaudio'

    map['pdb'] = 'chemical/x-pdb'
    map['xyz'] = 'chemical/x-xyz'
    map['igs'] = 'model/iges'
    map['iges'] = 'model/iges'
    map['msh'] = 'model/mesh'
    map['mesh'] = 'model/mesh'
    map['silo'] = 'model/mesh'

    map['wrl'] = 'model/vrml'
    map['vrml'] = 'model/vrml'
    map['vrw'] = 'x-world/x-vream'
    map['svr'] = 'x-world/x-svr'
    map['wvr'] = 'x-world/x-wvr'
    map['3dmf'] = 'x-world/x-3dmf'
    map['p3d'] = 'application/x-p3d'

    map['mpeg'] = 'video/mpeg'
    map['mpg'] = 'video/mpeg'
    map['mpe'] = 'video/mpeg'
    map['mpv2'] = 'video/mpeg2'
    map['mp2v'] = 'video/mpeg2'
    map['qt'] = 'video/quicktime'
    map['mov'] = 'video/quicktime'
    map['avi'] = 'video/x-msvideo'
    map['movie'] = 'video/x-sgi-movie'
    map['vdo'] = 'video/vdo'
    map['viv'] = 'video/viv'
    map['mxu'] = 'video/vnd.mpegurl'

    map['ice'] = 'x-conference/x-cooltalk'
    import mimetypes
    mimetypes.init()
    mime_type = ""
    try:
        mime_type = mimetypes.types_map["." + suffix]
    except Exception, e:
        if map.has_key(suffix):
            mime_type = map[suffix]
        else:
            mime_type = 'application/octet-stream'
    return mime_type

def smart_code(input_stream):
    if isinstance(input_stream, str):
        try:
            tmp = unicode(input_stream, 'utf-8')
        except UnicodeDecodeError:
            try:
                tmp = unicode(input_stream, 'gbk')
            except UnicodeDecodeError:
                try:
                    tmp = unicode(input_stream, 'big5')
                except UnicodeDecodeError:
                    try:
                        tmp = unicode(input_stream, 'ascii')
                    except:
                        tmp = input_stream
    else:
        tmp = input_stream
    return tmp

def is_ip(s):
    try:
        tmp_list = s.split(':')
        s = tmp_list[0]
        tmp_list = s.split('.')
        if len(tmp_list) != 4:
            return False
        else:
            for i in tmp_list:
                if int(i) < 0 or int(i) > 255:
                    return False
    except:
        return False
    return True

if __name__ == '__main__':
    pass
