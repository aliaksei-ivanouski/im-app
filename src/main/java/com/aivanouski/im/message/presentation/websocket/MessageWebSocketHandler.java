package com.aivanouski.im.message.presentation.websocket;

import com.aivanouski.im.shared.exception.ValidationException;
import com.aivanouski.im.message.application.MessageService;
import com.aivanouski.im.message.domain.Message;
import com.aivanouski.im.shared.error.WsErrorMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Component
public class MessageWebSocketHandler implements WebSocketHandler {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(MessageWebSocketHandler.class);

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final WsErrorMapper errorMapper;

    public MessageWebSocketHandler(MessageService messageService, ObjectMapper objectMapper, WsErrorMapper errorMapper) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.errorMapper = errorMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.send(
                session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .flatMap(this::dispatch)
                        .map(session::textMessage)
        );
    }

    private Mono<String> dispatch(String payloadText) {
        WsRequest request;
        try {
            request = objectMapper.readValue(payloadText, WsRequest.class);
        } catch (JsonProcessingException ex) {
            return Mono.just(writeSafe(WsResponse.error(null, "invalid_json", "Invalid JSON payload.")));
        }

        if (request.type() == null || request.type().isBlank()) {
            return Mono.just(writeSafe(WsResponse.error(request.requestId(), "invalid_request", "type is required.")));
        }

        return switch (request.type()) {
            case "create_message" -> handleCreate(request);
            case "edit_message" -> handleEdit(request);
            case "list_messages" -> handleList(request);
            default -> Mono.just(writeSafe(WsResponse.error(request.requestId(), "unknown_type", "Unsupported type.")));
        };
    }

    private Mono<String> handleCreate(WsRequest request) {
        return Mono.fromCallable(() -> {
                    CreateMessageRequest body = readPayload(request, CreateMessageRequest.class);
                    Message message = messageService.createMessage(body.userId(), body.chatId(), body.payload());
                    return WsResponse.ok("create_message_result", request.requestId(), MessageDto.from(message));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> Mono.just(errorMapper.toResponse(request.requestId(), ex)))
                .map(this::writeSafe);
    }

    private Mono<String> handleEdit(WsRequest request) {
        return Mono.fromCallable(() -> {
                    EditMessageRequest body = readPayload(request, EditMessageRequest.class);
                    Integer version = body.version();
                    if (version == null) {
                        throw new ValidationException("version is required.");
                    }
                    Message message = messageService.editMessage(
                            body.messageId(),
                            body.userId(),
                            body.chatId(),
                            version,
                            body.payload()
                    );
                    return WsResponse.ok("edit_message_result", request.requestId(), MessageDto.from(message));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> Mono.just(errorMapper.toResponse(request.requestId(), ex)))
                .map(this::writeSafe);
    }

    private Mono<String> handleList(WsRequest request) {
        return Mono.fromCallable(() -> {
                    ListMessagesRequest body = readPayload(request, ListMessagesRequest.class);
                    int page = body.page() == null ? DEFAULT_PAGE : body.page();
                    int size = body.size() == null ? DEFAULT_SIZE : body.size();
                    Page<Message> messages = messageService.listMessages(body.chatId(), body.userId(), page, size);
                    List<MessageDto> items = messages.getContent().stream()
                            .map(MessageDto::from)
                            .toList();
                    ListMessagesResponse response = new ListMessagesResponse(
                            items,
                            messages.getNumber(),
                            messages.getSize(),
                            messages.getTotalElements(),
                            messages.getTotalPages()
                    );
                    return WsResponse.ok("list_messages_result", request.requestId(), response);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> Mono.just(errorMapper.toResponse(request.requestId(), ex)))
                .map(this::writeSafe);
    }

    private <T> T readPayload(WsRequest request, Class<T> type) {
        if (request.payload() == null || request.payload().isNull()) {
            throw new ValidationException("payload is required.");
        }
        try {
            return objectMapper.treeToValue(request.payload(), type);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("payload is invalid.");
        }
    }

    private String writeSafe(WsResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize response", ex);
            return "{\"type\":\"error\",\"status\":\"error\",\"error\":{\"code\":\"serialization_error\",\"message\":\"Failed to serialize response.\"}}";
        }
    }
}
