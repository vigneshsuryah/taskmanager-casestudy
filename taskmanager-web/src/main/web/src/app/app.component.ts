import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params} from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit{
  onActivate(e:any, outlet:any){
    window.scrollTo(0,0);
  }
  
  constructor(public router: Router, private activatedRoute: ActivatedRoute) {
  }

  ngOnInit() {
    
  }
}
