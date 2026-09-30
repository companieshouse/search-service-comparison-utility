package uk.gov.companieshouse.search.comparison.controller;
import org.springframework.web.bind.annotation.*;

@RestController
public class RootController {
    @GetMapping("/")
    public String home() {
        return "Search Comparison Service is up";
    }
}
