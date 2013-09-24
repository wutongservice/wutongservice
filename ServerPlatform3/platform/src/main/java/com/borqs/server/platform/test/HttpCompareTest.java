package com.borqs.server.platform.test;


import com.borqs.server.platform.web.DualHttpApiClient;
import com.borqs.server.platform.web.HttpApiClient;
import org.junit.After;
import org.junit.Before;

public abstract class HttpCompareTest {
    protected DualHttpApiClient dualClient;

    protected HttpCompareTest() {
    }

    protected abstract HttpApiClient createClient1();

    protected abstract HttpApiClient createClient2();

    @Before
    void setUp() {
        dualClient = new DualHttpApiClient(createClient1(), createClient2());
    }

    @After
    void tearDown() {
        dualClient = null;
    }

}
