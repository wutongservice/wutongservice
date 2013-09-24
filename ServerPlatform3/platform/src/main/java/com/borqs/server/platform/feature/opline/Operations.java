package com.borqs.server.platform.feature.opline;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Operations extends ArrayList<Operation> {
    public Operations() {
    }

    public Operations(int initialCapacity) {
        super(initialCapacity);
    }

    public Operations(Collection<? extends Operation> c) {
        super(c);
    }

    public Operations(Operation... opers) {
        Collections.addAll(this, opers);
    }

    public boolean isSameUser() {
        if (isEmpty())
            return true;

        long userId = 0;
        for (Operation oper : this) {
            if (oper != null) {
                userId = oper.getUserId();
                break;
            }
        }

        for (Operation oper : this) {
            if (oper != null && oper.getUserId() != userId)
                return false;
        }
        return true;
    }

    public long getFirstUserId() {
        for (Operation oper : this) {
            if (oper != null)
                return oper.getUserId();
        }
        return 0L;
    }

    public Operation[] toOperationsArray() {
        return toArray(new Operation[size()]);
    }
}
