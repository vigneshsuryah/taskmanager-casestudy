import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { AddTaskModule } from './addtask/addtask.module';
import { RequestOptions, RequestMethod, Headers } from '@angular/http';
import { AppRoutingModule } from './app-routing.module'; 
import { FormsModule } from '@angular/forms';

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
    BrowserModule, FormsModule, AppRoutingModule, AddTaskModule
  ],
  exports: [
    FormsModule
  ],
  providers: [
    {
    provide: RequestOptions,
    useClass: MyOptions
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule{ }
