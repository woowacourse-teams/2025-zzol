package coffeeshout.room.infra.persistence.nickname;

import coffeeshout.room.application.port.CustomProfanityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface CustomProfanityJpaRepository extends Repository<CustomProfanityEntity, Long>, CustomProfanityRepository {

    // AI 심사 자동 차단 전용 — source = AI_AUDIT, 중복 시 무시
    @Override
    @Modifying
    @Query(value = "INSERT IGNORE INTO custom_profanity (word, source, created_at) VALUES (:word, :source, NOW(6))",
            nativeQuery = true)
    int insertIgnore(String word, String source);

    // 운영자 수동 차단 — source = OPERATOR_MANUAL, AI_AUDIT으로 이미 등록된 경우 source 업그레이드
    @Override
    @Modifying
    @Query(value = "INSERT INTO custom_profanity (word, source, created_at) VALUES (:word, 'OPERATOR_MANUAL', NOW(6))"
            + " ON DUPLICATE KEY UPDATE source = 'OPERATOR_MANUAL'",
            nativeQuery = true)
    void upsertOperatorManual(String word);

    // 운영자 허용 처리 — AI가 추가한 엔트리만 삭제, OPERATOR_MANUAL은 보존
    @Override
    @Modifying
    @Query(value = "DELETE FROM custom_profanity WHERE word = :word AND source = 'AI_AUDIT'",
            nativeQuery = true)
    int deleteAiAuditByWord(String word);

    @Override
    @Query("SELECT c.word FROM CustomProfanityEntity c ORDER BY c.id ASC")
    Slice<String> findWords(Pageable pageable);
}
