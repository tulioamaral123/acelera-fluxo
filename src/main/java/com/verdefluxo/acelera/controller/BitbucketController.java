package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.service.BitbucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bitbucket")
@RequiredArgsConstructor
public class BitbucketController {

    private final BitbucketService bitbucketService;

    @GetMapping("/repos")
    public ResponseEntity<Map<String, Object>> getRepositories() {
        return ResponseEntity.ok(bitbucketService.getRepositories());
    }

    @GetMapping("/repos/{repoSlug}/prs")
    public ResponseEntity<Map<String, Object>> getPullRequests(@PathVariable String repoSlug) {
        return ResponseEntity.ok(bitbucketService.getPullRequests(repoSlug));
    }
}
