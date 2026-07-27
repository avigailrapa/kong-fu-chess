package src.server.core;

import java.util.HashMap;
import java.util.Map;

public class SessionRegistry {

    private final Map<Object, Session> sessionsByConnection = new HashMap<>();
    private final Map<Session, Match> matchBySession = new HashMap<>();

    public Session sessionFor(Object conn) {
        return sessionsByConnection.get(conn);
    }

    public Match matchFor(Object conn) {
        Session session = sessionsByConnection.get(conn);
        return session == null ? null : matchBySession.get(session);
    }

    public Match matchFor(Session session) {
        return matchBySession.get(session);
    }

    public void register(Object conn, Session session) {
        sessionsByConnection.put(conn, session);
    }

    public Session unregister(Object conn) {
        return sessionsByConnection.remove(conn);
    }

    public void bind(Session session, Match match) {
        matchBySession.put(session, match);
    }

    public boolean isBound(Session session) {
        return matchBySession.containsKey(session);
    }
}
