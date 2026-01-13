package com.app.backend.controllers;


import com.app.backend.components.LocalizationUtils;
import com.app.backend.dtos.ChuDeDTO;
import com.app.backend.models.ChuDe;
import com.app.backend.responses.ResponseObject;
import com.app.backend.services.chude.ChuDeService;
import com.app.backend.services.chude.IChuDeRedisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/chuDe")
public class ChuDeController {
    private final ChuDeService chuDeService;
    private final IChuDeRedisService chuDeRedisService;
    private final LocalizationUtils localizationUtils;

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getAllChuDe() {
        // 1. Check Redis cache first
        List<ChuDe> chuDes = chuDeRedisService.getAllChuDe();
        
        if (chuDes == null) {
            // 2. Cache miss -> Query DB
            chuDes = chuDeService.getAllChuDe();
            // 3. Save to cache
            chuDeRedisService.saveAllChuDe(chuDes);
        }

        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Lay danh sach chu de thanh cong")
                .status(HttpStatus.OK)
                .data(chuDes)
                .build());
    }

    @PostMapping("")
    public ResponseEntity<ResponseObject> createChuDe(@Valid @RequestBody ChuDeDTO chuDeDTO, BindingResult result) {
        if (result.hasErrors()) {
            List<String> errorMessages = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();
            return ResponseEntity.ok().body(ResponseObject.builder()
                    .message(errorMessages.toString())
                    .status(HttpStatus.BAD_REQUEST)
                    .data(null)
                    .build());

        }
        ChuDe createdChuDe = chuDeService.createChuDe(chuDeDTO);
        // Clear cache khi có thay đổi
        chuDeRedisService.clearChuDeCache();
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Tao chu de thanh cong")
                .status(HttpStatus.OK)
                .data(createdChuDe)
                .build());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getChuDeById(@PathVariable("id") Long id) {
        ChuDe chuDe = chuDeService.getChuDeById(id);
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Lay chu de thanh cong")
                .status(HttpStatus.OK)
                .data(chuDe)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseObject> updateChuDe(@PathVariable("id") Long id,
                                                      @Valid @RequestBody ChuDeDTO chuDeDTO,
                                                      BindingResult result) {
        if (result.hasErrors()) {
            List<String> errorMessages = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();
            return ResponseEntity.ok().body(ResponseObject.builder()
                    .message(errorMessages.toString())
                    .status(HttpStatus.BAD_REQUEST)
                    .data(null)
                    .build());

        }
        ChuDe updatedChuDe = chuDeService.updateChuDe(id, chuDeDTO);
        // Clear cache khi có thay đổi
        chuDeRedisService.clearChuDeCache();
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Cap nhat chu de thanh cong")
                .status(HttpStatus.OK)
                .data(updatedChuDe)
                .build());
    }
}
