package com.vigneshsuryah.springboot.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vigneshsuryah.springboot.entity.Task;
import com.vigneshsuryah.springboot.service.TaskManagerService;

@RestController
public class TaskManagerController {
	
	@Autowired
	private TaskManagerService taskManagerService;
	
	public void setTaskManagerService(TaskManagerService taskManagerService) {
		this.taskManagerService = taskManagerService;
	}
	
	@GetMapping("/api/tasks")
	public List<Task> getEmployees() {
		List<Task> tasks = taskManagerService.retriveTasks();
		return tasks;
	}
}
