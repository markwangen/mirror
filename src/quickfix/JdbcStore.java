/****************************************************************************
** Copyright (c) 2001-2005 quickfixengine.org  All rights reserved.
**
** This file is part of the QuickFIX FIX Engine
**
** This file may be distributed under the terms of the quickfixengine.org
** license as defined by quickfixengine.org and appearing in the file
** LICENSE included in the packaging of this file.
**
** This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
** WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
**
** See http://www.quickfixengine.org/LICENSE for licensing information.
**
** Contact ask@quickfixengine.org if any conditions of this licensing are
** not clear to you.
**
****************************************************************************/

package quickfix;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

class JdbcStore implements MessageStore {
    private MemoryStore cache = new MemoryStore();
    private Connection connection;
    private SessionID sessionID;

    public JdbcStore(SessionSettings settings, SessionID sessionID) throws Exception {
        this.sessionID = sessionID;
        connection = connect(settings, sessionID);
        loadCache();
    }

    protected Connection connect(SessionSettings settings, SessionID sessionID)
            throws ClassNotFoundException, ConfigError, FieldConvertError, SQLException {
        return JdbcUtil.openConnection(settings, sessionID);
    }

    private void loadCache() throws SQLException, IOException {
        PreparedStatement query = connection
                .prepareStatement("SELECT creation_time, incoming_seqnum, "
                        + "outgoing_seqnum FROM sessions WHERE beginstring=? and  "
                        + "sendercompid=? and targetcompid=? and session_qualifier=?");
        query.setString(1, sessionID.getBeginString());
        query.setString(2, sessionID.getSenderCompID());
        query.setString(3, sessionID.getTargetCompID());
        query.setString(4, sessionID.getSessionQualifier());
        ResultSet rs = query.executeQuery();
        if (rs.next()) {
            Calendar timestamp = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            timestamp.setTime(rs.getDate(1));
            cache.setNextTargetMsgSeqNum(rs.getInt(2));
            cache.setNextSenderMsgSeqNum(rs.getInt(3));
        } else {
            PreparedStatement insert = connection
                    .prepareStatement("INSERT INTO sessions (beginstring, sendercompid, "
                            + "targetcompid, session_qualifier, creation_time, "
                            + "incoming_seqnum, outgoing_seqnum) VALUES(?,?,?,?,?,?,?)");
            insert.setString(1, sessionID.getBeginString());
            insert.setString(2, sessionID.getSenderCompID());
            insert.setString(3, sessionID.getTargetCompID());
            insert.setString(4, sessionID.getSessionQualifier());
            insert.setTimestamp(5, new Timestamp(cache.getCreationTime().getTime()));
            insert.setInt(6, cache.getNextTargetMsgSeqNum());
            insert.setInt(7, cache.getNextSenderMsgSeqNum());
            insert.execute();
        }
    }

    public Date getCreationTime() throws IOException {
        return cache.getCreationTime();
    }

    public int getNextSenderMsgSeqNum() throws IOException {
        return cache.getNextSenderMsgSeqNum();
    }

    public int getNextTargetMsgSeqNum() throws IOException {
        return cache.getNextTargetMsgSeqNum();
    }

    public void incrNextSenderMsgSeqNum() throws IOException {
        cache.incrNextSenderMsgSeqNum();
        setNextSenderMsgSeqNum(cache.getNextSenderMsgSeqNum());
    }

    public void incrNextTargetMsgSeqNum() throws IOException {
        cache.incrNextTargetMsgSeqNum();
        setNextTargetMsgSeqNum(cache.getNextTargetMsgSeqNum());
    }

