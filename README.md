# Build-To-Order Management System (BTOMS)

NTU AY2024/25 Semester 2 | SC2002 Group Project


## Project Overview
The Build-To-Order Management System (BTOMS) is a Java-based command line interface (CLI) application that serves as a centralised hub for applicants and Housing Development Board (HDB) staff to view, apply and manage BTO projects. This project is developed as part of the SC2002, Object-Oriented Design & Programming, module assignment at Nanyang Technological University, Singapore. 


## Project Structure
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
- [ ] **System**
  - [x] User login.
  - [x] View role-specific dashboards.
  - [x] NRIC validation.
  - [x] Change password, and prompts re-login.
  - [ ] Filter for project listing (user's setting persists throughout program execution).
- [ ] **Applicant**
    - [ ] View available projects.
    - [ ] Apply for a project.
    - [ ] View their application status.
    - [ ] Create booking for one flat (upon successful application).
    - [ ] Submit request for withdrawal of application.
    - [ ] Create, read, update, delete (CRUD) enquiries regarding BTO projects.
- [ ] **HDB Officer**
    - [ ] Possesses all of Applicant's capabilities.
    - [ ] Register for a project.
    - [ ] View their registration status.
    - [ ] View details of project (upon successful registration).
    - [ ] Respond to (and view) enquiries of the project.
    - [ ] Generate receipt of applicants with their respective flat booking details.
- [ ] **HDB Manager**
    - [ ] CRUD BTO project listings.
    - [ ] Approve/reject HDB Officer's registration.
    - [ ] Approve/reject Applicant's application.
    - [ ] Approve/reject Applicant's request to withdraw the application.
    - [ ] Respond to (and view) all enquiries.
    - [ ] Generate report on Applicants and their respective flat booking details.


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
