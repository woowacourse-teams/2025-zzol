package coffeeshout.profanity.application;

import java.util.Set;

/**
 * 검열 행이 없는 닉네임이 랭킹에 노출됐다. 검열 큐에 넣어 판정을 받게 한다(#1906).
 *
 * <p>운영자 허용 닉네임은 발행 쪽이 이미 걸러 담지 않는다.
 */
public record UnregisteredNicknamesExposedEvent(Set<String> nicknames) {}
