import { Component, OnInit, Inject, ViewEncapsulation, OnDestroy, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DOCUMENT } from '@angular/platform-browser';
import {NgbTypeahead, NgbTypeaheadSelectItemEvent, NgbDatepicker, NgbDatepickerConfig, NgbDate, NgbCalendar} from '@ng-bootstrap/ng-bootstrap';
import {Observable, Subject, merge} from 'rxjs';
import {debounceTime, distinctUntilChanged, filter, map} from 'rxjs/operators';
declare var jQuery:any;

@Component({

  selector: 'app-addtask',
  templateUrl: 'addtask.component.html',
  styleUrls: ['addtask.component.css']
})
export class AddTaskComponent implements OnInit, OnDestroy {

  @ViewChild('instance') instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();
 
  task : any = {};

  hoveredDate: NgbDate;
  fromDate: NgbDate;
  toDate: NgbDate;
  calendarToday: NgbCalendar
  

  alltaskList : any = [{"taskId":1,"taskName":"task 1","startDate":"2018-10-07","endDate":"2018-10-10","priority":"15","status":"A","parentTask":null},{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null},{"taskId":3,"taskName":"task 333","startDate":"2018-10-11","endDate":"2018-10-16","priority":"1","status":"A","parentTask":null},{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":null},{"taskId":5,"taskName":"task 5","startDate":"2018-10-09","endDate":"2018-10-19","priority":"26","status":"A","parentTask":{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null}},{"taskId":6,"taskName":"task 6","startDate":"2018-10-10","endDate":"2018-10-20","priority":"17","status":"A","parentTask":null},{"taskId":7,"taskName":"task 7","startDate":"2018-10-15","endDate":"2018-10-19","priority":"15","status":"A","parentTask":{"taskId":8,"taskName":"task hello 6","startDate":"2018-10-16","endDate":"2018-10-21","priority":"16","status":"I","parentTask":{"taskId":15,"taskName":"task new","startDate":"2018-10-11","endDate":"2018-10-16","priority":"25","status":"A","parentTask":{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":null}}}},{"taskId":8,"taskName":"task hello 6","startDate":"2018-10-16","endDate":"2018-10-21","priority":"16","status":"I","parentTask":{"taskId":15,"taskName":"task new","startDate":"2018-10-11","endDate":"2018-10-16","priority":"25","status":"A","parentTask":{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":null}}},{"taskId":9,"taskName":"task 9","startDate":"2018-10-17","endDate":"2018-10-26","priority":"19","status":"A","parentTask":{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null}},{"taskId":10,"taskName":"task 10","startDate":"2018-10-20","endDate":"2018-10-24","priority":"1","status":"A","parentTask":{"taskId":3,"taskName":"task 333","startDate":"2018-10-11","endDate":"2018-10-16","priority":"1","status":"A","parentTask":null}},{"taskId":15,"taskName":"task new","startDate":"2018-10-11","endDate":"2018-10-16","priority":"25","status":"A","parentTask":{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":null}},{"taskId":16,"taskName":"task new 1","startDate":"2018-10-11","endDate":"2018-10-16","priority":"25","status":"A","parentTask":{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":null}},{"taskId":17,"taskName":"task new 2","startDate":"2018-10-11","endDate":"2018-10-16","priority":"25","status":"A","parentTask":{"taskId":5,"taskName":"task 5","startDate":"2018-10-09","endDate":"2018-10-19","priority":"26","status":"A","parentTask":{"taskId":2,"taskName":"task 2","startDate":"2018-10-10","endDate":"2018-10-12","priority":"30","status":"A","parentTask":null}}},{"taskId":18,"taskName":"task 555 2","startDate":"2018-10-11","endDate":"2018-10-16","priority":"30","status":"I","parentTask":{"taskId":8,"taskName":"task hello 6","startDate":"2018-10-16","endDate":"2018-10-21","priority":"16","status":"I","parentTask":{"taskId":15,"taskName":"task new","startDate":"2018-10-11","endDate":"2018-10-16","priority":"25","status":"A","parentTask":{"taskId":4,"taskName":"task 4","startDate":"2018-10-13","endDate":"2018-10-23","priority":"0","status":"A","parentTask":null}}}}];

  constructor(calendar: NgbCalendar, config: NgbDatepickerConfig) {
    this.fromDate = calendar.getToday();
    this.calendarToday = calendar;
    this.toDate = calendar.getNext(calendar.getToday(), 'd', 10);
    const currentDate = new Date();
    config.minDate = {year:currentDate.getFullYear(), month:currentDate.getMonth()+1, day: currentDate.getDate()};
    config.maxDate = {year: 2099, month: 12, day: 31};
    config.outsideDays = 'hidden';
  }

  ngOnInit() {
    this.task = {
      "taskName":"",
      "priority":"15",
      "parentTaskId":"",
      "startDate":new Date(),
      "endDate":new Date()
    };
  }

  ngOnDestroy() { 
    
  }

  formatter = (value: any) => value.taskName || '';

  addTask(task: any){
    alert(JSON.stringify(task));
  }

  parentTaskSearch = (text$: Observable<string>) => {
    const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
    const clicksWithClosedPopup$ = this.click$.pipe(filter(() => !this.instance.isPopupOpen()));
    const inputFocus$ = this.focus$;
    return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe(
      map(term => (term === '' ? this.alltaskList : this.alltaskList.filter(v => v.taskName.toLowerCase().indexOf(term.toLowerCase()) > -1)).slice(0, 10))
    );
  }

  selectedParentTaskItem(event: NgbTypeaheadSelectItemEvent): void {
    event.preventDefault();
    jQuery("#parentTask").val(event.item.taskName);
    this.task.parentTaskId = event.item.taskId;
  }

  clearParentId(){
    this.task.parentTaskId = "";
  }

  resetButton(){
    this.task = {
      "taskName":"",
      "priority":"15",
      "parentTaskId":"",
      "startDate":new Date(),
      "endDate":new Date()
    };
    this.fromDate = this.calendarToday.getToday();
    this.toDate = this.calendarToday.getNext(this.calendarToday.getToday(), 'd', 10);
    jQuery("#parentTask").val("");

  }

  /* Datepicker functions*/
  onDateSelection(date: NgbDate) {
    if (!this.fromDate && !this.toDate) {
      this.fromDate = date;
    } else if (this.fromDate && !this.toDate && date.after(this.fromDate)) {
      this.toDate = date;
    } else {
      this.toDate = null;
      this.fromDate = date;
    }
  }

  isHovered(date: NgbDate) {
    return this.fromDate && !this.toDate && this.hoveredDate && date.after(this.fromDate) && date.before(this.hoveredDate);
  }

  isInside(date: NgbDate) {
    return date.after(this.fromDate) && date.before(this.toDate);
  }

  isRange(date: NgbDate) {
    return date.equals(this.fromDate) || date.equals(this.toDate) || this.isInside(date) || this.isHovered(date);
  }

  convertDateJsonToString(json: any){
    if(json !== undefined && json !== null){
      return json.year + '-' + json.month + '-' + json.day;
    }
  }

}



