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
package com.netbout.hub;

import com.netbout.spi.Bout;
import com.netbout.spi.Identity;
import com.netbout.spi.Message;
import com.netbout.spi.MessageNotFoundException;
import com.netbout.spi.Participant;
import com.ymock.util.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Identity.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class HubBout implements Bout {

    /**
     * The hub.
     */
    private final transient Hub hub;

    /**
     * The viewer.
     */
    private final transient Identity viewer;

    /**
     * The data.
     */
    private final transient BoutDt data;

    /**
     * Public ctor.
     * @param ihub The hub
     * @param idnt The viewer
     * @param dat The data
     */
    public HubBout(final Hub ihub, final Identity idnt, final BoutDt dat) {
        this.hub = ihub;
        this.viewer = idnt;
        this.data = dat;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Bout bout) {
        final String query = "(equal $pos 0)";
        final List<Message> mine = this.messages(query);
        final List<Message> his = bout.messages(query);
        int result = this.date().compareTo(bout.date());
        if (!mine.isEmpty() && !his.isEmpty()) {
            result = mine.get(0).date().compareTo(his.get(0).date());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long number() {
        return this.data.getNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String title() {
        return this.data.getTitle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date date() {
        return this.data.getDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void confirm() {
        this.data.confirm(this.viewer.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave() {
        this.data.kickOff(this.viewer.name());
        if (this.viewer instanceof InvitationSensitive) {
            ((InvitationSensitive) this.viewer).kickedOff(this.number());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(final String text) {
        if (!this.confirmed()) {
            throw new IllegalStateException("You can't rename until you join");
        }
        this.data.setTitle(text);
    }

    /**
     * {@inheritDoc}
     * @checkstyle RedundantThrows (4 lines)
     */
    @Override
    public Participant invite(final Identity friend) {
        if (!this.confirmed()) {
            throw new IllegalStateException("You can't invite until you join");
        }
        final ParticipantDt dude = this.data.addParticipant(friend.name());
        Logger.debug(
            this,
            "#invite('%s'): success",
            friend
        );
        if (friend instanceof InvitationSensitive) {
            ((InvitationSensitive) friend).invited(this);
        }
        return new HubParticipant(this.hub, this, dude, this.data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Collection<Participant> participants() {
        final Collection<Participant> participants
            = new ArrayList<Participant>();
        for (ParticipantDt dude : this.data.getParticipants()) {
            participants.add(
                new HubParticipant(this.hub, this, dude, this.data)
            );
        }
        Logger.debug(
            this,
            "#participants(): %d participant(s) found in bout #%d",
            participants.size(),
            this.number()
        );
        return participants;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public List<Message> messages(final String query) {
        final List<MessageDt> datas =
            new ArrayList<MessageDt>(this.data.getMessages());
        final List<Message> messages = new ArrayList<Message>();
        for (MessageDt msg : datas) {
            messages.add(new HubMessage(this.hub, this.viewer, this, msg));
        }
        Collections.sort(messages, Collections.reverseOrder());
        final List<Message> result = new ArrayList<Message>();
        final Predicate predicate = new PredicateBuilder(this.hub).parse(query);
        for (Message msg : messages) {
            if (query.isEmpty()
                || (Boolean) predicate.evaluate(msg, result.size())) {
                result.add(msg);
            }
        }
        Logger.debug(
            this,
            "#messages('%s'): %d message(s) found",
            query,
            result.size()
        );
        return result;
    }

    /**
     * {@inheritDoc}
     * @checkstyle RedundantThrows (4 lines)
     */
    @Override
    public Message message(final Long num) throws MessageNotFoundException {
        final Message message = new HubMessage(
            this.hub,
            this.viewer,
            this,
            this.data.findMessage(num)
        );
        Logger.debug(
            this,
            "#message(#%d): found",
            num
        );
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message post(final String text) {
        if (!this.confirmed()) {
            throw new IllegalStateException("You can't post until you join");
        }
        final MessageDt msg = this.data.addMessage();
        msg.setDate(new Date());
        msg.setAuthor(this.viewer.name());
        msg.setText(text);
        Logger.debug(
            this,
            "#post('%s'): message posted",
            text
        );
        final Message message = new HubMessage(
            this.hub,
            this.viewer,
            this,
            msg
        );
        message.text();
        this.hub.make("notify-bout-participants")
            .arg(this.number())
            .arg(message.number())
            .asDefault(false)
            .exec();
        return message;
    }

    /**
     * This identity has confirmed participation?
     * @return Is it?
     */
    private boolean confirmed() {
        for (Participant dude : this.participants()) {
            if (dude.identity().equals(this.viewer)) {
                return dude.confirmed();
            }
        }
        throw new IllegalStateException("Can't find myself in participants");
    }

}
