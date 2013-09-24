package com.borqs.server.platform.expansion;


import com.borqs.server.platform.context.Context;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;

public class ExpansionHelper {

    public static <T> void expand(List<? extends Expansion<T>> expansions, Context ctx, String[] expCols, T data) {
        if (CollectionUtils.isNotEmpty(expansions)) {
            for (Expansion<T> expansion : expansions)
                expansion.expand(ctx, expCols, data);
        }
    }

    public static boolean needExpand(String[] expCols, String[] abilityCols) {
        if (expCols != null) {
            for (String col : expCols) {
                if (ArrayUtils.contains(abilityCols, col))
                    return true;
            }
        }
        return false;
    }
}
