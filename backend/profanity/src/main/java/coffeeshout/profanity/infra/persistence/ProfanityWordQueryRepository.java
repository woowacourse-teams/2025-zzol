package coffeeshout.profanity.infra.persistence;

import static coffeeshout.profanity.infra.persistence.QProfanityWordEntity.profanityWordEntity;

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
                .where(profanityWordEntity.isActive.isTrue()
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

    private BooleanExpression searchContains(String search) {
        return (search == null || search.isBlank()) ? null : profanityWordEntity.word.containsIgnoreCase(search);
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
