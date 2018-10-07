package com.vigneshsuryah.springboot.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;


import com.vigneshsuryah.springboot.entity.Task;

@Repository
public interface TaskManagerRepository extends JpaRepository<Task,Long>{

}

