package coffeeshout.report.ui;

import coffeeshout.global.filter.ClientIpExtractor;
import coffeeshout.report.application.ReportService;
import coffeeshout.report.ui.request.CreateReportRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController implements ReportApi {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Void> submit(@Valid @RequestBody CreateReportRequest request, HttpServletRequest httpRequest) {
        final String ip = ClientIpExtractor.extract(httpRequest);
        reportService.submit(ip, request.category(), request.gameType(), request.joinCode(), request.content());
        return ResponseEntity.status(201).build();
    }
}