    public void reset() throws IOException {
        cache.reset();
        try {
            PreparedStatement deleteMessages = connection
                    .prepareStatement("DELETE FROM messages WHERE beginstring=? and sendercompid=? "
                            + "and targetcompid=? and session_qualifier=?");
            deleteMessages.setString(1, sessionID.getBeginString());
            deleteMessages.setString(2, sessionID.getSenderCompID());
            deleteMessages.setString(3, sessionID.getTargetCompID());
            deleteMessages.setString(4, sessionID.getSessionQualifier());
            deleteMessages.execute();

            PreparedStatement updateTime = connection
                    .prepareStatement("UPDATE sessions SET creation_time=?, incoming_seqnum=?, outgoing_seqnum=? "
                            + "WHERE beginstring=? and sendercompid=? and targetcompid=? and session_qualifier=?");
            updateTime.setTimestamp(1, new Timestamp(Calendar.getInstance(
                    TimeZone.getTimeZone("UTC")).getTimeInMillis()));
            updateTime.setInt(2, getNextTargetMsgSeqNum());
            updateTime.setInt(3, getNextSenderMsgSeqNum());
            updateTime.setString(4, sessionID.getBeginString());
            updateTime.setString(5, sessionID.getSenderCompID());
            updateTime.setString(6, sessionID.getTargetCompID());
            updateTime.setString(7, sessionID.getSessionQualifier());
            updateTime.execute();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } catch (IOException e) {
            throw e;
        }
    }

    public void get(int startSequence, int endSequence, Collection messages) throws IOException {
        try {
            PreparedStatement query = connection
                    .prepareStatement("SELECT message FROM messages WHERE  beginstring=? and sendercompid=? and "
                            + "targetcompid=? and session_qualifier=? and msgseqnum>=? and msgseqnum<=? "
                            + "ORDER BY msgseqnum");
            query.setString(1, sessionID.getBeginString());
            query.setString(2, sessionID.getSenderCompID());
            query.setString(3, sessionID.getTargetCompID());
            query.setString(4, sessionID.getSessionQualifier());
            query.setInt(5, startSequence);
            query.setInt(6, endSequence);
            ResultSet rs = query.executeQuery();
            while (rs.next()) {
                messages.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean set(int sequence, String message) throws IOException {
        try {
            PreparedStatement insert = connection
                    .prepareStatement("INSERT INTO messages (beginstring, sendercompid, targetcompid, "
                            + "session_qualifier, msgseqnum, message) VALUES (?,?,?,?,?,?)");
            insert.setString(1, sessionID.getBeginString());
            insert.setString(2, sessionID.getSenderCompID());
            insert.setString(3, sessionID.getTargetCompID());
            insert.setString(4, sessionID.getSessionQualifier());
            insert.setInt(5, sequence);
            insert.setString(6, message);
            insert.execute();
        } catch (SQLException ex) {
            try {
                PreparedStatement update = connection
                        .prepareStatement("UPDATE messages SET message=? WHERE beginstring=? and sendercompid=? "
                                + "and targetcompid=? and session_qualifier=? and msgseqnum=?");
                update.setString(1, sessionID.getBeginString());
                update.setString(2, sessionID.getSenderCompID());
                update.setString(3, sessionID.getTargetCompID());
                update.setString(4, sessionID.getSessionQualifier());
                update.setInt(5, sequence);
                update.setString(6, message);
                update.execute();
                // TODO QUESTION determine why the update is here
                return false;
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
        return true;
    }

    public void setNextSenderMsgSeqNum(int next) throws IOException {
        cache.setNextSenderMsgSeqNum(next);
        storeSequenceNumber(next, "outgoing_seqnum");
    }

    public void setNextTargetMsgSeqNum(int next) throws IOException {
        cache.setNextTargetMsgSeqNum(next);
        storeSequenceNumber(next, "incoming_seqnum");
    }

    private void storeSequenceNumber(int next, String column) throws IOException {
        try {
            PreparedStatement update = connection.prepareStatement("UPDATE sessions SET " + column
                    + "=? WHERE beginstring=? and sendercompid=? "
                    + "and targetcompid=? and session_qualifier=?");
            update.setInt(1, next);
            update.setString(2, sessionID.getBeginString());
            update.setString(3, sessionID.getSenderCompID());
            update.setString(4, sessionID.getTargetCompID());
            update.setString(5, sessionID.getSessionQualifier());
            update.execute();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

}