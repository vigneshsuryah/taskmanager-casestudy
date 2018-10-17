import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'taskSearchPipe', pure: false})
export class TaskSearchPipe implements PipeTransform{
    transform(items: Array<any>, filtertask : any){
        if (items && items.length){
            return items.filter(item =>{
                if (filtertask.taskName && item.taskName.toLowerCase().indexOf(filtertask.taskName.toLowerCase()) === -1){
                    return false;
                }
                if (filtertask.parentTaskName && item.parentTask && item.parentTask.taskName &&
                    item.parentTask.taskName.toLowerCase().indexOf(filtertask.parentTaskName.toLowerCase()) === -1){
                    return false;
                }
                if (filtertask.parentTaskName !== undefined && filtertask.parentTaskName !== null && filtertask.parentTaskName !== '' && 
                    (item.parentTask === null || item.parentTask === undefined || item.parentTask.taskName === '')){
                    return false;
                }
                if (filtertask.startDate && item.startDate.toLowerCase().indexOf(filtertask.startDate.toLowerCase()) === -1){
                    return false;
                }
                if (filtertask.endDate && item.taskName.toLowerCase().indexOf(filtertask.endDate.toLowerCase()) === -1){
                    return false;
                } 
                if (filtertask.priorityFrom && item.priority && parseInt(item.priority) < parseInt(filtertask.priorityFrom)){
                    return false;
                }
                if (filtertask.priorityTo && item.priority && parseInt(item.priority) > parseInt(filtertask.priorityTo)){
                    return false;
                }
                return true;
           })
        }
        else{
            return items;
        }
    }
}