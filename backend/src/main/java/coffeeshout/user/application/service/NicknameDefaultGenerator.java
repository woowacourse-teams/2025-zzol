package coffeeshout.user.application.service;

import coffeeshout.user.domain.UserNickname;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class NicknameDefaultGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "용감한", "빠른", "귀여운", "강한", "활발한", "조용한", "씩씩한", "영리한",
            "차분한", "유쾌한", "현명한", "신나는", "대담한", "엉뚱한", "다정한", "부지런한"
    );

    private static final List<String> NOUNS = List.of(
            "호랑이", "여우", "고양이", "강아지", "토끼", "사자", "펭귄", "독수리",
            "늑대", "판다", "하마", "코끼리", "기린", "치타", "수달", "오리"
    );

    public String generate() {
        final String adjective = pick(ADJECTIVES);
        final String noun = pick(NOUNS);
        final String candidate = adjective + noun;
        if (candidate.length() > UserNickname.MAX_LENGTH) {
            return noun;
        }
        return candidate;
    }

    private String pick(List<String> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
