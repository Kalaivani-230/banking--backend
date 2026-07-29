package com.example.MiniProject.controller;

import com.example.MiniProject.entity.FxRate;
import com.example.MiniProject.service.FxRateService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/fx-rates")
public class AdminFxRateController {

    private final FxRateService service;

    public AdminFxRateController(FxRateService service) { this.service = service; }

    // US058 — Add new FX rate
    @PostMapping
    public FxRate add(@RequestBody Map<String, Object> body) {
        String from        = (String) body.get("fromCurrency");
        String to          = (String) body.get("toCurrency");
        BigDecimal rate    = new BigDecimal(body.get("rate").toString());
        BigDecimal markup  = body.containsKey("markupPercent")
                ? new BigDecimal(body.get("markupPercent").toString()) : BigDecimal.ZERO;
        Instant effFrom    = Instant.parse((String) body.get("effectiveFrom"));
        Long adminId       = getCallerId();
        return service.add(from, to, rate, markup, effFrom, adminId);
    }

    // US058 — Update INACTIVE rate
    @PutMapping("/{id}")
    public FxRate update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal rate   = body.containsKey("rate")
                ? new BigDecimal(body.get("rate").toString()) : null;
        BigDecimal markup = body.containsKey("markupPercent")
                ? new BigDecimal(body.get("markupPercent").toString()) : null;
        Instant effFrom   = body.containsKey("effectiveFrom")
                ? Instant.parse((String) body.get("effectiveFrom")) : null;
        return service.update(id, rate, markup, effFrom);
    }

    // US059 — Activate a rate (auto-deactivates existing active for same pair)
    @PostMapping("/{id}/activate")
    public FxRate activate(@PathVariable Long id) {
        return service.activate(id);
    }

    // US059 — Deactivate an active rate
    @PostMapping("/{id}/deactivate")
    public FxRate deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }

    // US060 — History (all or filtered by pair)
    @GetMapping("/history")
    public List<FxRate> history(
            @RequestParam(required = false) String fromCurrency,
            @RequestParam(required = false) String toCurrency) {
        return service.history(fromCurrency, toCurrency);
    }

    // All rates (admin list view)
    @GetMapping
    public List<FxRate> all() {
        return service.history(null, null);
    }

    private Long getCallerId() {
        String subject = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return Long.parseLong(subject.split(":")[1]);
    }
}
