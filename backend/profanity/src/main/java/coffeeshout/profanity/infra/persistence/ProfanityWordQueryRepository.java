package coffeeshout.profanity.infra.persistence;

import static coffeeshout.profanity.infra.persistence.QProfanityWordEntity.profanityWordEntity;

import coffeeshout.global.persistence.LikePattern;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.WordSource;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfanityWordQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ProfanityWordEntity> findAllActive() {
        return queryFactory
                .selectFrom(profanityWordEntity)
                .where(profanityWordEntity
                        .isActive
                        .isTrue()
                        .and(profanityWordEntity.source.ne(WordSource.OPERATOR_ALLOWED)))
                .fetch();
    }

    public Page<ProfanityWordEntity> findAllPaged(
            String search, Language language, WordSource source, Boolean activeOnly, Pageable pageable) {

        final List<ProfanityWordEntity> content = queryFactory
                .selectFrom(profanityWordEntity)
                .where(searchContains(search), languageEq(language), sourceEq(source), activeEq(activeOnly))
                .orderBy(profanityWordEntity.word.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Long total = queryFactory
                .select(profanityWordEntity.count())
                .from(profanityWordEntity)
                .where(searchContains(search), languageEq(language), sourceEq(source), activeEq(activeOnly))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /**
     * {@code containsIgnoreCase} 를 쓰지 않는다. QueryDSL 이 이스케이프 없는 LIKE 로 풀어
     * 주어서, 검색어의 {@code %} 와 {@code _} 가 와일드카드로 읽힌다. 금칙어 사전에는
     * 그 두 글자가 낱말 자체로 들어 있을 수 있다.
     */
    private BooleanExpression searchContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return profanityWordEntity.word.likeIgnoreCase(LikePattern.contains(search), LikePattern.ESCAPE);
    }

    private BooleanExpression languageEq(Language language) {
        return language != null ? profanityWordEntity.language.eq(language) : null;
    }

    private BooleanExpression sourceEq(WordSource source) {
        return source != null ? profanityWordEntity.source.eq(source) : null;
    }

    private BooleanExpression activeEq(Boolean activeOnly) {
        return activeOnly != null ? profanityWordEntity.isActive.eq(activeOnly) : null;
    }
}
