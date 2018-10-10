import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { appService } from '../service/index'; 
import { AddTaskComponent } from './addtask.component';
import { AddTaskRoutingModule } from './addtask-routing.module';
import { FormsModule } from '@angular/forms';

@NgModule({
  imports: [AddTaskRoutingModule, FormsModule],
  declarations: [AddTaskComponent],
  exports: [AddTaskComponent, FormsModule],
  providers: [appService]
})

export class AddTaskModule { 

}