package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 계층 의존 방향 강제: domain ← application ← infra (역방향 금지).
 * domain이 infra/application을 직접 import하거나
 * application이 ui를 import하는 것을 금지한다.
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class LayerArchitectureTest {

    // ─────────────────────────────────────────
    // room 도메인
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule room_domain은_infra를_직접_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.infra..")
            .as("room.domain은 room.infra를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule room_domain은_application을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.application..")
            .as("room.domain은 room.application을 참조할 수 없다");

    @ArchTest
    static final ArchRule room_domain은_ui를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.ui..")
            .as("room.domain은 room.ui를 참조할 수 없다");

    // ─────────────────────────────────────────
    // user 도메인
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule user_domain은_infra를_직접_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.user.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.user.infra..")
            .as("user.domain은 user.infra를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule user_domain은_application을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.user.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.user.application..")
            .as("user.domain은 user.application을 참조할 수 없다");

    @ArchTest
    static final ArchRule user_domain은_ui를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.user.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.user.ui..")
            .as("user.domain은 user.ui를 참조할 수 없다");

    // ─────────────────────────────────────────
    // room application.service (port 인터페이스는 엔티티 참조 허용)
    // ─────────────────────────────────────────

    /**
     * application/port/ 는 JPA 엔티티를 메서드 시그니처에 사용하는 것이 설계상 허용됨.
     * (포트가 인프라 어댑터에게 entity 타입을 강제하는 구조)
     * application/service/ 는 infra.persistence를 직접 참조하면 안 됨.
     *
     * [기술부채] 아래 서비스들은 R10 DIP 리팩토링이 완전히 적용되지 않아 예외 처리:
     *   RouletteService, RoomService, PlayerNameAuditService,
     *   PlayerNameFeedbackService, PlayerNameAuditBatchProcessor, PlayerNameRankingCleanupService
     * 해소 방향: 각 서비스가 사용하는 entity → port 인터페이스로 추상화
     */
    @ArchTest
    static final ArchRule room_service는_infra_persistence를_직접_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.application.service..")
            .and().haveSimpleNameNotContaining("RoulettePersistenceService")
            .and().haveSimpleNameNotContaining("RouletteService")
            .and().haveSimpleNameNotContaining("RoomService")
            .and().haveSimpleNameNotContaining("PlayerNameAuditService")
            .and().haveSimpleNameNotContaining("PlayerNameFeedbackService")
            .and().haveSimpleNameNotContaining("PlayerNameAuditBatchProcessor")
            .and().haveSimpleNameNotContaining("PlayerNameRankingCleanupService")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.infra.persistence..")
            .as("room.application.service(기술부채 목록 제외)는 room.infra.persistence를 직접 참조할 수 없다");

    // ─────────────────────────────────────────
    // user application
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule user_application은_infra_persistence를_직접_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.user.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.user.infra.persistence..")
            .as("user.application은 user.infra.persistence를 직접 참조할 수 없다 — " +
                    "UserCreationPort 등 port 인터페이스를 통해 접근해야 한다");

    // ─────────────────────────────────────────
    // friend 도메인 (user 모듈 내)
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule friend_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.friend.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.friend.infra..")
            .as("friend.domain은 friend.infra를 참조할 수 없다");

    @ArchTest
    static final ArchRule friend_application은_infra를_직접_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.friend.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.friend.infra..")
            .as("friend.application은 friend.infra를 직접 참조할 수 없다");

    // ─────────────────────────────────────────
    // profanity 도메인
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule profanity_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.profanity.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.profanity.infra..")
            .as("profanity.domain은 profanity.infra를 참조할 수 없다");

    @ArchTest
    static final ArchRule profanity_domain은_application을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.profanity.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.profanity.application..")
            .as("profanity.domain은 profanity.application을 참조할 수 없다");

    @ArchTest
    static final ArchRule profanity_application은_infra를_직접_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.profanity.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.profanity.infra..")
            .as("profanity.application은 profanity.infra를 직접 참조할 수 없다");
}
