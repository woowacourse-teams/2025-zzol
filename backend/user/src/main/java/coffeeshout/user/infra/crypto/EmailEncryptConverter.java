package coffeeshout.user.infra.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@code email} 컬럼을 읽고 쓸 때 자동으로 복호화/암호화하는 JPA 컨버터.
 *
 * <p>{@code autoApply = false}(기본값)로 두고 엔티티의 email 필드에만 {@code @Convert}로 명시 적용한다.
 * 모든 String 컬럼에 적용되지 않도록 하기 위함이다. Spring Boot는 Hibernate가 Spring 빈 컨테이너로
 * 컨버터를 생성하도록 구성하므로 {@link EmailEncryptor} 생성자 주입이 동작한다.
 */
@Converter
public class EmailEncryptConverter implements AttributeConverter<String, String> {

    private final EmailEncryptor emailEncryptor;

    public EmailEncryptConverter(EmailEncryptor emailEncryptor) {
        this.emailEncryptor = emailEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return emailEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return emailEncryptor.decrypt(dbData);
    }
}
