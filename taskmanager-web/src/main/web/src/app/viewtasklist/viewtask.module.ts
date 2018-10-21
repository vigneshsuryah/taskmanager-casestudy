import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { appService } from '../service/index'; 
import { ViewTaskComponent } from './viewtask.component';
import { ViewTaskRoutingModule } from './viewtask-routing.module';
import { FormsModule } from '@angular/forms';
import { NgbModal, NgbModule, NgbTypeaheadModule, NgbDatepicker} from '@ng-bootstrap/ng-bootstrap';
import { HttpClientModule } from '@angular/common/http'; 
import { HttpModule } from '@angular/http';
import { CommonutilsModule } from '../commonutils.module';

@NgModule({
  imports: [ViewTaskRoutingModule, FormsModule, CommonModule, NgbModule.forRoot(), NgbTypeaheadModule.forRoot(), CommonutilsModule.forRoot()],
  declarations: [ViewTaskComponent],
  exports: [ViewTaskComponent, FormsModule],
  providers: [appService]
})

export class ViewTaskModule { 

}