from __future__ import unicode_literals

__author__ = 'rongxin.gao@borqs.com'

from lxml import etree

import os, os.path
from util import select_lang, MAX_INT32, to_json
from sets import Set

######################################################################

def _xpath(elem, xpath):
    l = elem.xpath(xpath)
    return l[0] if l else None


def _xpath_text(elem, xpath, defval=''):
    x = _xpath(elem, xpath)
    if x is not None:
        if isinstance(x, basestring):
            return unicode(x, encoding='utf8')
        t = x.text
        if t is None:
            return ''
        else:
            return unicode(t, encoding='utf8')
    else:
        return defval


def _xpath_langs(elem, xpath, default_lang):
    x = _xpath(elem, xpath)
    if x is not None:
        langs = {default_lang: unicode(x.text, encoding='utf8')}
        langs_elem = _xpath(x, 'langs')
        if langs_elem is not None and langs_elem.tag == 'langs':
            for lang_elem in langs_elem:
                langs[lang_elem.tag] = lang_elem.text

        return langs
    else:
        return {default_lang: ""}


def _xpath0_dependencies(elem, xpath):
    # TODO:
    return []

######################################################################


class Manifest(object):
    def __init__(self, default_lang, pid, version, name, version_name, recent_change, desc,
                 app_id, category, min_app_vc, max_app_vc, supported_mod,
                 logo, cover,
                 author_name, author_email, author_phone, author_website,
                 screenshot1, screenshot2, screenshot3, screenshot4, screenshot5,
                 dependencies):
        self.default_lang = default_lang
        self.pid = pid
        self.version = version
        self.name = name
        self.version_name = version_name
        self.recent_change = recent_change
        self.description = desc
        self.app_id = app_id
        self.category = category
        self.min_app_vc = min_app_vc
        self.max_app_vc = max_app_vc
        self.supported_mod = supported_mod
        self.logo = logo
        self.cover = cover
        self.author_name = author_name
        self.author_email = author_email
        self.author_phone = author_phone
        self.author_website = author_website
        self.screenshot1 = screenshot1
        self.screenshot2 = screenshot2
        self.screenshot3 = screenshot3
        self.screenshot4 = screenshot4
        self.screenshot5 = screenshot5
        self.dependencies = dependencies

    def name_(self, lang=None):
        return select_lang(self.name, lang=lang, default_lang=self.default_lang)

    def version_name_(self, lang=None):
        return select_lang(self.version_name, lang=lang, default_lang=self.default_lang)

    def recent_change_(self, lang=None):
        return select_lang(self.recent_change, lang=lang, default_lang=self.default_lang)

    def description_(self, lang=None):
        return select_lang(self.description, lang=lang, default_lang=self.default_lang)

    def all_langs(self):
        langs = Set()
        langs.add(self.default_lang)
        for lang in self.name.keys():
            langs.add(lang)
        for lang in self.version_name.keys():
            langs.add(lang)
        for lang in self.recent_change.keys():
            langs.add(lang)
        return [lang for lang in langs]


    def __unicode__(self):
        return to_json({
            'default_lang': self.default_lang,
            'id': self.pid,
            'version': self.version,
            'name': self.name,
            'version_name': self.version_name,
            'recent_change': self.recent_change,
            'description': self.description,
            'app_id': self.app_id,
            'category': self.category,
            'min_app_vc': self.min_app_vc,
            'max_app_vc': self.max_app_vc,
            'supported_mod': self.supported_mod,
            'author_email': self.author_email,
            'author_name': self.author_name,
            'author_phone': self.author_phone,
            'author_website': self.author_website,
            'logo': self.logo,
            'cover': self.cover,
            'screenshot1': self.screenshot1,
            'screenshot2': self.screenshot2,
            'screenshot3': self.screenshot3,
            'screenshot4': self.screenshot4,
            'screenshot5': self.screenshot5,
            'dependencies': self.dependencies,
        })

    def __str__(self):
        return unicode(self).encode('utf8')

    @staticmethod
    def parse(text):
        doc = etree.fromstring(text)
        root = doc.xpath('/BorqsResource')[0]
        default_lang = _xpath_text(root, '@defaultLang', 'en_US')
        return Manifest(
            default_lang=default_lang,
            pid=_xpath_text(root, 'id', ''),
            version=int(_xpath_text(root, 'version', '0')),
            name=_xpath_langs(root, 'name', default_lang),
            version_name=_xpath_langs(root, 'versionName', default_lang),
            recent_change=_xpath_langs(root, 'recentChange', default_lang),
            desc=_xpath_langs(root, 'description', default_lang),
            app_id=_xpath_text(root, 'app', ''),
            category=_xpath_text(root, 'category', ''),
            min_app_vc=int(_xpath_text(root, 'minAppVC', 0)),
            max_app_vc=int(_xpath_text(root, 'maxAppVC', MAX_INT32)),
            supported_mod=_xpath_text(root, 'supportedMod', ''),
            author_name=_xpath_text(root, 'authorName', ''),
            author_email=_xpath_text(root, 'authorEmail', ''),
            author_phone=_xpath_text(root, 'authorPhone', ''),
            author_website=_xpath_text(root, 'authorWebsite', ''),
            logo=_xpath_text(root, 'logo', ''),
            cover=_xpath_text(root, 'cover', ''),
            screenshot1=_xpath_text(root, 'screenshot1', ''),
            screenshot2=_xpath_text(root, 'screenshot2', ''),
            screenshot3=_xpath_text(root, 'screenshot3', ''),
            screenshot4=_xpath_text(root, 'screenshot4', ''),
            screenshot5=_xpath_text(root, 'screenshot5', ''),
            dependencies=_xpath0_dependencies(root, 'dependencies')
        )

    @staticmethod
    def load(fp):
        with open(fp) as f:
            return Manifest.parse(f.read())

