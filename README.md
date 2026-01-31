# Firefighting Drone Swarm Project
# SYSC3303, Group 4
Members: Ashwin Kumar, Abhiram Sureshkumar, Jason Keah, Maryam Manjra 

This project is built using OpenJDK 21.0.8, prior versions supporting standard Swing, Thread, and the ArrayDeque<> implementations are necessary. 

#Iteration 1 
All project code files can be found in the package labelled FireFightingDroneSwarm. The main system setup and deployment class is labelled DroneSystemTest. 
The Drone Subsystem is implemented in the subpackage DroneSubsystem with classes for the Drone thread and an enum for Drone Status. 

The Incident Subsystem is implemented in the subpackage FireIncidentSubsystem and includes an object for representing events called FireEvent, a input parsing class called InputReader, enums for severity, and task type, a Zone class, and finally the IncidentReporter thread for notifying the scheduler.

The Scheduler is implemented in the subpackage Scheduler and has minimal implementation for acting as a communication channel. 

The UI is implemented in the UserInterface subpackage using Swing, and can be run separately from the system in the UserInterface class file.

Resources like the zone and event files are located in src/main/resources. 

To run the simulation, please run main in the DroneSystemTest file. Console output will be visible for the events. To view the GUI please run the main method in UserInterface. 

Responsibility breakdown:
Ashwin Kumar - Drone Subsystem + Test 
Maryam Manjra - Fire Incident Subsystem + Sequence Diagram
Jason Keah - GUI + UML Class Diagram
Abhiram Sureshkumar - Scheduler 
