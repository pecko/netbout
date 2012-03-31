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
package com.netbout.inf.index;

import com.netbout.spi.Urn;
import java.util.concurrent.ConcurrentMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * Test case of {@link FsIndex}.
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
public final class FsIndexTest {

    /**
     * Temporary folder.
     * @checkstyle VisibilityModifier (3 lines)
     */
    @Rule
    public transient TemporaryFolder temp = new TemporaryFolder();

    /**
     * Temporary folder.
     */
    private transient Folder folder;

    /**
     * Create temporary folder.
     * @throws Exception If there is some problem inside
     */
    @Before
    public void createFolder() throws Exception {
        this.folder = Mockito.mock(Folder.class);
        Mockito.doReturn(this.temp.newFolder("foo")).when(this.folder).path();
    }

    /**
     * FsIndex can persist itself.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void persistsItself() throws Exception {
        final FsIndex index = new FsIndex(this.folder);
        final String name = "some-test-name";
        final ConcurrentMap<Long, Urn> map = index.get(name);
        final Urn urn = new Urn("urn:test:abc");
        map.put(1L, urn);
        index.flush();
        MatcherAssert.assertThat(
            this.folder.path().list(),
            Matchers.arrayWithSize(Matchers.greaterThan(0))
        );
        MatcherAssert.assertThat(
            new FsIndex(this.folder).<Long, Urn>get(name).get(1L),
            Matchers.equalTo(urn)
        );
    }

    /**
     * FsIndex can return statistics.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void producesStatisticsAsText() throws Exception {
        MatcherAssert.assertThat(
            new FsIndex(this.folder).statistics(),
            Matchers.notNullValue()
        );
    }

}