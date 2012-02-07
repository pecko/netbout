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
package com.netbout.inf.predicates.text;

import com.netbout.inf.MsgMocker;
import com.netbout.inf.Predicate;
import com.netbout.inf.PredicateMocker;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

/**
 * Test case of {@link MatchesPred}.
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
@SuppressWarnings({
    "PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"
})
public final class MatchesPredTest {

    /**
     * MatchesPred can match empty text.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void positivelyMatchesEmptyText() throws Exception {
        final Predicate pred = new MatchesPred(
            Arrays.asList(
                new Predicate[] {
                    new PredicateMocker().doReturn("").mock(),
                    new PredicateMocker().doReturn("some text").mock(),
                }
            )
        );
        MatcherAssert.assertThat(
            "matched",
            (Boolean) pred.evaluate(new MsgMocker().mock(), 0)
        );
    }

    /**
     * MatchesPred can match by keyword or a combination of them.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void positivelyMatchesByKeywords() throws Exception {
        final Map<String, String> matches = ArrayUtils.toMap(
            new String[][] {
                {"", ""},
                {"", "hello dear friend, how are you?"},
                {"up?", "hi there, what's up"},
                {"any time", "You can call me any time, really!"},
                {"jeff lebowski", "the dude is Jeff Bridges (Lebowski)"},
            }
        );
        for (Map.Entry<String, String> entry : matches.entrySet()) {
            final Predicate pred = new MatchesPred(
                Arrays.asList(
                    new Predicate[] {
                        new PredicateMocker().doReturn(entry.getKey()).mock(),
                        new PredicateMocker().doReturn(entry.getValue()).mock(),
                    }
                )
            );
            MatcherAssert.assertThat(
                String.format(
                    "matches '%s' in '%s' as expected",
                    entry.getKey(),
                    entry.getValue()
                ),
                (Boolean) pred.evaluate(new MsgMocker().mock(), 0)
            );
        }
    }

    /**
     * MatchesPred can avoid matching when it's not necessary.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void doesntMatchWhenItShouldnt() throws Exception {
        final Map<String, String> matches = ArrayUtils.toMap(
            new String[][] {
                {"boy", "short story about some girls"},
            }
        );
        for (Map.Entry<String, String> entry : matches.entrySet()) {
            final Predicate pred = new MatchesPred(
                Arrays.asList(
                    new Predicate[] {
                        new PredicateMocker().doReturn(entry.getKey()).mock(),
                        new PredicateMocker().doReturn(entry.getValue()).mock(),
                    }
                )
            );
            MatcherAssert.assertThat(
                String.format(
                    "doesn't match '%s' in '%s' as expected",
                    entry.getKey(),
                    entry.getValue()
                ),
                !(Boolean) pred.evaluate(new MsgMocker().mock(), 0)
            );
        }
    }

}