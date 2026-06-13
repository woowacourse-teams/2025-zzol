package coffeeshout.room.application.event;

import coffeeshout.RoomModuleIntegrationTest;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.PlayerSnapshotRequiredEvent;
import coffeeshout.room.application.port.RoomEntityRepository;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PlayerEntity 생성 책임 이관의 <b>크로스 모듈 이벤트 배선</b>을 end-to-end로 검증한다.
 *
 * <p>실제 {@link ApplicationEventPublisher}로 {@link PlayerSnapshotRequiredEvent}를 발행하면, 실제
 * {@link PlayerSnapshotListener}가 동기 수신해 방 플레이어를 {@link PlayerEntity}로 실제 DB에 저장하는지 확인한다.
 * 단위 테스트({@code PlayerSnapshotListenerTest})가 모킹한 이벤트 경계·영속을 실제 Spring 컨텍스트·TestContainer DB로
 * 메운다(발행자 트랜잭션 안에서 리스너가 실행됨을 {@link TransactionTemplate}으로 재현).
 */
class PlayerSnapshotListenerIntegrationTest extends RoomModuleIntegrationTest {

    private static final JoinCode JOIN_CODE = new JoinCode("P7K9");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private RoomEntityRepository roomEntityRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("PlayerSnapshotRequiredEvent를 발행하면 리스너가 방 플레이어를 PlayerEntity로 DB에 저장한다")
    void 이벤트_발행으로_플레이어_스냅샷이_DB에_저장된다() {
        // given — 방 도메인(MemoryRepository)과 RoomEntity(DB)를 준비한다
        final Room room = RoomFixture.호스트_꾹이(JOIN_CODE);
        roomRepository.save(room);

        final TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status ->
                roomJpaRepository.save(new RoomEntity(JOIN_CODE.getValue())));

        // when — 실제 발행자가 실제 리스너로 디스패치한다(발행자 트랜잭션 안에서 동기 실행, 운영과 동일)
        txTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new PlayerSnapshotRequiredEvent(JOIN_CODE.getValue())));

        // then — 방 플레이어 전원이 PlayerEntity로 실제 DB에 저장된다
        final RoomEntity savedRoomEntity = roomEntityRepository
                .findFirstByJoinCodeOrderByCreatedAtDesc(JOIN_CODE.getValue())
                .orElseThrow();
        final List<PlayerEntity> savedPlayers = playerJpaRepository.findAllByRoomSession(savedRoomEntity);

        final List<String> savedNames = savedPlayers.stream().map(PlayerEntity::getPlayerName).toList();
        final List<String> expectedNames = room.getPlayers().stream()
                .map(player -> player.getName().value())
                .toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(savedPlayers).hasSize(expectedNames.size());
            softly.assertThat(savedNames).containsExactlyInAnyOrderElementsOf(expectedNames);
        });
    }
}
