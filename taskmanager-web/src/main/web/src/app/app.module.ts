import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppComponent } from './app.component';
import { AddTaskModule } from './addtask/addtask.module';
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
    BrowserModule, FormsModule, CommonModule, AppRoutingModule, AddTaskModule, HttpModule, HttpClientModule, NgbModule.forRoot(), NgbTypeaheadModule.forRoot()
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
