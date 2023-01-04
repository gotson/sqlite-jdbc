/*--------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

public class SQLiteConnectionPoolDataSourceTest {

    @Test
    public void connectionTest() throws SQLException {
        ConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();

        PooledConnection pooledConn = ds.getPooledConnection();

        Connection handle = pooledConn.getConnection();
        assertThat(handle.isClosed()).isFalse();
        assertThat(handle.createStatement().execute("select 1")).isTrue();

        Connection handle2 = pooledConn.getConnection();
        assertThat(handle.isClosed()).isTrue();
        Connection finalHandle = handle;
        assertThatThrownBy(() -> finalHandle.createStatement().execute("select 1"))
                .isInstanceOf(SQLException.class)
                .hasMessage("Connection is closed");

        assertThat(handle2.createStatement().execute("select 1")).isTrue();
        handle2.close();

        handle = pooledConn.getConnection();
        assertThat(handle.createStatement().execute("select 1")).isTrue();

        pooledConn.close();
        assertThat(handle.isClosed()).isTrue();
    }

    @Disabled
    @Test
    public void proxyConnectionCloseTest() throws SQLException {
        ConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
        PooledConnection pooledConn = ds.getPooledConnection();
        System.out.println("pooledConn: " + pooledConn.getClass());

        Connection handle = pooledConn.getConnection();
        System.out.println("pooledConn.getConnection: " + handle.getClass());

        Statement st = handle.createStatement();
        System.out.println("statement: " + st.getClass());
        Connection stConn = handle.createStatement().getConnection();
        System.out.println("statement connection:" + stConn.getClass());
        stConn.close(); // This closes the physical connection, not the proxy

        Connection handle2 = pooledConn.getConnection();
    }

    @Test
    public void raceConditionTest() throws Exception {
        ConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
        PooledConnection pooledConn = ds.getPooledConnection();

        List<Thread> threads = new ArrayList<>();
        AtomicBoolean hasError = new AtomicBoolean(false);

        for (int i = 0; i < 200; i++) {
            Thread thread = new Thread(() -> {
                for (int i1 = 0; i1 < 200000 && !hasError.get(); i1++) {
                    try {
                        try (Connection connection = pooledConn.getConnection()) {
                            connection.setAutoCommit(false);
                        }
                    } catch (Exception e) {
                        hasError.set(true);
                        System.out.println("worker error");
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (final Thread t : threads) {
            t.join();
        }

        assertThat(hasError.get()).isFalse();
    }

}
