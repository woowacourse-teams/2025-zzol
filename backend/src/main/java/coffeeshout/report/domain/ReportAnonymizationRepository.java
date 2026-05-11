package coffeeshout.report.domain;

public interface ReportAnonymizationRepository {

    void clearUserCodeByUserId(Long userId);
}
