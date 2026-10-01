package coffeeshout.admin.overview.domain;

/**
 * 방 진행 퍼널. 이 서비스에서 "얼마나 잘 되고 있나"에 가장 직접적으로 답하는 지표다.
 *
 * <p>단계는 {@code room_session.roomStatus}로 판정한다. 그 값이 방이 도달한 최대 단계를
 * 그대로 들고 있기 때문이다. READY 로 생성되고, 게임이 시작되면 PLAYING, 룰렛에 들어가면
 * ROULETTE, 끝나면 DONE 으로 갱신된다.
 *
 * <p><b>SCORE_BOARD 는 세지 않는다.</b> 그 상태는 메모리 안에서만 존재하고 DB 로 내려가지
 * 않는다(Room.java 가 필드만 바꾸고 RoomStatusPort 를 부르지 않는다). 없는 단계를 표에
 * 그려 두면 언제나 0 이 찍혀 지표를 못 믿게 된다.
 *
 * <p><b>mini_game_play 로 "게임 시작"을 세지 않는다.</b> 그 행은 게임이 <b>시작될 때</b>
 * 쌓이므로(MiniGamePersistenceService) roomStatus 와 거의 같은 것을 센다. 두 단계가 같은
 * 것을 세면 그 사이에서 빠지는 방이 없어 퍼널의 한 줄이 놀게 된다.
 *
 * @param created      방 생성 수
 * @param miniGamePlayed 미니게임을 한 판이라도 끝낸 방
 *                     <p>한때 이 자리에 "2인 이상 입장"이 있었다. 뺐다. 2인부터 게임이
 *                     되므로 게임을 시작한 방은 전부 2인 이상이고, 그래서 그 칸은 늘
 *                     100%를 찍었다. 언제나 100%인 칸은 아무것도 알려주지 않으면서
 *                     퍼널의 한 줄을 차지한다.
 *
 *                     <p>대신 여기를 본다. 판정 근거는 {@code mini_game_play} 가 아니라
 *                     <b>결과({@code mini_game_result})</b>다. 판 행은 게임이 시작될 때
 *                     쌓여서 그것만 보면 시작한 방까지 완료로 세지고, 실제로 그렇게 세다가
 *                     이 단계가 직전 단계보다 커진 적이 있다.
 *
 *                     <p>게임 시작과 이 단계 사이의 차이가 곧 <b>하다가 나간 방</b>이다.
 *                     그건 실제로 손을 쓸 수 있는 신호다 - 특정 게임이 자꾸 중간에
 *                     버려지는지를 게임별 플레이와 나란히 놓고 볼 수 있다.
 * @param gameStarted  게임을 시작한 방 (roomStatus 가 READY 를 벗어남)
 * @param rouletteReached 룰렛까지 간 방
 * @param completed    끝까지 간 방 (DONE)
 */
public record RoomFunnel(long created, long gameStarted, long miniGamePlayed, long rouletteReached, long completed) {

    public static RoomFunnel empty() {
        return new RoomFunnel(0, 0, 0, 0, 0);
    }

    /** 방 생성 대비 완주 비율. 0.0 ~ 1.0. 생성이 없으면 0. */
    public double completionRate() {
        return created == 0 ? 0 : (double) completed / created;
    }
}
