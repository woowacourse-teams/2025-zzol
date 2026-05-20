package coffeeshout.report.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reporter {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_code", length = 5)
    private String userCode;

    public Reporter(Long userId, String userCode) {
        this.userId = userId;
        this.userCode = userCode;
    }
}
