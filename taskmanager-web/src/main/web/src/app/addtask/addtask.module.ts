import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { appService } from '../service/index'; 
import { AddTaskComponent } from './addtask.component';
import { AddTaskRoutingModule } from './addtask-routing.module';

@NgModule({
  imports: [AddTaskRoutingModule],
  declarations: [AddTaskComponent],
  exports: [AddTaskComponent],
  providers: [appService]
})

export class AddTaskModule { 

}