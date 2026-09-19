package coffeeshout.arch;

import static com.tngtech.archunit.lang.conditions.ArchConditions.be;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.springframework.context.annotation.Profile;

/**
 * 로컬 전용 시더는 이름이 {@code Local} 로 시작한다.
 *
 * <p>이름이 규칙인 이유는 <b>jacoco 가 애너테이션을 못 읽기 때문</b>이다. 루트
 * {@code build.gradle.kts} 는 {@code Local} 로 시작하는 클래스를 커버리지에서 뺀다.
 * 운영에서 아예 로드되지 않는 시연 데이터 생성기를 테스트하라고 요구하는 수치는
 * 아무것도 지켜 주지 않으면서 모듈 전체를 끌어내리기 때문이다. 제외 대상을 고르는 유일한
 * 근거가 클래스 이름이라, 이름과 애너테이션이 어긋나는 순간 규칙이 조용히 무너진다.
 *
 * <p>그래서 양방향을 다 건다. 한쪽만 걸면 각각 다른 방식으로 샌다.
 *
 * <ul>
 *   <li>애너테이션은 있는데 이름이 다르면 → 커버리지에서 안 빠져 수치가 다시 내려간다.
 *       {@code ReportMockDataInitializer} 가 그랬다
 *   <li>이름은 {@code Local} 인데 애너테이션이 없으면 → 운영에서 도는 코드가 커버리지
 *       바깥으로 조용히 빠져나간다. 이쪽이 더 위험하다
 * </ul>
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class LocalSeederNamingArchitectureTest {

    private static final DescribedPredicate<JavaClass> 로컬_프로필 =
            new DescribedPredicate<>("@Profile(\"local\") 이 붙어 있다") {
                @Override
                public boolean test(JavaClass type) {
                    return type.tryGetAnnotationOfType(Profile.class)
                            .map(profile -> List.of(profile.value()).contains("local"))
                            .orElse(false);
                }
            };

    /**
     * 중첩 클래스와 람다는 제외한다. 바깥이 {@code Local} 이면 안쪽 이름도 {@code Local} 로
     * 시작하는데, 거기에 애너테이션을 요구하면 익명 클래스 하나 때문에 규칙이 깨진다.
     * jacoco 도 바깥 이름으로 함께 빼므로 최상위만 보면 된다.
     */
    private static final DescribedPredicate<JavaClass> 이름이_Local로_시작 =
            new DescribedPredicate<>("이름이 Local 로 시작하는 최상위 클래스다") {
                @Override
                public boolean test(JavaClass type) {
                    return type.isTopLevelClass() && type.getSimpleName().startsWith("Local");
                }
            };

    @ArchTest
    static final ArchRule 로컬_프로필_빈은_이름이_Local로_시작한다 = classes()
            .that(로컬_프로필)
            .should(be(이름이_Local로_시작))
            .as("@Profile(\"local\") 클래스는 이름이 Local 로 시작해야 커버리지 제외에 걸린다");

    @ArchTest
    static final ArchRule Local로_시작하는_클래스는_로컬_프로필이다 = classes()
            .that(이름이_Local로_시작)
            .should(be(로컬_프로필))
            .as("Local 로 시작하는 클래스는 @Profile(\"local\") 이어야 한다. 아니면 운영 코드가 커버리지에서 빠진다");
}
