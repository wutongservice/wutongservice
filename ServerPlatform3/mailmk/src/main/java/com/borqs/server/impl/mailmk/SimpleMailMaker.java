package com.borqs.server.impl.mailmk;

import com.borqs.server.impl.mailmk.i18n.PackageClass;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.maker.AbstractMaker;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.util.template.FreeMarker;

public class SimpleMailMaker extends AbstractMaker<Mail> {
    public static final FreeMarker FREE_MARKER = new FreeMarker(PackageClass.class);

    public SimpleMailMaker() {
    }

    @Override
    public String[] getTemplates() {
        return new String[] {
                MakerTemplates.EMAIL_APK_COMMENT,
                MakerTemplates.EMAIL_APK_LIKE,
                MakerTemplates.EMAIL_ESSENTIAL,
                MakerTemplates.EMAIL_SHARE_TO,
                MakerTemplates.EMAIL_STREAM_COMMENT,
                MakerTemplates.EMAIL_STREAM_LIKE
        };
    }

    @Override
    public Mail make(Context ctx, String template, Record opts) {
        String locale = ctx.getLocale();
        opts.put("emailType", template);

        String from = opts.checkGetString("from");
        String to = opts.checkGetString("to");
        String subject = opts.checkGetString("subject");

        opts.remove("from");
        opts.remove("to");
        opts.remove("subject");

        String html = FREE_MARKER.merge("mail.ftl", locale, opts);
        return Mail.html(from, to, subject, html);
    }
}
