<h1 align="center">Build-To-Order Management System (BTOMS)</h1>

<div align="center">
<p>NTU AY2024/25 Semester 2 | SC2002 Group Project</p>

[![Javadoc Badge](https://img.shields.io/badge/Javadoc-F8981D?style=for-the-badge&logo=readthedocs&logoColor=FFFFFF&logoSize=auto&labelColor=222222)](https://danielwang0099.github.io/SC2002/)
&nbsp;
[![Class Diagrams Badge](https://img.shields.io/badge/Class%20Diagrams-C2F0C0?style=for-the-badge&logo=diagramsdotnet&logoColor=FFFFFF&logoSize=auto&labelColor=222222)](https://github.com/DanielWang0099/SC2002/blob/main/docs/classDiagram/main.png)
&nbsp;
[![Sequence Diagrams Badge](https://img.shields.io/badge/Sequence%20Diagrams-FFF6B6?style=for-the-badge&logo=miro&logoSize=auto&labelColor=222222)](https://github.com/DanielWang0099/SC2002/blob/main/docs/sequenceDiagram/mainFunctionalities/Application%20Process.jpg)

👆 Click on the buttons above to view our work! 👆

<p align="center">
<a href="#introduction">Introduction</a> &nbsp;&bull;&nbsp;
<a href="#project-structure">Project Structure</a> &nbsp;&bull;&nbsp;
<a href="#team-members">Team Members</a> &nbsp;&bull;&nbsp;
<a href="#features">Features</a> &nbsp;&bull;&nbsp;
<a href="#getting-started">Getting Started</a>
</p>
</div>

## Introduction
The Build-To-Order Management System (BTOMS) is a Java-based command line interface (CLI) application that serves as a centralised hub for applicants and Housing Development Board (HDB) staff to view, apply and manage BTO projects. This project is developed as part of the SC2002, Object-Oriented Design & Programming, module assignment at Nanyang Technological University, Singapore. 


## Project Structure
- `docs/`
  - `classDiagram/`: Stores the project UML class diagrams.
  - `javadoc/`: Comprises of the generated Javadoc HTML.
  - `sequenceDiagram/`: Stores the project UML sequence diagrams.
- `resources`: Stores the sample data files (e.g. user lists and BTO project data).
- `src`: Contains all the Java source files.


## Team Members

| **Name**           | **GitHub Profile**                                  | **Email Address**        |
|--------------------|-----------------------------------------------------|--------------------------|
| Gerald Koh         | [![GitHub Badge](https://img.shields.io/badge/callmegerlad-%23181717?logo=github)](https://github.com/callmegerlad) | GKOH015@e.ntu.edu.sg     |
| Daniel Wang        | [![GitHub Badge](https://img.shields.io/badge/DanielWang0099-%23181717?logo=github)](https://github.com/DanielWang0099) | WA0001EL@e.ntu.edu.sg    |
| Lin Xin            | [![GitHub Badge](https://img.shields.io/badge/delelin-%23181717?logo=github)](https://github.com/delelin) | XLIN030@e.ntu.edu.sg     |
| Shuen Hwee         | [![GitHub Badge](https://img.shields.io/badge/shenxh24-%23181717?logo=github)](https://github.com/shenxh24) | SIMS0047@e.ntu.edu.sg    |
| Tripathi Eishani   |                                                     |                          |



## Features
- [x] **System**
  - [x] Login.
  - [x] View profile.
  - [x] Change password.
  - [x] Filter for project listing (user's setting persists throughout program execution).
- [x] **Applicant**
    - [x] View available projects.
    - [x] Apply for a project.
    - [x] View their application status.
    - [x] Create booking for one flat (upon successful application).
    - [x] Submit request for withdrawal of application.
    - [x] Create, read, update, delete (CRUD) enquiries regarding BTO projects.
- [x] **HDB Officer**
    - [x] Possesses all of Applicant's capabilities.
    - [x] Register for a project.
    - [x] View their registration status.
    - [x] View details of project (upon successful registration).
    - [x] Respond to (and view) enquiries of the project.
    - [x] Generate receipt of applicants with their respective flat booking details.
- [x] **HDB Manager**
    - [x] CRUD BTO project listings.
    - [x] Approve/reject HDB Officer's registration.
    - [x] Approve/reject Applicant's application.
    - [x] Approve/reject Applicant's request to withdraw the application.
    - [x] Respond to (and view) all enquiries.
    - [x] Generate report on Applicants and their respective flat booking details.


## Getting Started

### 1. Clone the Repository

Clone this repository and navigate into the project folder:
```shell
git clone https://github.com/DanielWang0099/SC2002.git
cd SC2002
```

### 2. Compile all the Java source files

Compiles all Java source files in `src` and stores them in the directory, `bin`:
```shell
javac -d ./bin ./src/**/*.java
```

### 3. Run the application

Execute the main class: `src/Main.java`.
```shell
java -cp ./bin Main
```

### Alternatively: Compile & Run in One Step

Combining steps 2 and 3, we can compile and run the compiled application with just one command:
```shell
javac -d ./bin ./src/**/*.java && java -cp ./bin Main
```


## License

This project is for educational purposes only.
