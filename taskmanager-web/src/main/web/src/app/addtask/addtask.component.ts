import { Component, OnInit, Inject, ViewEncapsulation, OnDestroy } from '@angular/core';
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

  constructor() {

  }

  ngOnInit() {

  }

  ngOnDestroy() {
    
  }
}



