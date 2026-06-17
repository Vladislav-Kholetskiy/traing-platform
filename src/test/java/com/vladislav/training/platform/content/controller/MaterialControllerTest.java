package com.vladislav.training.platform.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.service.CreateMaterialCommand;
import com.vladislav.training.platform.content.service.MaterialCommandService;
import com.vladislav.training.platform.content.service.MaterialQueryService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code Material}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private MaterialCommandService commandService;
    @Mock private MaterialQueryService queryService;
    @Mock private UtcClock utcClock;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MaterialController(commandService, queryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void createDelegatesToDraftCommandService() throws Exception {
        when(commandService.createMaterial(any())).thenReturn(new Material(10L, 20L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/materials")
                .contentType("application/json")
                .content("""
                    {
                      "topicId": 20,
                      "name": "Material",
                      "description": "",
                      "materialType": "TEXT",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.topicId").value(20))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        ArgumentCaptor<CreateMaterialCommand> captor = ArgumentCaptor.forClass(CreateMaterialCommand.class);
        verify(commandService).createMaterial(captor.capture());
        assertThat(captor.getValue().topicId()).isEqualTo(20L);
        assertThat(captor.getValue().name()).isEqualTo("Material");
        assertThat(captor.getValue().description()).isEmpty();
        assertThat(captor.getValue().materialType()).isEqualTo(MaterialType.TEXT);
        assertThat(captor.getValue().sortOrder()).isEqualTo(0);
    }

    @Test
    void updateUsesDedicatedUpdateDtoWithoutParentId() throws Exception {
        when(commandService.updateMaterial(any(), any())).thenReturn(new Material(10L, 20L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(patch("/api/v1/expert/content/materials/10")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "Material",
                    "description", "",
                    "materialType", "TEXT",
                    "sortOrder", 0
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topicId").value(20));

        verify(commandService).updateMaterial(any(), any());
    }

    @Test
    void updateIgnoresLifecycleFieldInGenericSurface() throws Exception {
        when(commandService.updateMaterial(any(), any())).thenReturn(new Material(10L, 20L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(patch("/api/v1/expert/content/materials/10")
                .contentType("application/json")
                .content("""
                    {
                      "name": "Material",
                      "description": "",
                      "materialType": "TEXT",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(commandService).updateMaterial(any(), any());
    }
}
