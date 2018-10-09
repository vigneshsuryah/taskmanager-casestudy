import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AddTaskComponent } from './addtask.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      { path: '', component: AddTaskComponent },
      { path: 'addtask', component: AddTaskComponent }
    ])
  ],
  exports: [RouterModule]
})
export class AddTaskRoutingModule { }
