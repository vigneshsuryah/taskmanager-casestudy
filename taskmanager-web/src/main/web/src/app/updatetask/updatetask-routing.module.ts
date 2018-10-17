import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { UpdateTaskComponent } from './updatetask.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      { path: '', component: UpdateTaskComponent },
      { path: 'addtask', component: UpdateTaskComponent },
      { path: 'edittask', component: UpdateTaskComponent }
    ])
  ],
  exports: [RouterModule]
})
export class UpdateTaskRoutingModule { }
