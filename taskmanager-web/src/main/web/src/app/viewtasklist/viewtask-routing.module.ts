import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ViewTaskComponent } from './viewtask.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      { path: 'viewtask', component: ViewTaskComponent }
    ])
  ],
  exports: [RouterModule]
})
export class ViewTaskRoutingModule { }
