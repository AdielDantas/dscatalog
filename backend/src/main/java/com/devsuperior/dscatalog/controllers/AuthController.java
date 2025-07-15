package com.devsuperior.dscatalog.controllers;

import com.devsuperior.dscatalog.dto.EmailDTO;
import com.devsuperior.dscatalog.dto.NewPasswordDTO;
import com.devsuperior.dscatalog.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/auth")
@Tag(name = "Auth", description = "Controller for Auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Criar token de recuperação de senha",
            description = "Gera um token de recuperação e envia um e-mail com instruções para redefinir a senha",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Token criado e e-mail enviado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida (campos ausentes ou inválidos)"),
                    @ApiResponse(responseCode = "404", description = "Email não localizado")
            }
    )
    @PostMapping(value = "/recover-token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createRecoverToken(@Valid @RequestBody EmailDTO body) {
        authService.createRecoverToken(body);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Salvar nova senha",
            description = "Define uma nova senha com base em um token de recuperação válido",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: senha ausente ou inválida)"),
                    @ApiResponse(responseCode = "404", description = "Token inválido ou expirado")
            }
    )
    @PutMapping(value = "/new-password", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveNewPassword(@Valid @RequestBody NewPasswordDTO body) {
        authService.saveNewPassword(body);
        return ResponseEntity.noContent().build();
    }

}
