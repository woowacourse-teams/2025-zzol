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
    // friend 도메인 (user 모듈 내)
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule friend_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.friend.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.friend.infra..")
            .as("friend.domain은 friend.infra를 참조할 수 없다");
}
