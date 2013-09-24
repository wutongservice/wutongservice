
import flask

def error_page(title=u'', message=u''):
    return flask.render_template(u'error.jinja2', page_class=u'error_page', error_title=title, error_message=message)

def redirect_to_signin():
    return flask.redirect(u'/account/signin')
