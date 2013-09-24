package com.borqs.server.qiupu.util.market;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;

public interface AsyncTaskListener {
	public void asyncRead(final Record apk, RecordHandler handler);
}