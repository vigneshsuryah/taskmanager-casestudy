import { NgModule, ModuleWithProviders } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { TaskSearchPipe } from './pipes/searchtask';

@NgModule({
  imports: [CommonModule, RouterModule, FormsModule],
  declarations: [TaskSearchPipe],
  exports: [CommonModule, FormsModule, RouterModule, TaskSearchPipe],
  providers: [TaskSearchPipe]
})
export class CommonutilsModule {

  static forRoot(): ModuleWithProviders {
    return {
      ngModule: CommonutilsModule,      
    };
  }

  
}
