package com.borqs.server.market.utils.jsptags;


import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.core.LoopTagSupport;
import java.io.IOException;

public abstract class WritableLoopTagSupport extends LoopTagSupport {
    protected WritableLoopTagSupport() {
    }

    protected void doWriteStartTag() throws Exception {
    }

    protected void doWriteEndTag() throws Exception {
    }

    protected void doWriteStartStep() throws Exception {
    }

    protected void doWriteEndStep() throws Exception {
    }

    @Override
    public int doStartTag() throws JspException {
        int r = super.doStartTag();
        try {
            doWriteStartTag();
            doWriteStartStep();
        } catch (Exception e) {
            throw new JspException(e);
        }
        return r;
    }

    @Override
    public int doAfterBody() throws JspException {
        try {
            doWriteEndStep();
            int r = super.doAfterBody();
            if (r == EVAL_BODY_AGAIN) {
                doWriteStartStep();
            }
            return r;
        } catch (Exception e) {
            throw new JspException(e);
        }
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            doWriteEndTag();
        } catch (Exception e) {
            throw new JspException(e);
        }
        return super.doEndTag();
    }
}
