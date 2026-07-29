package com.example.MiniProject.controller;

import com.example.MiniProject.entity.*;
import com.example.MiniProject.repository.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/masters")
public class AdminMastersController {

    private final MasterCountryRepository countryRepo;
    private final MasterCurrencyRepository currencyRepo;
    private final MasterChannelRepository channelRepo;
    private final MasterCustomerTypeRepository customerTypeRepo;
    private final MasterCorridorRepository corridorRepo;

    public AdminMastersController(MasterCountryRepository countryRepo,
                                  MasterCurrencyRepository currencyRepo,
                                  MasterChannelRepository channelRepo,
                                  MasterCustomerTypeRepository customerTypeRepo,
                                  MasterCorridorRepository corridorRepo) {
        this.countryRepo = countryRepo;
        this.currencyRepo = currencyRepo;
        this.channelRepo = channelRepo;
        this.customerTypeRepo = customerTypeRepo;
        this.corridorRepo = corridorRepo;
    }

    // Countries
    @GetMapping("/countries")
    public List<MasterCountry> getCountries() {
        return countryRepo.findAll();
    }

    @PostMapping("/countries")
    public MasterCountry addCountry(@RequestBody MasterCountry c) {
        return countryRepo.save(c);
    }

    // Currencies
    @GetMapping("/currencies")
    public List<MasterCurrency> getCurrencies() {
        return currencyRepo.findAll();
    }

    @PostMapping("/currencies")
    public MasterCurrency addCurrency(@RequestBody MasterCurrency c) {
        return currencyRepo.save(c);
    }

    // Channels
    @GetMapping("/channels")
    public List<MasterChannel> getChannels() {
        return channelRepo.findAll();
    }

    @PostMapping("/channels")
    public MasterChannel addChannel(@RequestBody MasterChannel c) {
        return channelRepo.save(c);
    }

    // Customer Types
    @GetMapping("/customer-types")
    public List<MasterCustomerType> getCustomerTypes() {
        return customerTypeRepo.findAll();
    }

    @PostMapping("/customer-types")
    public MasterCustomerType addCustomerType(@RequestBody MasterCustomerType c) {
        return customerTypeRepo.save(c);
    }

    // Corridors
    @GetMapping("/corridors")
    public List<MasterCorridor> getCorridors() {
        return corridorRepo.findAll();
    }

    @PostMapping("/corridors")
    public MasterCorridor addCorridor(@RequestBody MasterCorridor c) {
        return corridorRepo.save(c);
    }
}