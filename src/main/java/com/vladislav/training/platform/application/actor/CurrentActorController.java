package com.vladislav.training.platform.application.actor;

import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code CurrentActorController}.
 */
@RestController
@RequestMapping("/api/v1/me")
public class CurrentActorController {

    private final CurrentActorReadService currentActorReadService;

    public CurrentActorController(CurrentActorReadService currentActorReadService) {
        this.currentActorReadService = Objects.requireNonNull(
            currentActorReadService,
            "currentActorReadService must not be null"
        );
    }

    @GetMapping
    public CurrentActorResponse getCurrentActor() {
        return currentActorReadService.readCurrentActor();
    }
}
