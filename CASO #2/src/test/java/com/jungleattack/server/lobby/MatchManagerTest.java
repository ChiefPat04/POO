package com.jungleattack.server.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.jungleattack.server.game.GameException;
import com.jungleattack.server.game.GameSession;
import com.jungleattack.server.model.Player;
import com.jungleattack.server.util.GameConstants;

class MatchManagerTest {

    private final MatchManager manager = MatchManager.getInstance();

    @Test
    void theSameInstanceIsAlwaysReturned() {
        assertSame(MatchManager.getInstance(), MatchManager.getInstance());
    }

    @Test
    void codeHasFourValidCharacters() {
        GameSession session = manager.createMatch(new Player("Ana", null));
        String code = session.getCode();

        assertEquals(GameConstants.CODE_LENGTH, code.length());
        for (char c : code.toCharArray()) {
            assertTrue(GameConstants.CODE_CHARACTERS.indexOf(c) >= 0, "carácter no válido: " + c);
        }
    }

    @Test
    void activeCodesAreNotRepeated() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codes.add(manager.createMatch(new Player("Jugador" + i, null)).getCode());
        }
        assertEquals(100, codes.size()); // un Set no guarda repetidos
    }

    @Test
    void joinIgnoresCaseAndSpaces() throws GameException {
        GameSession created = manager.createMatch(new Player("Ana", null));
        String sloppy = "  " + created.getCode().toLowerCase() + " ";

        GameSession joined = manager.joinMatch(sloppy, new Player("Beto", null));
        assertSame(created, joined);
        assertTrue(joined.isFull());
    }

    @Test
    void badCodeIsRejected() {
        GameException e = assertThrows(GameException.class,
                () -> manager.joinMatch("ZZZZ-no-existe", new Player("Caro", null)));
        assertEquals(GameConstants.ERROR_BAD_CODE, e.getMessage());
    }

    @Test
    void fullMatchIsRejected() throws GameException {
        GameSession session = manager.createMatch(new Player("Ana", null));
        manager.joinMatch(session.getCode(), new Player("Beto", null));

        GameException e = assertThrows(GameException.class,
                () -> manager.joinMatch(session.getCode(), new Player("Caro", null)));
        assertEquals(GameConstants.ERROR_MATCH_FULL, e.getMessage());
    }

    @Test
    void removedMatchCannotBeJoined() {
        GameSession session = manager.createMatch(new Player("Ana", null));
        manager.removeMatch(session.getCode());

        assertThrows(GameException.class,
                () -> manager.joinMatch(session.getCode(), new Player("Beto", null)));
    }

    @Test
    void normalizeCodeTrimsAndUppercases() {
        assertEquals("A7K2", MatchManager.normalizeCode(" a7k2 "));
        assertEquals("", MatchManager.normalizeCode(null));
    }
}
