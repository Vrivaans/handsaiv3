package org.dynamcorp.handsaiv2.service;

import org.dynamcorp.handsaiv2.model.IntentMemory;
import org.dynamcorp.handsaiv2.model.KnowledgeCategoryEnum;
import org.dynamcorp.handsaiv2.model.KnowledgeMemory;
import org.dynamcorp.handsaiv2.repository.IntentMemoryRepository;
import org.dynamcorp.handsaiv2.repository.KnowledgeMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemoryServiceTest {

    @Mock
    private IntentMemoryRepository intentRepository;

    @Mock
    private KnowledgeMemoryRepository knowledgeRepository;

    @Mock
    private org.dynamcorp.handsaiv2.repository.TaskMemoryRepository taskRepository;

    @InjectMocks
    private MemoryService memoryService;

    private IntentMemory mockIntent;
    private KnowledgeMemory mockKnowledge;

    @BeforeEach
    void setUp() {
        mockIntent = new IntentMemory();
        mockIntent.setId(1L);
        mockIntent.setAgentId("agent-1");
        mockIntent.setIntent("test intent");
        mockIntent.setCompleted(false);

        mockKnowledge = new KnowledgeMemory();
        mockKnowledge.setId(10L);
        mockKnowledge.setTitle("A test bugfix");
        mockKnowledge.setCategory(KnowledgeCategoryEnum.BUGFIX);
    }

    @Test
    void testSaveIntent() {
        when(intentRepository.save(any(IntentMemory.class))).thenReturn(mockIntent);

        IntentMemory saved = memoryService.saveIntent("agent-1", "session-1", "test intent", "facts", 0.9, null,
                "[\"tag1\"]");

        assertNotNull(saved);
        assertEquals("agent-1", saved.getAgentId());
        verify(intentRepository, times(1)).save(any(IntentMemory.class));
    }

    @Test
    void testGetActiveIntents_WithAgentId() {
        when(intentRepository.findByAgentIdAndCompletedOrderByCreatedAtDesc("agent-1", false))
                .thenReturn(List.of(mockIntent));

        List<IntentMemory> intents = memoryService.getActiveIntents("agent-1", null);

        assertEquals(1, intents.size());
        verify(intentRepository, times(1)).findByAgentIdAndCompletedOrderByCreatedAtDesc("agent-1", false);
    }

    @Test
    void testCompleteIntent() {
        when(intentRepository.findById(1L)).thenReturn(Optional.of(mockIntent));
        when(intentRepository.save(any(IntentMemory.class))).thenReturn(mockIntent);

        Optional<IntentMemory> completed = memoryService.completeIntent(1L);

        assertTrue(completed.isPresent());
        assertTrue(completed.get().isCompleted());
        verify(intentRepository, times(1)).save(mockIntent);
    }

    @Test
    void testSaveKnowledge() {
        when(knowledgeRepository.save(any(KnowledgeMemory.class))).thenReturn(mockKnowledge);

        KnowledgeMemory saved = memoryService.saveKnowledge("A test bugfix", KnowledgeCategoryEnum.BUGFIX, "what",
                "why", "where", "learned");

        assertNotNull(saved);
        assertEquals(KnowledgeCategoryEnum.BUGFIX, saved.getCategory());
        verify(knowledgeRepository, times(1)).save(any(KnowledgeMemory.class));
    }

    @Test
    void testSearchKnowledge_WithQuery() {
        when(knowledgeRepository.searchByContentOrTitleIgnoreCase("bugfix"))
                .thenReturn(List.of(mockKnowledge));

        List<KnowledgeMemory> results = memoryService.searchKnowledge("bugfix", null);

        assertEquals(1, results.size());
        verify(knowledgeRepository, times(1)).searchByContentOrTitleIgnoreCase("bugfix");
    }

    @Test
    void testCreateTask() {
        org.dynamcorp.handsaiv2.model.TaskMemory mockTask = new org.dynamcorp.handsaiv2.model.TaskMemory();
        mockTask.setId(100L);
        mockTask.setTitle("Test task");
        mockTask.setStatus("PENDING");

        when(taskRepository.save(any(org.dynamcorp.handsaiv2.model.TaskMemory.class))).thenReturn(mockTask);

        org.dynamcorp.handsaiv2.model.TaskMemory saved = memoryService.createTask("Test task", "desc", "HIGH",
                "agent-x");

        assertNotNull(saved);
        assertEquals("PENDING", saved.getStatus());
        verify(taskRepository, times(1)).save(any(org.dynamcorp.handsaiv2.model.TaskMemory.class));
    }

    @Test
    void testListPendingTasks() {
        org.dynamcorp.handsaiv2.model.TaskMemory mockTask = new org.dynamcorp.handsaiv2.model.TaskMemory();
        when(taskRepository.findByStatusNotOrderByPriorityDescCreatedAtAsc("COMPLETED"))
                .thenReturn(List.of(mockTask));

        List<org.dynamcorp.handsaiv2.model.TaskMemory> tasks = memoryService.listPendingTasks();

        assertEquals(1, tasks.size());
        verify(taskRepository, times(1)).findByStatusNotOrderByPriorityDescCreatedAtAsc("COMPLETED");
    }

    @Test
    void testUpdateTaskStatus() {
        org.dynamcorp.handsaiv2.model.TaskMemory mockTask = new org.dynamcorp.handsaiv2.model.TaskMemory();
        mockTask.setId(100L);
        mockTask.setStatus("PENDING");

        when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(org.dynamcorp.handsaiv2.model.TaskMemory.class))).thenReturn(mockTask);

        Optional<org.dynamcorp.handsaiv2.model.TaskMemory> updated = memoryService.updateTaskStatus(100L,
                "IN_PROGRESS");

        assertTrue(updated.isPresent());
        assertEquals("IN_PROGRESS", updated.get().getStatus());
        verify(taskRepository, times(1)).save(mockTask);
    }

    @Test
    void testDeleteTask() {
        doNothing().when(taskRepository).deleteById(100L);
        memoryService.deleteTask(100L);
        verify(taskRepository, times(1)).deleteById(100L);
    }
}
