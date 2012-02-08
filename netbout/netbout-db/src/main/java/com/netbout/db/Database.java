/**
 * Copyright (c) 2009-2011, netBout.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are PROHIBITED without prior written permission from
 * the author. This product may NOT be used anywhere and on any computer
 * except the server platform of netBout Inc. located at www.netbout.com.
 * Federal copyright law prohibits unauthorized reproduction by any means
 * and imposes fines up to $25,000 for violation. If you received
 * this code occasionally and without intent to use it, please report this
 * incident to the author by email.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package com.netbout.db;

import com.ymock.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.dbutils.DbUtils;

/**
 * Database-related utility class.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
final class Database {

    /**
     * Singleton instance, lazy loaded in {@link #connection()}.
     */
    private static Database instance;

    /**
     * Datasource to use.
     */
    private final transient DataSource source = new DataSourceBuilder().build();

    /**
     * Protected ctor.
     */
    protected Database() {
        Logger.info(Database.class, "#Database(): instantiated");
    }

    /**
     * Drop all connections.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void drop() {
        synchronized (Database.class) {
            Database.instance = null;
        }
    }

    /**
     * Convenient method to get a new JDBC connection.
     * @return New JDBC connection
     * @throws SQLException If some SQL error
     */
    @SuppressWarnings("PMD.CloseResource")
    public static Connection connection() throws SQLException {
        synchronized (Database.class) {
            if (Database.instance == null) {
                Database.instance = new Database();
                final Connection conn = Database.instance.connect();
                Database.update(conn);
                DbUtils.closeQuietly(conn);
            }
            return Database.instance.connect();
        }
    }

    /**
     * Create JDBC connection.
     * @return New JDBC connection
     * @throws SQLException If some SQL error
     */
    protected Connection connect() throws SQLException {
        return this.source.getConnection();
    }

    /**
     * Update DB schema to the latest version.
     * @param connection JDBC connection to use
     */
    private static void update(final Connection connection) {
        final long start = System.currentTimeMillis();
        try {
            final Liquibase liquibase = new Liquibase(
                "com/netbout/db/liquibase.xml",
                new ClassLoaderResourceAccessor(),
                new JdbcConnection(connection)
            );
            liquibase.update("netbout");
        } catch (liquibase.exception.LiquibaseException ex) {
            throw new IllegalStateException(ex);
        }
        Logger.info(
            Database.class,
            "#update(): updated DB schema [%dms]",
            System.currentTimeMillis() - start
        );
    }

}
