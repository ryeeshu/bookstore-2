package edu.cmu.bookstore.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping(value = "/status", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("OK");
    }
}