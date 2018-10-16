import { Component, OnInit, Inject, ViewEncapsulation, OnDestroy, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DOCUMENT } from '@angular/platform-browser';
import { NgbTypeahead, NgbTypeaheadSelectItemEvent, NgbDatepicker, NgbDatepickerConfig, NgbDate, NgbCalendar} from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject, merge} from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map} from 'rxjs/operators';
import { appService } from '../service/index';
declare var jQuery:any;

@Component({
  selector: 'app-viewtask',
  templateUrl: 'viewtask.component.html',
  styleUrls: ['viewtask.component.css']
})
export class ViewTaskComponent implements OnInit, OnDestroy {

  calendarToday: NgbCalendar
  alltaskList : any = [];
  task : any = {};
  screenLoader : boolean = false;
  modalHeading : string = '';
  modalBody : string = '';

  constructor(calendar: NgbCalendar, config: NgbDatepickerConfig, public router: Router, private appService : appService) {

    //this.screenLoader = true;
    appService.getTasks().subscribe((data :any) => {
      this.alltaskList = data;
      this.screenLoader = false;
    });
  }

  ngOnInit() {
    
  }

  ngOnDestroy() {
    
  }

  onDateSelectPicker(date: NgbDate, field: string){
    if(field === 'start'){
      this.task.startDate = this.convertDateJsonToString(this.task.startDate);
      setTimeout(()=>{
        jQuery("#startDate").val(this.task.startDate);
      },50);
    }else if(field === 'end'){
      this.task.endDate = this.convertDateJsonToString(this.task.endDate);
      setTimeout(()=>{
        jQuery("#endDate").val(this.task.endDate);
      },50);
    }
  }

  convertDateJsonToString(json: any){
    if(json !== undefined && json !== null){
      return json.day + '/' + json.month + '/' + json.year;
    }
  }

}



