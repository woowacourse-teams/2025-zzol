package coffeeshout.friend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.fixture.FriendshipFixture;
import coffeeshout.fixture.UserFixture;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.global.ServiceTest;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FriendSearchServiceTest extends ServiceTest {

    @Autowired
    private FriendSearchService friendSearchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = userRepository.save(UserFixture.회원_엠제이());
        other = userRepository.save(UserFixture.회원_루키());
    }

    @Nested
    class 유저코드로_검색 {

        @Test
        void 존재하는_유저코드로_검색하면_결과를_반환한다() {
            final List<UserSearchResult> results = friendSearchService.searchByUserCode(
                    me.getId(), other.getUserCode().value()
            );

            assertSoftly(softly -> {
                softly.assertThat(results).hasSize(1);
                softly.assertThat(results.get(0).user().getId()).isEqualTo(other.getId());
                softly.assertThat(results.get(0).relationStatus()).isEqualTo(RelationStatus.NONE);
            });
        }

        @Test
        void 자기_자신의_유저코드로_검색하면_빈_결과를_반환한다() {
            final List<UserSearchResult> results = friendSearchService.searchByUserCode(
                    me.getId(), me.getUserCode().value()
            );

            assertThat(results).isEmpty();
        }

        @Test
        void 존재하지_않는_유저코드로_검색하면_빈_결과를_반환한다() {
            final List<UserSearchResult> results = friendSearchService.searchByUserCode(
                    me.getId(), "ZZZZZ"
            );

            assertThat(results).isEmpty();
        }

        @Test
        void 이미_친구인_사용자는_FRIEND_상태로_반환된다() {
            friendshipRepository.save(FriendshipFixture.accepted(me.getId(), other.getId()));

            final List<UserSearchResult> results = friendSearchService.searchByUserCode(
                    me.getId(), other.getUserCode().value()
            );

            assertThat(results.get(0).relationStatus()).isEqualTo(RelationStatus.FRIEND);
        }

        @Test
        void 내가_요청한_PENDING은_PENDING_OUTGOING으로_반환된다() {
            friendshipRepository.save(FriendshipFixture.pending(me.getId(), other.getId()));

            final List<UserSearchResult> results = friendSearchService.searchByUserCode(
                    me.getId(), other.getUserCode().value()
            );

            assertThat(results.get(0).relationStatus()).isEqualTo(RelationStatus.PENDING_OUTGOING);
        }

        @Test
        void 상대가_요청한_PENDING은_PENDING_INCOMING으로_반환된다() {
            friendshipRepository.save(FriendshipFixture.pending(other.getId(), me.getId()));

            final List<UserSearchResult> results = friendSearchService.searchByUserCode(
                    me.getId(), other.getUserCode().value()
            );

            assertThat(results.get(0).relationStatus()).isEqualTo(RelationStatus.PENDING_INCOMING);
        }
    }

    @Nested
    class 닉네임으로_검색 {

        @Test
        void 존재하는_닉네임으로_검색하면_결과를_반환한다() {
            final List<UserSearchResult> results = friendSearchService.searchByNickname(
                    me.getId(), other.getNickname().value()
            );

            assertSoftly(softly -> {
                softly.assertThat(results).hasSize(1);
                softly.assertThat(results.get(0).user().getId()).isEqualTo(other.getId());
            });
        }

        @Test
        void 자기_자신의_닉네임으로_검색하면_본인은_제외된다() {
            final List<UserSearchResult> results = friendSearchService.searchByNickname(
                    me.getId(), me.getNickname().value()
            );

            assertThat(results).isEmpty();
        }

        @Test
        void 존재하지_않는_닉네임으로_검색하면_빈_결과를_반환한다() {
            final List<UserSearchResult> results = friendSearchService.searchByNickname(
                    me.getId(), "없는닉네임"
            );

            assertThat(results).isEmpty();
        }
    }
}
