package com.weidong.gamebackend.controller;

import com.weidong.gamebackend.assistant.gameAgent;
import com.weidong.gamebackend.model.ChatForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class gameController {

    @Autowired
    private gameAgent gameAgent;

    @PutMapping("/chat")
    public String chat(@RequestBody ChatForm chatForm){
        return gameAgent.chat(chatForm.getMemoryId(),chatForm.getMessage());
    }

}
