package com.example.MiniProject.config;

import com.example.MiniProject.entity.*;
import com.example.MiniProject.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final InternalUserRepository internalRepo;
    private final BCryptPasswordEncoder encoder;
    private final MasterCountryRepository countryRepo;
    private final MasterCurrencyRepository currencyRepo;
    private final MasterChannelRepository channelRepo;
    private final MasterCustomerTypeRepository customerTypeRepo;
    private final MasterCorridorRepository corridorRepo;
    private final CustomerRepository customerRepo;
    private final AccountRepository accountRepo;
    private final KycRequestRepository kycRepo;
    private final FeePolicyRepository feePolicyRepo;
    private final FeeComponentRepository feeComponentRepo;
    private final FxRateRepository fxRateRepo;
    private final ApprovalRepository approvalRepo;

    public DemoDataSeeder(InternalUserRepository internalRepo,
                          BCryptPasswordEncoder encoder,
                          MasterCountryRepository countryRepo,
                          MasterCurrencyRepository currencyRepo,
                          MasterChannelRepository channelRepo,
                          MasterCustomerTypeRepository customerTypeRepo,
                          MasterCorridorRepository corridorRepo,
                          CustomerRepository customerRepo,
                          AccountRepository accountRepo,
                          KycRequestRepository kycRepo,
                          FeePolicyRepository feePolicyRepo,
                          FeeComponentRepository feeComponentRepo,
                          FxRateRepository fxRateRepo,
                          ApprovalRepository approvalRepo) {
        this.internalRepo = internalRepo;
        this.encoder = encoder;
        this.countryRepo = countryRepo;
        this.currencyRepo = currencyRepo;
        this.channelRepo = channelRepo;
        this.customerTypeRepo = customerTypeRepo;
        this.corridorRepo = corridorRepo;
        this.customerRepo = customerRepo;
        this.accountRepo = accountRepo;
        this.kycRepo = kycRepo;
        this.feePolicyRepo = feePolicyRepo;
        this.feeComponentRepo = feeComponentRepo;
        this.fxRateRepo = fxRateRepo;
        this.approvalRepo = approvalRepo;
    }

    @Override
    public void run(String... args) {
        seedMasterData();
        seedInternalUsers();
        seedDemoCustomer();
        seedFeePoliciesAndFxRates();
        seedPendingApprovals();
        System.out.println("✅ All demo data seeded.");
    }

    /** Create Approval records for any PENDING_APPROVAL policy that has none. */
    private void seedPendingApprovals() {
        feePolicyRepo.findByStatusOrderByUpdatedAtDesc("PENDING_APPROVAL").forEach(p -> {
            boolean exists = approvalRepo
                    .findTopByEntityIdAndEntityTypeAndStatusOrderBySubmittedAtDesc(
                            p.getId(), "FEE_POLICY", "PENDING")
                    .isPresent();
            if (!exists) {
                Approval a = new Approval();
                a.setEntityType("FEE_POLICY");
                a.setEntityId(p.getId());
                a.setSubmittedBy(1L); // Admin EMP1001
                a.setSubmittedAt(Instant.now());
                a.setStatus("PENDING");
                approvalRepo.save(a);
            }
        });
    }

    private void seedMasterData() {
        if (countryRepo.count() == 0) {
            countryRepo.save(makeCountry("IN", "India"));
            countryRepo.save(makeCountry("US", "United States"));
            countryRepo.save(makeCountry("GB", "United Kingdom"));
            countryRepo.save(makeCountry("AE", "UAE"));
            countryRepo.save(makeCountry("EU", "Europe"));
            countryRepo.save(makeCountry("SG", "Singapore"));
        }
        if (currencyRepo.count() == 0) {
            currencyRepo.save(makeCurrency("INR", "Indian Rupee", "₹"));
            currencyRepo.save(makeCurrency("USD", "US Dollar", "$"));
            currencyRepo.save(makeCurrency("GBP", "British Pound", "£"));
            currencyRepo.save(makeCurrency("AED", "UAE Dirham", "د.إ"));
            currencyRepo.save(makeCurrency("EUR", "Euro", "€"));
            currencyRepo.save(makeCurrency("SGD", "Singapore Dollar", "S$"));
        }
        if (channelRepo.count() == 0) {
            channelRepo.save(makeChannel("ONLINE", "Digital banking"));
            channelRepo.save(makeChannel("BRANCH", "Branch assisted"));
            channelRepo.save(makeChannel("SWIFT", "SWIFT transfer"));
        }
        if (customerTypeRepo.count() == 0) {
            customerTypeRepo.save(makeCustomerType("RETAIL", "Individual"));
            customerTypeRepo.save(makeCustomerType("CORPORATE", "Business"));
        }
        if (corridorRepo.count() == 0) {
            corridorRepo.save(makeCorridor("IN", "US"));
            corridorRepo.save(makeCorridor("US", "IN"));
            corridorRepo.save(makeCorridor("IN", "GB"));
            corridorRepo.save(makeCorridor("IN", "AE"));
            corridorRepo.save(makeCorridor("IN", "EU"));
            corridorRepo.save(makeCorridor("IN", "SG"));
        }
    }

    private void seedInternalUsers() {
        seedInternalUser("EMP1001", "Admin One",       "admin1@bank.com",       "ADMIN");
        seedInternalUser("EMP1002", "Admin Two",       "admin2@bank.com",       "ADMIN");
        seedInternalUser("EMP2001", "Compliance One",  "compliance1@bank.com",  "COMPLIANCE_OPS");
        seedInternalUser("EMP2002", "Compliance Two",  "compliance2@bank.com",  "COMPLIANCE_OPS");
        seedInternalUser("EMP3001", "Teller One",      "teller1@bank.com",      "TELLER");
        seedInternalUser("EMP4001", "Ops One",         "ops1@bank.com",         "OPS");
        seedInternalUser("EMP4002", "Ops Two",         "ops2@bank.com",         "OPS");
    }

    private void seedDemoCustomer() {
        if (customerRepo.findByEmail("demo@cbft.com").isPresent()) return;

        // Create customer
        Customer c = new Customer();
        c.setCustomerCode("CUST-DEMO0001");
        c.setFullName("Demo Customer");
        c.setEmail("demo@cbft.com");
        c.setMobile("9999999999");
        c.setPasswordHash(encoder.encode("Demo@1234"));
        c.setStatus("ACTIVE");
        c.setCustomerTypeCode("RETAIL");
        c.setPreferredAccountType("SAVINGS");
        c.setFailedLoginAttempts(0);
        Customer saved = customerRepo.save(c);

        // Create KYC
        KycRequest kyc = new KycRequest();
        kyc.setCustomerId(saved.getId());
        kyc.setPan("ABCDE1234F");
        kyc.setAadhaar("123456789012");
        kyc.setStatus("VERIFIED");
        kyc.setSubmittedAt(Instant.now());
        kyc.setVerifiedAt(Instant.now());
        kycRepo.save(kyc);

        // Create account
        Account a = new Account();
        a.setCustomerId(saved.getId());
        a.setAccountNo("ACCT" + (100000 + saved.getId()));
        a.setAccountType("SAVINGS");
        a.setCurrencyCode("INR");
        a.setCountryCode("IN");
        a.setStatus("ACTIVE");
        a.setLedgerBalance(new BigDecimal("50000.00"));
        a.setAvailableBalance(new BigDecimal("50000.00"));
        accountRepo.save(a);

        System.out.println("✅ Demo customer seeded: demo@cbft.com / Demo@1234");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void seedInternalUser(String employeeId, String fullName, String email, String role) {
        if (internalRepo.existsByEmployeeId(employeeId)) return;
        InternalUser u = new InternalUser();
        u.setEmployeeId(employeeId);
        u.setFullName(fullName);
        u.setEmail(email);
        u.setRole(role);
        u.setStatus("ACTIVE");
        u.setFailedLoginAttempts(0);
        u.setPasswordHash(encoder.encode("Password@123"));
        internalRepo.save(u);
    }

    private MasterCountry makeCountry(String code, String name) {
        MasterCountry c = new MasterCountry(); c.setCode(code); c.setName(name); c.setIsActive(true); return c;
    }
    private MasterCurrency makeCurrency(String code, String name, String symbol) {
        MasterCurrency c = new MasterCurrency(); c.setCode(code); c.setName(name); c.setSymbol(symbol); c.setIsActive(true); return c;
    }
    private MasterChannel makeChannel(String code, String desc) {
        MasterChannel c = new MasterChannel(); c.setCode(code); c.setDescription(desc); c.setIsActive(true); return c;
    }
    private MasterCustomerType makeCustomerType(String code, String desc) {
        MasterCustomerType c = new MasterCustomerType(); c.setCode(code); c.setDescription(desc); c.setIsActive(true); return c;
    }
    private MasterCorridor makeCorridor(String from, String to) {
        MasterCorridor c = new MasterCorridor(); c.setFromCountryCode(from); c.setToCountryCode(to); c.setIsActive(true); c.setCreatedAt(Instant.now()); return c;
    }

    private void seedFeePoliciesAndFxRates() {
        if (fxRateRepo.count() == 0) {
            // Real-world approximate mid-market rates with typical bank markup
            fxRateRepo.save(makeFxRate("USD", "INR", "83.50",  "0.50")); // 1 USD = 83.50 INR, 0.5% markup
            fxRateRepo.save(makeFxRate("GBP", "INR", "106.20", "0.60")); // 1 GBP = 106.20 INR
            fxRateRepo.save(makeFxRate("EUR", "INR", "90.10",  "0.55")); // 1 EUR = 90.10 INR
            fxRateRepo.save(makeFxRate("SGD", "INR", "62.30",  "0.45")); // 1 SGD = 62.30 INR
            fxRateRepo.save(makeFxRate("AED", "INR", "22.70",  "0.40")); // 1 AED = 22.70 INR
            System.out.println("✅ FX rates seeded.");
        }

        if (feePolicyRepo.count() > 0) return;

        // Find the IN→US corridor
        MasterCorridor inUs = corridorRepo.findAll().stream()
                .filter(c -> "IN".equals(c.getFromCountryCode()) && "US".equals(c.getToCountryCode()))
                .findFirst().orElse(null);
        MasterCorridor inGb = corridorRepo.findAll().stream()
                .filter(c -> "IN".equals(c.getFromCountryCode()) && "GB".equals(c.getToCountryCode()))
                .findFirst().orElse(null);
        MasterCorridor inAe = corridorRepo.findAll().stream()
                .filter(c -> "IN".equals(c.getFromCountryCode()) && "AE".equals(c.getToCountryCode()))
                .findFirst().orElse(null);
        MasterCorridor inEu = corridorRepo.findAll().stream()
                .filter(c -> "IN".equals(c.getFromCountryCode()) && "EU".equals(c.getToCountryCode()))
                .findFirst().orElse(null);
        MasterCorridor inSg = corridorRepo.findAll().stream()
                .filter(c -> "IN".equals(c.getFromCountryCode()) && "SG".equals(c.getToCountryCode()))
                .findFirst().orElse(null);

        // ── INR → USD  (3 amount slabs × 2 channels × 2 customer types) ──────────
        if (inUs != null) {
            Long cid = inUs.getId();

            // --- ONLINE / RETAIL ---
            // Slab 1: ₹0 – ₹25,000  (small remittance — higher flat fee)
            FeePolicy p1 = savePolicy("INR→USD Online Retail Small (≤25K)", cid, "INR", "USD",
                    "ONLINE", "RETAIL", "0", "25000", 1, "ACTIVE");
            addComponents(p1.getId(), "299.00", "FIXED", "200.00", "FIXED", "99.00", "FIXED", "18.00");

            // Slab 2: ₹25,001 – ₹5,00,000  (standard remittance — % based)
            FeePolicy p2 = savePolicy("INR→USD Online Retail Standard (25K–5L)", cid, "INR", "USD",
                    "ONLINE", "RETAIL", "25001", "500000", 2, "ACTIVE");
            addComponentsPercent(p2.getId(), "0.50", "200.00", "500.00",   // base: 0.5%, min ₹200, max ₹500
                                              "0.20", "100.00", "300.00",   // intermediary: 0.2%
                                              "0.10", "50.00",  "150.00",   // handling: 0.1%
                                              "18.00");                      // GST 18%

            // Slab 3: ₹5,00,001+  (large / high-value — lower % with caps)
            FeePolicy p3 = savePolicy("INR→USD Online Retail High-Value (>5L)", cid, "INR", "USD",
                    "ONLINE", "RETAIL", "500001", null, 3, "ACTIVE");
            addComponentsPercent(p3.getId(), "0.30", "500.00", "2000.00",
                                              "0.15", "200.00", "800.00",
                                              "0.05", "100.00", "400.00",
                                              "18.00");

            // --- ONLINE / CORPORATE (lower fees than retail) ---
            FeePolicy p4 = savePolicy("INR→USD Online Corporate Standard", cid, "INR", "USD",
                    "ONLINE", "CORPORATE", "0", null, 1, "ACTIVE");
            addComponentsPercent(p4.getId(), "0.25", "150.00", "1500.00",
                                              "0.15", "100.00", "600.00",
                                              "0.05", "50.00",  "200.00",
                                              "18.00");

            // --- BRANCH / RETAIL (branch is more expensive than online) ---
            FeePolicy p5 = savePolicy("INR→USD Branch Retail Standard", cid, "INR", "USD",
                    "BRANCH", "RETAIL", "0", null, 1, "ACTIVE");
            addComponents(p5.getId(), "499.00", "FIXED", "250.00", "FIXED", "149.00", "FIXED", "18.00");

            // --- PENDING_APPROVAL: revised online retail slab 1 (admin submitted, awaiting compliance) ---
            FeePolicy p6 = savePolicy("INR→USD Online Retail Small v2 (Revised)", cid, "INR", "USD",
                    "ONLINE", "RETAIL", "0", "25000", 1, "PENDING_APPROVAL");
            addComponents(p6.getId(), "249.00", "FIXED", "180.00", "FIXED", "79.00", "FIXED", "18.00");

            // --- DRAFT: proposed zero-fee promo for new customers ---
            FeePolicy p7 = savePolicy("INR→USD Online Retail Promo (Zero Base Fee)", cid, "INR", "USD",
                    "ONLINE", "RETAIL", "0", "10000", 0, "DRAFT");
            addComponents(p7.getId(), "0.00", "FIXED", "100.00", "FIXED", "0.00", "FIXED", "18.00");

            // --- REJECTED: old high-fee policy rejected by compliance ---
            FeePolicy p8 = savePolicy("INR→USD Online Retail Legacy (Rejected)", cid, "INR", "USD",
                    "ONLINE", "RETAIL", "0", null, 5, "REJECTED");
            addComponents(p8.getId(), "599.00", "FIXED", "300.00", "FIXED", "199.00", "FIXED", "18.00");
        }

        // ── INR → GBP ────────────────────────────────────────────────────────────
        if (inGb != null) {
            Long cid = inGb.getId();

            FeePolicy g1 = savePolicy("INR→GBP Online Retail Standard", cid, "INR", "GBP",
                    "ONLINE", "RETAIL", "0", "500000", 1, "ACTIVE");
            addComponentsPercent(g1.getId(), "0.55", "250.00", "600.00",
                                              "0.25", "150.00", "400.00",
                                              "0.10", "75.00",  "200.00",
                                              "18.00");

            FeePolicy g2 = savePolicy("INR→GBP Online Retail High-Value (>5L)", cid, "INR", "GBP",
                    "ONLINE", "RETAIL", "500001", null, 2, "ACTIVE");
            addComponentsPercent(g2.getId(), "0.35", "500.00", "2500.00",
                                              "0.18", "200.00", "900.00",
                                              "0.07", "100.00", "500.00",
                                              "18.00");

            FeePolicy g3 = savePolicy("INR→GBP Online Corporate", cid, "INR", "GBP",
                    "ONLINE", "CORPORATE", "0", null, 1, "ACTIVE");
            addComponentsPercent(g3.getId(), "0.28", "200.00", "1800.00",
                                              "0.15", "100.00", "700.00",
                                              "0.05", "50.00",  "250.00",
                                              "18.00");

            // PENDING_APPROVAL: branch policy awaiting sign-off
            FeePolicy g4 = savePolicy("INR→GBP Branch Retail", cid, "INR", "GBP",
                    "BRANCH", "RETAIL", "0", null, 1, "PENDING_APPROVAL");
            addComponents(g4.getId(), "549.00", "FIXED", "275.00", "FIXED", "175.00", "FIXED", "18.00");
        }

        // ── INR → AED ────────────────────────────────────────────────────────────
        if (inAe != null) {
            Long cid = inAe.getId();

            FeePolicy a1 = savePolicy("INR→AED Online Retail Standard", cid, "INR", "AED",
                    "ONLINE", "RETAIL", "0", "500000", 1, "ACTIVE");
            addComponentsPercent(a1.getId(), "0.45", "199.00", "500.00",
                                              "0.20", "100.00", "300.00",
                                              "0.08", "50.00",  "150.00",
                                              "18.00");

            FeePolicy a2 = savePolicy("INR→AED Online Corporate", cid, "INR", "AED",
                    "ONLINE", "CORPORATE", "0", null, 1, "ACTIVE");
            addComponentsPercent(a2.getId(), "0.22", "150.00", "1200.00",
                                              "0.12", "80.00",  "500.00",
                                              "0.04", "40.00",  "180.00",
                                              "18.00");

            // DRAFT: proposed weekend surcharge policy
            FeePolicy a3 = savePolicy("INR→AED Online Retail Weekend Surcharge (Draft)", cid, "INR", "AED",
                    "ONLINE", "RETAIL", "0", null, 0, "DRAFT");
            addComponents(a3.getId(), "399.00", "FIXED", "200.00", "FIXED", "149.00", "FIXED", "18.00");
        }

        // ── INR → EUR ────────────────────────────────────────────────────────────
        if (inEu != null) {
            Long cid = inEu.getId();

            FeePolicy e1 = savePolicy("INR→EUR Online Retail Standard", cid, "INR", "EUR",
                    "ONLINE", "RETAIL", "0", "500000", 1, "ACTIVE");
            addComponentsPercent(e1.getId(), "0.50", "220.00", "550.00",
                                              "0.22", "120.00", "350.00",
                                              "0.09", "60.00",  "180.00",
                                              "18.00");

            FeePolicy e2 = savePolicy("INR→EUR Online Retail High-Value (>5L)", cid, "INR", "EUR",
                    "ONLINE", "RETAIL", "500001", null, 2, "ACTIVE");
            addComponentsPercent(e2.getId(), "0.32", "500.00", "2200.00",
                                              "0.16", "200.00", "850.00",
                                              "0.06", "100.00", "450.00",
                                              "18.00");

            FeePolicy e3 = savePolicy("INR→EUR Online Corporate", cid, "INR", "EUR",
                    "ONLINE", "CORPORATE", "0", null, 1, "ACTIVE");
            addComponentsPercent(e3.getId(), "0.26", "180.00", "1600.00",
                                              "0.14", "90.00",  "650.00",
                                              "0.05", "45.00",  "220.00",
                                              "18.00");

            // PENDING_APPROVAL: new student remittance slab
            FeePolicy e4 = savePolicy("INR→EUR Online Retail Student (<10K)", cid, "INR", "EUR",
                    "ONLINE", "RETAIL", "0", "10000", 0, "PENDING_APPROVAL");
            addComponents(e4.getId(), "149.00", "FIXED", "100.00", "FIXED", "49.00", "FIXED", "18.00");
        }

        // ── INR → SGD ────────────────────────────────────────────────────────────
        if (inSg != null) {
            Long cid = inSg.getId();

            FeePolicy s1 = savePolicy("INR→SGD Online Retail Standard", cid, "INR", "SGD",
                    "ONLINE", "RETAIL", "0", "500000", 1, "ACTIVE");
            addComponentsPercent(s1.getId(), "0.42", "180.00", "480.00",
                                              "0.18", "90.00",  "280.00",
                                              "0.08", "45.00",  "140.00",
                                              "18.00");

            FeePolicy s2 = savePolicy("INR→SGD Online Corporate", cid, "INR", "SGD",
                    "ONLINE", "CORPORATE", "0", null, 1, "ACTIVE");
            addComponentsPercent(s2.getId(), "0.20", "140.00", "1100.00",
                                              "0.10", "70.00",  "450.00",
                                              "0.04", "35.00",  "160.00",
                                              "18.00");

            // DRAFT: branch policy not yet submitted
            FeePolicy s3 = savePolicy("INR→SGD Branch Retail (Draft)", cid, "INR", "SGD",
                    "BRANCH", "RETAIL", "0", null, 1, "DRAFT");
            addComponents(s3.getId(), "449.00", "FIXED", "220.00", "FIXED", "129.00", "FIXED", "18.00");
        }

        System.out.println("✅ Realistic fee policies seeded (" + feePolicyRepo.count() + " policies).");
    }

    /** Save a FeePolicy with all scalar fields. */
    private FeePolicy savePolicy(String name, Long corridorId, String sendCcy, String recvCcy,
                                  String channel, String custType,
                                  String amtMin, String amtMax,
                                  int priority, String status) {
        FeePolicy p = new FeePolicy();
        p.setPolicyName(name);
        p.setCorridorId(corridorId);
        p.setSendCurrency(sendCcy);
        p.setReceiveCurrency(recvCcy);
        p.setChannelCode(channel);
        p.setCustomerTypeCode(custType);
        p.setAmountMin(new BigDecimal(amtMin));
        p.setAmountMax(amtMax != null ? new BigDecimal(amtMax) : null);
        p.setPriority(priority);
        p.setEffectiveFrom(Instant.parse("2024-01-01T00:00:00Z"));
        p.setStatus(status);
        p.setVersionNumber(1);
        p.setCreatedBy(1L);
        return feePolicyRepo.save(p);
    }

    /** All FIXED fee components. */
    private void addComponents(Long policyId,
                                String baseFee, String baseCalc,
                                String intFee,  String intCalc,
                                String hndFee,  String hndCalc,
                                String taxPct) {
        feeComponentRepo.save(makeComponent(policyId, "BASE_FEE",         baseCalc, baseFee, null, null, false));
        feeComponentRepo.save(makeComponent(policyId, "INTERMEDIARY_FEE", intCalc,  intFee,  null, null, true));
        feeComponentRepo.save(makeComponent(policyId, "HANDLING_FEE",     hndCalc,  hndFee,  null, null, false));
        feeComponentRepo.save(makeComponent(policyId, "TAX",              "PERCENT", taxPct, null, null, false));
    }

    /** PERCENT fee components with min/max caps. */
    private void addComponentsPercent(Long policyId,
                                       String basePct,  String baseMin,  String baseMax,
                                       String intPct,   String intMin,   String intMax,
                                       String hndPct,   String hndMin,   String hndMax,
                                       String taxPct) {
        feeComponentRepo.save(makeComponent(policyId, "BASE_FEE",         "PERCENT", basePct, baseMin, baseMax, false));
        feeComponentRepo.save(makeComponent(policyId, "INTERMEDIARY_FEE", "PERCENT", intPct,  intMin,  intMax,  true));
        feeComponentRepo.save(makeComponent(policyId, "HANDLING_FEE",     "PERCENT", hndPct,  hndMin,  hndMax,  false));
        feeComponentRepo.save(makeComponent(policyId, "TAX",              "PERCENT", taxPct,  null,    null,    false));
    }

    private String currencyForCountry(String countryCode) {
        return switch (countryCode) {
            case "IN" -> "INR";
            case "US" -> "USD";
            case "GB" -> "GBP";
            case "AE" -> "AED";
            default   -> null;
        };
    }

    private FxRate makeFxRate(String from, String to, String rate, String markup) {
        FxRate fx = new FxRate();
        fx.setFromCurrency(from);
        fx.setToCurrency(to);
        fx.setRate(new BigDecimal(rate));
        fx.setMarkupPercent(new BigDecimal(markup));
        fx.setEffectiveFrom(Instant.parse("2024-01-01T00:00:00Z"));
        fx.setStatus("ACTIVE");
        fx.setCreatedBy(1L);
        return fx;
    }

    private FeeComponent makeComponent(Long policyId, String type, String calcType,
                                        String value, String minFee, String maxFee,
                                        boolean estimated) {
        FeeComponent c = new FeeComponent();
        c.setPolicyId(policyId);
        c.setComponentType(type);
        c.setCalcType(calcType);
        c.setValue(new BigDecimal(value));
        if (minFee != null) c.setMinFee(new BigDecimal(minFee));
        if (maxFee != null) c.setMaxFee(new BigDecimal(maxFee));
        c.setIsEstimated(estimated);
        return c;
    }
}
