import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { appService } from '../service'; 
import { UpdateTaskComponent } from './updatetask.component';
import { UpdateTaskRoutingModule } from './updatetask-routing.module';
import { FormsModule } from '@angular/forms';
import { NgbModal, NgbModule, NgbTypeaheadModule, NgbDatepicker} from '@ng-bootstrap/ng-bootstrap';
import { HttpClientModule } from '@angular/common/http'; 
import { HttpModule } from '@angular/http';

@NgModule({
  imports: [UpdateTaskRoutingModule, FormsModule, CommonModule, NgbModule.forRoot(), NgbTypeaheadModule.forRoot()],
  declarations: [UpdateTaskComponent],
  exports: [UpdateTaskComponent, FormsModule],
  providers: [appService]
})

export class UpdateTaskModule { 

}