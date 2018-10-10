import { Component, OnInit, Inject, ViewEncapsulation, OnDestroy, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { DOCUMENT } from '@angular/platform-browser';
declare var jQuery:any;

@Component({

  selector: 'app-addtask',
  templateUrl: 'addtask.component.html',
  styleUrls: ['addtask.component.css']
})
export class AddTaskComponent implements OnInit, OnDestroy {

  task : any = {
    "taskName":"",
    "priority":"50",
    "parentTaskId":""
  };

  alltaskList : any = [{"taskId":1,"taskName":"task 1","startDate":"2018-10-07","endDate":"2018-10-10","priority":"15","status":"A","parentTask":null},{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null},{"taskId":3,"taskName":"task 3","startDate":"2018-10-11","endDate":"2018-10-16","priority":"1","status":"A","parentTask":null},{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null}},{"taskId":5,"taskName":"task 5","startDate":"2018-10-09","endDate":"2018-10-19","priority":"26","status":"A","parentTask":{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null}},{"taskId":6,"taskName":"task 6","startDate":"2018-10-10","endDate":"2018-10-20","priority":"17","status":"A","parentTask":{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null}}},{"taskId":7,"taskName":"task 7","startDate":"2018-10-15","endDate":"2018-10-19","priority":"15","status":"A","parentTask":null},{"taskId":8,"taskName":"task 8","startDate":"2018-10-16","endDate":"2018-10-21","priority":"16","status":"A","parentTask":{"taskId":3,"taskName":"task 3","startDate":"2018-10-11","endDate":"2018-10-16","priority":"1","status":"A","parentTask":null}},{"taskId":9,"taskName":"task 9","startDate":"2018-10-17","endDate":"2018-10-26","priority":"19","status":"A","parentTask":{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null}},{"taskId":10,"taskName":"task 10","startDate":"2018-10-20","endDate":"2018-10-24","priority":"1","status":"A","parentTask":{"taskId":3,"taskName":"task 3","startDate":"2018-10-11","endDate":"2018-10-16","priority":"1","status":"A","parentTask":null}}];

  constructor() {

  }

  ngOnInit() {

  }

  ngOnDestroy() {
    
  }

  addTask(task: any){
    alert(JSON.stringify(task));
  }

}