######################################################################

def _strip_path(path):
    return path[1:] if path.startswith('/') else path

def _select_path(path1, path2):
    if os.path.exists(path1):
        return path1
    if os.path.exists(path2):
        return path2
    return ''

class Resource(object):
    def __init__(self, path, unzip_dir=None):
        self.path = path
        self.unzip_dir = unzip_dir \
            or os.path.join(os.path.dirname(path), os.path.basename(path) + '.unzip')
        self.manifest = None

    def unzip(self):
        self.delete_unzip()
        os.system("unzip -q '%s' -d '%s'" % (self.path, self.unzip_dir))
        self.manifest = Manifest.load(_select_path(
            os.path.join(self.unzip_dir, 'ResourceManifest.xml'),
            os.path.join(self.unzip_dir, 'assets/ResourceManifest.xml')))

    def delete_unzip(self):
        os.system("rm -rf '%s'" % self.unzip_dir)

    def _image_path(self, f):
        if not f:
            return ''
        f = _strip_path(f)
        return _select_path(
            os.path.join(self.unzip_dir, f),
            os.path.join(self.unzip_dir, 'assets', f)
        )

    @property
    def logo_path(self):
        return self._image_path(self.manifest.logo)

    @property
    def cover_path(self):
        return self._image_path(self.manifest.cover)

    @property
    def screenshot1_path(self):
        return self._image_path(self.manifest.screenshot1)

    @property
    def screenshot2_path(self):
        return self._image_path(self.manifest.screenshot2)

    @property
    def screenshot3_path(self):
        return self._image_path(self.manifest.screenshot3)

    @property
    def screenshot4_path(self):
        return self._image_path(self.manifest.screenshot4)

    @property
    def screenshot5_path(self):
        return self._image_path(self.manifest.screenshot5)

    def __enter__(self):
        self.unzip()
        return self

    def __exit__(self, *args):
        try:
            self.delete_unzip()
        except:
            pass
        return False

