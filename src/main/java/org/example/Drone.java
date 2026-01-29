package org.example;

public class Drone implements Runnable{

    private final int droneId;
    private final Scheduler scheduler;
    private FireEvent task;

    public Drone(int droneId, FireEvent task){
        this.droneId = droneId;
        this.task = task;
    }

    /**
     * Override method of run() from Runnable
     */
    @Override
    public void run() {
        while(true){
            waitForTask();
            extinguishFire();
            notifyScheduler();
        }
    }

    public synchronized void assignTask(FireEvent task){
        this.task = task;
        notify();
    }
    private synchronized void waitForTask(){
        while(task == null){
            try{
                wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    public void startTask(){
        System.out.println("Deploying drone #" + this.droneId + "extinguishing fire");
        deployDrone();
        System.out.println("Drone #" + this.droneId + " has been deployed");
        // another method to "extinguish" fires needa wait to see how severity is done
        extinguishFire();
        System.out.println("Drone #" + this.droneId + " fire extinguished");
        returnToBase();
        System.out.println("Drone #" + this.droneId + " has returned");

    }

    public void extinguishFire(){
        // remove task here or something
        return;
    }

    public void deployDrone(){
        sleep(3000);
    }
    public void returnToBase(){
        sleep(2000);
    }

    public void sleep(long millis){
        try{
            Thread.sleep(millis);
        }catch(InterruptedException e){}
    }

    public void notifyScheduler(){
        scheduler.;
        this.task = null;
    }

}
