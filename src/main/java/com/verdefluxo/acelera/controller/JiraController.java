package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.service.JiraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/jira")
@RequiredArgsConstructor
public class JiraController {

    private final JiraService jiraService;

    @GetMapping("/tickets")
    public ResponseEntity<Map<String, Object>> getWeeklyTickets() {
        return ResponseEntity.ok(jiraService.getWeeklyTickets());
    }

    @PostMapping("/tickets/{issueKey}/worklog")
    public ResponseEntity<Map<String, Object>> logWork(
            @PathVariable String issueKey,
            @RequestBody Map<String, String> body) {
        String timeSpent = body.get("timeSpent");
        String comment   = body.get("comment");
        if (timeSpent == null || timeSpent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "timeSpent é obrigatório (ex: 1h 30m)"));
        }
        return ResponseEntity.ok(jiraService.logWork(issueKey, timeSpent, comment));
    }
}
