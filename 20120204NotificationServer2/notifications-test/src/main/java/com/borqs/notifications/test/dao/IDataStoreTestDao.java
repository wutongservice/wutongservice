package com.borqs.notifications.test.dao;

public interface IDataStoreTestDao {
	public abstract void init() throws Exception;

	public abstract void clearDatabase();

	public abstract void endPhase();

	public abstract void testCount() throws Exception;

	public abstract void testInsert(long count) throws Exception;

	public abstract void testUpdate() throws Exception;

	public abstract void destroy();

	public abstract void testQuery() throws Exception;

}