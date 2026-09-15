package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.model.dto.UserSettingsDTO;
import com.verdefluxo.acelera.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserSettingsDTO> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<UserSettingsDTO> saveSettings(@RequestBody UserSettingsDTO dto) {
        return ResponseEntity.ok(settingsService.saveSettings(dto));
    }
}
