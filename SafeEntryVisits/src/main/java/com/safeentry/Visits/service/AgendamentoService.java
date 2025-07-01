package com.safeentry.Visits.service;

import com.safeentry.Visits.model.Agendamento;
import com.safeentry.Visits.model.AgendamentoStatus;
import com.safeentry.Visits.dto.AgendamentoRequest;
import com.safeentry.Visits.repository.AgendamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.kafka.core.KafkaTemplate;
// import com.fasterxml.jackson.databind.ObjectMapper; // REMOVA ESTA LINHA (e a injeção no construtor)

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate; // Alterado de String para Object
    // private final ObjectMapper objectMapper; // REMOVA ESTA LINHA

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              KafkaTemplate<String, Object> kafkaTemplate /* REMOVA `, ObjectMapper objectMapper` */) {
        this.agendamentoRepository = agendamentoRepository;
        this.kafkaTemplate = kafkaTemplate;
        // this.objectMapper = objectMapper; // REMOVA ESTA LINHA
    }

    // Método para gerar um QR Token único e seguro
    private String generateQrToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[16]; // 16 bytes = 128 bits, bom para um token único
        secureRandom.nextBytes(tokenBytes);
        // Codifica para Base64 URL-safe para evitar caracteres problemáticos em URLs ou QR Codes
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    @Transactional
    public Agendamento createAgendamento(AgendamentoRequest request, UUID moradorId) {
        Agendamento agendamento = new Agendamento();
        agendamento.setMoradorId(moradorId); // Define o ID do morador extraído do token
        agendamento.setDataHoraVisita(request.getDataHoraVisita());
        agendamento.setVisitanteJson(request.getVisitante());
        agendamento.setQrToken(generateQrToken()); // Gera um QR Token único
        agendamento.setStatus(AgendamentoStatus.pendente); // Define o status inicial

        Agendamento savedAgendamento = agendamentoRepository.save(agendamento);

        // Publicar evento para o serviço de Portaria via Kafka
        try {
            // ENVIE O OBJETO DIRETAMENTE. O JsonSerializer configurado no KafkaProducerConfig fará a conversão para JSON.
            kafkaTemplate.send("agendamentos-criados", savedAgendamento.getId().toString(), savedAgendamento);
            System.out.println("Evento de agendamento criado enviado para Kafka: " + savedAgendamento.getId());
        } catch (Exception e) {
            System.err.println("Erro ao enviar evento para Kafka: " + e.getMessage());
            // Lidar com o erro de forma apropriada (log, retry, etc.)
        }

        return savedAgendamento;
    }

    public Optional<Agendamento> getAgendamentoById(UUID id) {
        return agendamentoRepository.findById(id);
    }

    public List<Agendamento> getAgendamentosByMorador(UUID moradorId) {
        return agendamentoRepository.findByMoradorIdOrderByDataHoraVisitaDesc(moradorId);
    }

    public Optional<Agendamento> getAgendamentoByQrToken(String qrToken) {
        return agendamentoRepository.findByQrToken(qrToken);
    }

    @Transactional
    public Agendamento updateAgendamentoStatus(UUID agendamentoId, AgendamentoStatus newStatus) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado com o ID: " + agendamentoId));

        // Adicione lógica de validação aqui se necessário (ex: não pode mudar de USADO para PENDENTE)
        agendamento.setStatus(newStatus);
        // Se o status for USADO, marque como usado = true
        if (newStatus == AgendamentoStatus.usado) {
            agendamento.setUsado(true);
        }

        return agendamentoRepository.save(agendamento);
    }

    // Método para marcar um agendamento como usado (inválido após o uso)
    @Transactional
    public Agendamento markAgendamentoAsUsed(String qrToken) {
        Agendamento agendamento = agendamentoRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado para o token: " + qrToken));

        if (agendamento.getUsado() || agendamento.getStatus() != AgendamentoStatus.pendente) {
            throw new IllegalStateException("Agendamento já foi usado ou está em status inválido.");
        }

        agendamento.setUsado(true);
        agendamento.setStatus(AgendamentoStatus.usado);
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento cancelAgendamento(UUID agendamentoId, UUID moradorId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado com o ID: " + agendamentoId));

        // Verifica se o agendamento pertence ao morador autenticado
        if (!agendamento.getMoradorId().equals(moradorId)) {
            throw new IllegalStateException("Você não tem permissão para cancelar este agendamento.");
        }

        // Verifica se o agendamento pode ser cancelado
        if (agendamento.getStatus() != AgendamentoStatus.pendente) {
            throw new IllegalStateException("Não é possível cancelar um agendamento que não esteja pendente. Status atual: " + agendamento.getStatus());
        }

        agendamento.setStatus(AgendamentoStatus.cancelado);
        return agendamentoRepository.save(agendamento);
    }
}