package coffeeshout.room.domain.repository;

import coffeeshout.gamecommon.JoinCode;

public interface JoinCodeRepository {

    boolean save(JoinCode joinCode);
}
