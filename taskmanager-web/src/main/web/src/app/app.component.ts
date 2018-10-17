import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params} from '@angular/router';
import { appService } from './service';
declare var jQuery:any;

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit{
  onActivate(e:any, outlet:any){
    window.scrollTo(0,0);
  }
  
  constructor(public router: Router, private activatedRoute: ActivatedRoute, private appService : appService) {
  }

  ngOnInit() {
    this.appService.updatetask = null;
  }

  addTask(){
    this.appService.updatetask = null;
    jQuery(".navig").removeClass('active-nav');
    jQuery("#add-task").addClass('active-nav');
    this.router.navigate(['/addtask']);
  }

  viewTask(){
    this.appService.updatetask = null;
    jQuery(".navig").removeClass('active-nav');
    jQuery("#view-task").addClass('active-nav');
    this.router.navigate(['/viewtask']);
  }

}
