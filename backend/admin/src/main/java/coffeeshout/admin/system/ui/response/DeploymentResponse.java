package coffeeshout.admin.system.ui.response;

import java.time.Instant;

/**
 * 지금 떠 있는 것이 무엇인지.
 *
 * <p>장애 때 가장 먼저 묻는 것이 "언제 나간 어느 커밋이냐"인데, 그동안은 배포 로그를
 * 뒤져야만 알 수 있었다. 여기 있으면 화면을 열어 둔 채로 답할 수 있다.
 *
 * @param commit  {@code build-info.properties} 의 커밋 해시. 빌드 환경에서 git 을 못 읽으면
 *                {@code unknown} 이다. 그 경우도 값을 비우지 않고 그대로 보낸다 -
 *                빈칸은 "조회 실패"처럼 보이지만 unknown 은 "빌드가 그렇게 됐다"는 사실이다.
 * @param profile 활성 프로필. 화면 상단 배지는 <b>빌드 시점</b>에 박힌 값을 쓰므로,
 *                서버가 실제로 무슨 프로필로 떴는지와 어긋날 수 있다. 그 어긋남을 잡는 값이다.
 */
public record DeploymentResponse(String version, String commit, Instant builtAt, String profile) {}
