package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.AIChatRequest;
import com.tt.Restaurant.dto.AIChatResponse;
import com.tt.Restaurant.service.OpenAIService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIChatController {

    private final OpenAIService openAIService;

    public AIChatController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/chat")
    public AIChatResponse chat(@RequestBody AIChatRequest request) {
        String reply = openAIService.chat(request.getMessage());
        return new AIChatResponse(reply);
    }
}