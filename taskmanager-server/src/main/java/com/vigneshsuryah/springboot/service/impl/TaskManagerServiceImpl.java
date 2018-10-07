package com.vigneshsuryah.springboot.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vigneshsuryah.springboot.entity.Task;
import com.vigneshsuryah.springboot.repository.TaskManagerRepository;
import com.vigneshsuryah.springboot.service.TaskManagerService;

@Service
public class TaskManagerServiceImpl implements TaskManagerService{

	@Autowired
	private TaskManagerRepository taskManagerRepository;
	
	public void setTaskManagerRepository(TaskManagerRepository taskManagerRepository) {
		this.taskManagerRepository = taskManagerRepository;
	}
	
	public List<Task> retriveTasks(){
		List<Task> tasks = taskManagerRepository.findAll();
		return tasks;
	}

	public void updateTask(Task task) {
		taskManagerRepository.save(task);
	}
	
	public Task getTask(Long taskId) {
		Optional<Task> optTask = taskManagerRepository.findById(taskId);
		return optTask.get();
	}
}
