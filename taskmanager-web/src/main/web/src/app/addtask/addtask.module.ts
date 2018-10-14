import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { appService } from '../service/index'; 
import { AddTaskComponent } from './addtask.component';
import { AddTaskRoutingModule } from './addtask-routing.module';
import { FormsModule } from '@angular/forms';
import { NgbModal, NgbModule, NgbTypeaheadModule, NgbDatepicker} from '@ng-bootstrap/ng-bootstrap';
import { HttpClientModule } from '@angular/common/http'; 
import { HttpModule } from '@angular/http';

@NgModule({
  imports: [AddTaskRoutingModule, FormsModule, CommonModule, NgbModule.forRoot(), NgbTypeaheadModule.forRoot()],
  declarations: [AddTaskComponent],
  exports: [AddTaskComponent, FormsModule],
  providers: [appService]
})

export class AddTaskModule { 

}