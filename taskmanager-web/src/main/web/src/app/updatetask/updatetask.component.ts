import { Component, OnInit, Inject, ViewEncapsulation, OnDestroy, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DOCUMENT } from '@angular/platform-browser';
import { NgbTypeahead, NgbTypeaheadSelectItemEvent, NgbDatepicker, NgbDatepickerConfig, NgbDate, NgbCalendar, NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject, merge} from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map} from 'rxjs/operators';
import { appService } from '../service';
declare var jQuery:any;

@Component({

  selector: 'app-updatetask',
  templateUrl: 'updatetask.component.html',
  styleUrls: ['updatetask.component.css']
})
export class UpdateTaskComponent implements OnInit, OnDestroy {

  @ViewChild('instance') instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();
 
  task : any = {};
  hoveredDate: NgbDate;
  fromDate: NgbDate;
  toDate: NgbDate;
  calendarToday: NgbCalendar
  alltaskList : any = [];
  errorShow : boolean = false;
  screenLoader : boolean = false;
  errorMessage : string = '';
  modalHeading : string = '';
  modalBody : string = '';
  flow : string = 'addtask';
  selectedParentTaskObj : any = {};

  constructor(calendar: NgbCalendar, config: NgbDatepickerConfig, public router: Router, private appService : appService) {
    this.calendarToday = calendar;
    if(this.appService.updatetask !== null){
      this.flow = 'updatetask';
    }
    if(this.flow === 'addtask'){
      this.task = {
        "taskName":"",
        "priority":"15",
        "parentTaskId":"",
        "parentTaskName":"",
        "startDate":new Date(),
        "endDate":new Date()
      };
      this.fromDate = calendar.getToday();
      this.toDate = calendar.getNext(calendar.getToday(), 'd', 10);
    }

    if(this.flow === 'updatetask'){
      var edittask = this.appService.updatetask;
      this.selectedParentTaskObj = edittask.parentTask !== null ? edittask.parentTask : null;
      this.task = {
        "taskName":edittask.taskName,
        "priority":edittask.priority,
        "parentTaskId":edittask.parentTask !== null ? edittask.parentTask.taskId : '',
        "parentTaskName":edittask.parentTask !== null ? edittask.parentTask.taskName : '',
        "startDate":new Date(),
        "endDate":new Date()
      };
      this.fromDate = NgbDate.from(this.constructDateFromService(edittask.startDate));
      this.toDate = NgbDate.from(this.constructDateFromService(edittask.endDate));
    }

    const currentDate = new Date();
    config.minDate = {year:currentDate.getFullYear(), month:currentDate.getMonth()+1, day: currentDate.getDate()};
    config.maxDate = {year: 2099, month: 12, day: 31};
    config.outsideDays = 'hidden';

    this.screenLoader = true;
    appService.getTasks().subscribe((data :any) => {
      this.alltaskList = data;
      this.screenLoader = false;
    });
  }

  ngOnInit() {
    
  }

  ngOnDestroy() {
    this.task = {};
  }

  formatter = (value: any) => value.taskName || '';

  updateTask(task: any){
    var parentTaskNameVal = jQuery("#parentTask").val();
    this.errorShow = false;
    this.errorMessage = '';
    if(parentTaskNameVal !== '' && this.task.parentTaskId === ''){
      this.errorShow = true;
      this.errorMessage = 'Select Parent Task from the list available. Either the task name is edited or you have typed a custom task name.';
    }else{
      if(this.flow === 'addtask'){
        var submitAddTask = {};
        if(this.task.parentTaskId === '' || this.task.parentTaskId === null || this.task.parentTaskId === undefined){
          submitAddTask = {
            "taskName": this.task.taskName,
            "startDate": this.convertDateJsonToString(this.fromDate),
            "endDate": this.convertDateJsonToString(this.toDate),
            "priority": this.task.priority,
            "status": "A"
          };
        }else{
          submitAddTask = {
            "taskName": this.task.taskName,
            "startDate": this.convertDateJsonToString(this.fromDate),
            "endDate": this.convertDateJsonToString(this.toDate),
            "priority": this.task.priority,
            "status": "A",
            "parentTask": {
              "taskId" : this.task.parentTaskId === '' ? null: this.task.parentTaskId
            }
          };
        }
        
        this.screenLoader = true;
        this.appService.addTask(submitAddTask).subscribe(
          (data: any) => {
            this.screenLoader = false;
            this.modalHeading = 'Yeah :-)';
            this.modalBody = 'Task Added Successfully';
            document.getElementById("submitModalOpener").click();
          },
          (err: any) => {
              this.screenLoader = false;
              this.modalHeading = 'Oh No !!!';
              this.modalBody = 'Unexpected error occured during Add Task. Please try after some time.';
              document.getElementById("submitModalOpener").click();        
            }
          );
      }
      if(this.flow === 'updatetask'){
        var submitUpdateTask = {
          "taskId": this.appService.updatetask.taskId,
          "taskName": this.task.taskName,
          "startDate": this.convertDateJsonToString(this.fromDate),
          "endDate": this.convertDateJsonToString(this.toDate),
          "priority": this.task.priority,
          "status": "A",
          "parentTask": this.selectedParentTaskObj
        };
        this.screenLoader = true;
        this.appService.editTask(submitUpdateTask, this.appService.updatetask.taskId).subscribe(
          (data: any) => {
            this.screenLoader = false;
            this.modalHeading = 'Yeah :-)';
            this.modalBody = 'Task Updated Successfully';
            document.getElementById("submitModalOpener").click();
          },
          (err: any) => {
              this.screenLoader = false;
              this.modalHeading = 'Oh No !!!';
              this.modalBody = 'Unexpected error occured during Update Task. Please try after some time.';
              document.getElementById("submitModalOpener").click();        
            }
          );
      }
    }
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
    this.selectedParentTaskObj = event.item;
    jQuery("#parentTask").val(event.item.taskName);
    this.task.parentTaskId = event.item.taskId;
  }

  clearParentId(event){
    if (event.key !== "Enter") {
      this.task.parentTaskId = "";
      this.selectedParentTaskObj = null;
    }
  }

  constructDateFromService(datestring: string){
    var res = datestring.split("/");
    const date: NgbDateStruct = { day: parseInt(res[0]), month: parseInt(res[1]), year: parseInt(res[2]) };
    return date;
  }

  resetButton(){
    this.task = {
      "taskName":"",
      "priority":"15",
      "parentTaskId":"",
      "parentTaskName":"",
      "startDate":new Date(),
      "endDate":new Date()
    };
    this.fromDate = this.calendarToday.getToday();
    this.toDate = this.calendarToday.getNext(this.calendarToday.getToday(), 'd', 10);
    jQuery("#parentTask").val("");
  }
  
  viewTaskScreen(){
    document.getElementById("view-task").click();
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
      return json.day + '/' + json.month + '/' + json.year;
    }
  }

}



