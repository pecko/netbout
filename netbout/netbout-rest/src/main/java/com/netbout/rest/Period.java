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
package com.netbout.rest;

import com.ymock.util.Logger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.joda.time.Interval;

/**
 * Period.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
final class Period {

    /**
     * Maximum number of items to show, no matter what.
     */
    private static final int MAX = 5;

    /**
     * Minimum number of items to show, no matter what.
     */
    private static final int MIN = 3;

    /**
     * Add dates in it.
     */
    private final transient SortedSet<Date> dates = new TreeSet<Date>();

    /**
     * Start of the period, the newest date.
     */
    private final transient Date start;

    /**
     * Maximum distance between dates, in milliseconds.
     */
    private final transient long limit;

    /**
     * Public ctor.
     */
    public Period() {
        this(new Date(), 5L * 24 * 60 * 60 * 1000);
    }

    /**
     * Public ctor.
     * @param from When to start
     * @param lmt The limit
     */
    private Period(final Date from, final long lmt) {
        this.start = from;
        this.limit = lmt;
    }

    /**
     * Build it back from text.
     * @param text The text
     * @return The period discovered
     */
    public static Period valueOf(final String text) {
        Period period;
        if (text != null && text.matches("^\\d+\\-\\d+$")) {
            final String[] parts = text.split("-");
            period = new Period(
                new Date(Long.valueOf(parts[0])),
                Long.valueOf(parts[1])
            );
        } else {
            period = new Period();
        }
        return period;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format(
            "%d-%d",
            this.start.getTime(),
            this.limit
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof Period)
            && ((Period) obj).start.equals(this.start)
            && ((Period) obj).limit == this.limit;
    }

    /**
     * This date fits into this period?
     * @param date The date
     * @return Whether it can be accepted in this period ({@code TRUE}) or
     *  this period is full and we should use {@link #next()} in order to get
     *  the next one
     */
    public boolean fits(final Date date) {
        final boolean offlimit = date.after(
            new Date(this.start.getTime() - this.limit)
        );
        final boolean overflow = this.dates.size() >= this.MAX
            && (this.dates.last().getTime() - this.dates.first().getTime()) > 1000 * 60L;
        return !overflow
            && !date.after(this.start)
            && (this.dates.size() < this.MIN || !offlimit);
    }

    /**
     * Add new date to it.
     * @param date The date
     */
    public void add(final Date date) {
        if (!this.fits(date)) {
            throw new IllegalArgumentException(
                String.format(
                    "Can't add %s, call #fits() first",
                    date
                )
            );
        }
        this.dates.add(date);
    }

    /**
     * Get next period.
     * @param date This date should start the new one
     * @return The period, which goes right after this one and is
     *  one size bigger
     */
    public Period next(final Date date) {
        return new Period(date, this.limit * 2);
    }

    /**
     * Newest date.
     * @return The date
     */
    public Date newest() {
        return this.start;
    }

    /**
     * Convert it to text.
     * @return Text presentation of this period
     */
    public String title() {
        final org.joda.time.Period distance = new Interval(
            this.start.getTime(),
            new Date().getTime()
        ).toPeriod();
        String title;
        if (distance.getYears() > 0) {
            title = String.format("%d years", distance.getYears());
        } else if (distance.getMonths() > 0) {
            title = String.format("%d months", distance.getMonths());
        } else if (distance.getWeeks() > 0) {
            title = String.format("%d weeks", distance.getWeeks());
        } else if (distance.getDays() > 0) {
            title = String.format("%d days", distance.getDays());
        } else if (distance.getHours() > 0) {
            title = String.format("%d hours", distance.getHours());
        } else if (distance.getMinutes() > 0) {
            title = String.format("%d mins", distance.getMinutes());
        } else {
            title = String.format("%d secs", distance.getSeconds());
        }
        return String.format("%s ago", title);
    }

}
