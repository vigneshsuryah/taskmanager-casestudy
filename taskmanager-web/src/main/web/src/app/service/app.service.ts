import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import 'rxjs/add/observable/throw';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';
import { Observable } from 'rxjs/Observable';  
import { Headers, RequestOptions } from '@angular/http';
import { Config } from '../env/index';
import { RouterModule , Router } from '@angular/router';

@Injectable()
export class appService {
  
    constructor(private http: Http, private router : Router) {} 

    getTasks(): Observable<string[]> {
        let headers = new Headers({ 'Accept': '*/*', 'Content-Type':'application/json' });
        let options = new RequestOptions({ headers: headers });
        return this.http.get(Config.API+ "api/tasks", options)
                        .map((res: Response) => res.json())
                        .catch(this.handleErrorNoChange.bind(this));
    }

    addTask(inputParam : {}): Observable<string[]> {
        let headers = new Headers({ 'Accept': '*/*', 'Content-Type':'application/json' });
        let options = new RequestOptions({ headers: headers });
        return this.http.post(Config.API+ "api/tasks", inputParam, options)
                        .map((res: Response) => res.json())
                        .catch(this.handleErrorNoChange.bind(this));
    }

    editTask(inputParam : {}, taskId : string): Observable<string[]> {
        let headers = new Headers({ 'Accept': '*/*', 'Content-Type':'application/json' });
        let options = new RequestOptions({ headers: headers });
        return this.http.put(Config.API+ "api/tasks/" + taskId, inputParam, options)
                        .map((res: Response) => res.json())
                        .catch(this.handleErrorNoChange.bind(this));
    }

    deleteTask(taskId : string): Observable<string[]> {
        let headers = new Headers({ 'Accept': '*/*', 'Content-Type':'application/json' });
        let options = new RequestOptions({ headers: headers });
        return this.http.delete(Config.API+ "api/tasks/"+ taskId, options)
                        .map((res: Response) => res.json())
                        .catch(this.handleErrorNoChange.bind(this));
    }

    private handleErrorNoChange (error: any) {
        let errMsg = (error.message) ? error.message :
        error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        console.log('Error handleErrorNoChange kytpp-service: ' + error);
        return Observable.throw(errMsg);
    }

}