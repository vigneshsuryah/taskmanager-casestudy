import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppComponent } from './app.component'; 
import { UpdateTaskModule } from './updatetask/updatetask.module'; 
import { ViewTaskModule } from './viewtask/viewtask.module';
import { RequestOptions, RequestMethod, Headers } from '@angular/http';
import { AppRoutingModule } from './app-routing.module'; 
import { FormsModule } from '@angular/forms';
import { NgbModal, NgbModule, NgbTypeaheadModule } from '@ng-bootstrap/ng-bootstrap';
import { appService } from './service/index';
import { HttpClientModule } from '@angular/common/http'; 
import { HttpModule } from '@angular/http';

export class MyOptions extends RequestOptions {
  constructor() {
    super({
      method: RequestMethod.Get,
      headers: new Headers({
        'Content-Type': 'application/json'
      })
    });
  }
}

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule, FormsModule, CommonModule, AppRoutingModule, UpdateTaskModule, ViewTaskModule, HttpModule, HttpClientModule, NgbModule.forRoot(), NgbTypeaheadModule.forRoot()
  ],
  exports: [
    FormsModule
  ],
  providers: [
    {
    provide: RequestOptions,
    useClass: MyOptions
    },
    appService
  ],
  bootstrap: [AppComponent]
})
export class AppModule{ }
