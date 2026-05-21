package coffeeshout.room.infra;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import coffeeshout.room.domain.service.ProfanityChecker;
import coffeeshout.room.infra.event.ProfanityWordAllowedEvent;
import coffeeshout.global.event.ProfanityWordBlockedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfanityCheckerSyncListenerTest {

    ProfanityChecker profanityChecker;
    ProfanityCheckerSyncListener listener;

    @BeforeEach
    void setUp() {
        profanityChecker = mock(ProfanityChecker.class);
        listener = new ProfanityCheckerSyncListener(profanityChecker);
    }

    @Nested
    class onWordBlocked {

        @Test
        void ProfanityWordBlockedEvent를_수신하면_ProfanityChecker에_add를_호출한다() {
            listener.onWordBlocked(new ProfanityWordBlockedEvent("욕설닉네임"));

            verify(profanityChecker).add("욕설닉네임");
        }
    }

    @Nested
    class onWordAllowed {

        @Test
        void ProfanityWordAllowedEvent를_수신하면_ProfanityChecker에_remove를_호출한다() {
            listener.onWordAllowed(new ProfanityWordAllowedEvent("욕설닉네임"));

            verify(profanityChecker).remove("욕설닉네임");
        }
    }
}
