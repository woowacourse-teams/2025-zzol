package coffeeshout.profanity.domain;

public interface ProfanityWordCommandPort {

    void add(String word, Language language, WordSource source);

    void deactivate(String word);
}
