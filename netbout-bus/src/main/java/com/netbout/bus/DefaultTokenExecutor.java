/**
 * Copyright (c) 2009-2012, Netbout.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are PROHIBITED without prior written permission from
 * the author. This product may NOT be used anywhere and on any computer
 * except the server platform of netBout Inc. located at www.netbout.com.
 * Federal copyright law prohibits unauthorized reproduction by any means
 * and imposes fines up to $25,000 for violation. If you received
 * this code accidentally and without intent to use it, please report this
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
package com.netbout.bus;

import com.jcabi.log.Logger;
import com.jcabi.urn.URN;
import com.netbout.bh.StatsProvider;
import com.netbout.spi.Bout;
import com.netbout.spi.Helper;
import com.netbout.spi.Identity;
import com.netbout.spi.Participant;
import com.netbout.spi.Plain;
import com.netbout.spi.plain.PlainList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Default executor of a token.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
final class DefaultTokenExecutor implements TokenExecutor, StatsProvider {

    /**
     * List of registered helpers.
     */
    private final transient ConcurrentMap<URN, Helper> helpers =
        new ConcurrentHashMap<URN, Helper>();

    /**
     * Consumption bills.
     */
    private final transient List<Bill> bills = new CopyOnWriteArrayList<Bill>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(final Identity identity, final Helper helper) {
        if (this.helpers.containsKey(identity.name())) {
            throw new IllegalArgumentException(
                String.format(
                    "Identity '%s' has already been registered as '%s'",
                    identity.name(),
                    helper.location()
                )
            );
        }
        if (this.helpers.containsValue(helper)) {
            throw new IllegalArgumentException(
                String.format(
                    "Helper '%s' has already been registered",
                    helper.location()
                )
            );
        }
        for (Map.Entry<URN, Helper> entry : this.helpers.entrySet()) {
            if (entry.getValue().location().equals(helper.location())) {
                throw new IllegalArgumentException(
                    String.format(
                        "Identity '%s' already registered '%s' location",
                        entry.getKey(),
                        helper.location()
                    )
                );
            }
        }
        this.helpers.put(identity.name(), helper);
        Logger.debug(
            this,
            "#register(%s): registered (%d total now)",
            helper,
            this.helpers.size()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exec(final TxToken token) {
        final Bill bill = this.run(token, this.helpers.entrySet());
        this.save(bill);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void exec(final TxToken token, final Bout bout) {
        final Set<Map.Entry<URN, Helper>> active =
            new HashSet<Map.Entry<URN, Helper>>();
        for (Participant participant : bout.participants()) {
            final URN name = participant.name();
            if (this.helpers.containsKey(name)) {
                active.add(
                    new AbstractMap.SimpleEntry<URN, Helper>(
                        name,
                        this.helpers.get(name)
                    )
                );
            }
        }
        final Bill bill = this.run(token, active);
        bill.inBout(bout.number());
        this.save(bill);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String statistics() {
        final StringBuilder text = new StringBuilder();
        for (Map.Entry<URN, Helper> entry : this.helpers.entrySet()) {
            text.append(
                Logger.format(
                    "%s as %s with %[list]s\n",
                    entry.getKey(),
                    entry.getValue().location(),
                    entry.getValue().supports()
                )
            );
        }
        for (Bill archive : this.bills) {
            text.append("\n").append(archive.toString());
        }
        return text.toString();
    }

    /**
     * Execute given token with a given set of helpers.
     * @param token The token to execute
     * @param targets The helpers to use
     * @return The bill
     */
    private Bill run(final TxToken token,
        final Set<Map.Entry<URN, Helper>> targets) {
        final String mnemo = token.mnemo();
        final Bill bill = new Bill(mnemo);
        for (Map.Entry<URN, Helper> helper : targets) {
            if (helper.getValue().supports().contains(mnemo)) {
                final long start = System.nanoTime();
                helper.getValue().execute(token);
                if (token.isCompleted()) {
                    bill.done(
                        helper.getKey().toString(),
                        System.nanoTime() - start
                    );
                    break;
                }
            }
        }
        Logger.debug(
            this,
            "#run(%s, %d helpers): returned [%s]",
            token,
            targets.size(),
            token.getResult()
        );
        return bill;
    }

    /**
     * Save bill to archive.
     * @param bill The bill to save
     */
    private void save(final Bill bill) {
        synchronized (this.bills) {
            // @checkstyle MagicNumber (1 line)
            if (this.bills.size() > 50) {
                final List<String> lines = new ArrayList<String>();
                for (Bill archive : this.bills) {
                    lines.add(archive.toString());
                }
                this.bills.clear();
                this.exec(
                    new DefaultTxToken(
                        "save-bills",
                        Arrays.asList(
                            new Plain<?>[] {new PlainList<String>(lines)}
                        )
                    )
                );
            }
        }
        if (bill.isDone()) {
            this.bills.add(bill);
        }
    }

    /**
     * One bill.
     */
    private static final class Bill {
        /**
         * Minimum amount of nanosec to report.
         */
        private static final long MIN_NANOSEC = 1L * 1000 * 1000;
        /**
         * When it happened.
         */
        private final transient Date date = new Date();
        /**
         * Mnemo.
         */
        private final transient String mnemo;
        /**
         * Helper.
         */
        private transient String helper;
        /**
         * Nanoseconds.
         */
        private transient Long nano;
        /**
         * Bout.
         */
        private transient Long number;
        /**
         * Public ctor.
         * @param mnem The token we just executed
         */
        public Bill(final String mnem) {
            this.mnemo = mnem;
        }
        /**
         * Mark it done.
         * @param hlpr Who completed
         * @param nsec How long did it take
         */
        public void done(final String hlpr, final Long nsec) {
            this.helper = hlpr;
            this.nano = nsec;
        }
        /**
         * It is related to this bout.
         * @param num The bout
         */
        public void inBout(final Long num) {
            this.number = num;
        }
        /**
         * It is done?
         * @return Yes or no
         */
        public boolean isDone() {
            return this.nano != null
                && this.nano >= DefaultTokenExecutor.Bill.MIN_NANOSEC
                && this.helper != null
                && this.number != null;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format(
                "%s %-30s %-25s %6d #%-5d",
                ISODateTimeFormat.dateTime().print(new DateTime(this.date)),
                this.mnemo,
                this.helper,
                // @checkstyle MagicNumber (1 line)
                this.nano / (1000 * 1000),
                this.number
            );
        }
    }

}