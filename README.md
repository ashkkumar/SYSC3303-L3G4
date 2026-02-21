# Firefighting Drone Swarm Project  
**SYSC 3303 – Real-Time Concurrent Systems**  
**Group 4**

## Group Members
- Ashwin Kumar  
- Abhiram Sureshkumar  
- Jason Keah  
- Maryam Manjra  

## Project Overview

This project simulates a **firefighting drone swarm system** using concurrent subsystems that communicate through a central Scheduler. The system models how fire incidents are detected, dispatched, and handled by drones in a time-based simulation environment.

The project emphasizes:
- Thread-based concurrency  
- Subsystem interaction  
- Event-driven scheduling  
- Incremental system development across iterations

## Subsystems 

#### Drone Subsystem (`DroneSubsystem`)

- `Drone` – Active thread representing a drone  
- `DroneStatus` – Enum representing drone states (IDLE, EN_ROUTE, REFILLING, etc)

#### Fire Incident Subsystem (`FireIncidentSubsystem`)
- `FireEvent` – Represents a fire incident  
- `InputReader` – Parses zone and event CSV files  
- `Severity` – Enum representing fire severity  
- `TaskType` – Enum representing incident task types  
- `Zone` – Represents a geographic fire zone  
- `IncidentReporter` – Thread responsible for notifying the Scheduler  

#### Scheduler (`Scheduler`)
- Acts as the communication hub between subsystems  
- Buffers fire events and dispatches tasks to drones  
- Receives task completion confirmations  

#### User Interface (`UserInterface`)
- `ZoneMapController` - Controller for the view
- `ZoneMapView` - Java Swing view displaying drone dispatch
- Swing-based graphical interface  
- Can be run independently from the simulation logic

## Technology & Requirements

- **Java Version:** OpenJDK 21.0.8  
  (Earlier Java versions supporting standard `Thread`, `Swing`, and `ArrayDeque<>` functionality may also work.)
- **Build Tool:** Maven  
- **UI Framework:** Java Swing  

## Project Structure

All project source code is located under the package:

src/main/resources
These include:
Zone definition CSV files
Fire event CSV files
All test scenarios use these files for repeatable system testing.
How to Set Up and Run the Project
1. Import the Project
Open the project in IntelliJ IDEA (or another Java IDE)
Import as a Maven project
Ensure the correct JDK (OpenJDK 21.0.8) is selected
2. Build the Project
Using Maven:
mvn clean compile
Or build directly through your IDE.
3. Run the Simulation (Iteration 2)
Navigate to: FireFightingDroneSwarm.DroneSystemTest Run the main() method
Console output will display: Zone loading, Fire event parsing, Scheduler buffering, Drone dispatch and task completion and display a GUI to visually demonstrate the drones in flight completing tasks

## Testing
System-level testing for Iteration 2 is performed through the DroneSystemTest.java class, running this file will execute the main() and perform the tests. This is in addition to individual junit testing classes.
A single test function is used multipleIncidents() in DroneSystemTest.java.
Multiple test scenarios are executed using different input CSV files to validate:
- Subsystem communication
- Task sequencing
- Concurrent execution behavior
- Note: Due to concurrency, console output may appear interleaved. This is expected.
- Junit tests are available as well for the DroneSubsystem, FireIncidentSubsystem, and Scheduler. To execute these tests open the junit test file (for example DroneTest.java) in Intellij and click the execute button.

## Responsibility Breakdown
- Ashwin Kumar – Drone logic, State implementation, Junit tests, Sequence Diagram
- Maryam Manjra – User interface update, ZoneMapController, ZoneMapView
- Jason Keah – Scheduler update - scheduling logic
- Abhiram Sureshkumar – Drone logic, UML Diagram, State Machine Diagram
