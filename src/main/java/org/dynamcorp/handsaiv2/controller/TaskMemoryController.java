package org.dynamcorp.handsaiv2.controller;

import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.model.TaskMemory;
import org.dynamcorp.handsaiv2.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memory/tasks")
@RequiredArgsConstructor
public class TaskMemoryController {

    private final MemoryService memoryService;

    @GetMapping
    public ResponseEntity<List<TaskMemory>> getPendingTasks() {
        return ResponseEntity.ok(memoryService.listPendingTasks());
    }

    @GetMapping("/completed")
    public ResponseEntity<List<TaskMemory>> getCompletedTasks() {
        return ResponseEntity.ok(memoryService.listCompletedTasks());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        memoryService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
