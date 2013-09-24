package com.borqs.server.platform.feature.comment;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.util.ArrayHelper;
import org.codehaus.plexus.util.StringUtils;

public class CommentContentDuplicateHook implements CommentHook {
    private CommentLogic comment;


    public CommentContentDuplicateHook() {
    }


    public void setComment(CommentLogic comment) {
        this.comment = comment;
    }


    @Override
    public void before(Context ctx, Comment data) {
        // find the duplicate comment
        Comments comments = comment.getCommentsOnTarget(ctx, Comment.STANDARD_COLUMNS, new Page(0, 1000), data.getTarget());
        for (Comment c : comments) {
            if(checkComment(c,data)){
                ctx.putSession("return",true);
            }
        }
    }

    private boolean checkComment(Comment oldComment, Comment newComment) {
        PeopleIds addToOld = oldComment.getAddTo();
        if(addToOld == null)
            addToOld = new PeopleIds();
        PeopleIds addToNew = newComment.getAddTo();
        if(addToNew == null)
            addToNew = new PeopleIds();
        
        boolean addto = ArrayHelper.equalsAsSet(addToOld.getUserIds(), addToNew.getUserIds());

        boolean oldCanlike =  oldComment.getCanLike();
        boolean newCanlike =  newComment.getCanLike();
        boolean canlike = oldCanlike & newCanlike;

        String oldMesage = oldComment.getMessage();
        String newMesage = newComment.getMessage();
        boolean message = StringUtils.equals(oldMesage,newMesage);


        return addto & canlike & message;
    }

    @Override
    public void after(Context ctx, Comment data) {

    }
}
